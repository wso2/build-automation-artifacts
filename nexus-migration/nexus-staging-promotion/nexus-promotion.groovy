#!/usr/bin/env groovy
// -------------------------------------------------------------------------------------
// Copyright (c) 2026 WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
// --------------------------------------------------------------------------------------
//
// Jenkins Pipeline: Nexus Staging → Maven Central
//
// Workflow:
//   1. Move artifact from Nexus staging → releases repository
//   2. Download all artifacts from Nexus releases (Maven repo layout)
//   3. Publish to Maven Central via maven-central.sh
//
// Required Jenkins credentials:
//   nexus-deployer-credentials   : Username/Password  – Nexus user with staging move rights
//   maven-central-token          : Username/Password  – Central Publisher Portal token
//   central-settings-xml         : Config File        – settings.xml with gpg.homedir etc.
// -------------------------------------------------------------------------------------

pipeline {

    agent { label 'TEST_PRODUCT_FOCAL_ECS' }

    parameters {
        string(
            name: 'GROUP_ID',
            defaultValue: '',
            description: 'Maven Group ID of the artifact to promote (e.g. com.wso2.test)',
            trim: true
        )
        string(
            name: 'ARTIFACT_ID',
            defaultValue: '',
            description: 'Maven Artifact ID to promote (e.g. mytest)',
            trim: true
        )
        string(
            name: 'VERSION',
            defaultValue: '',
            description: 'Release version to promote (e.g. 1.0.0)',
            trim: true
        )
        choice(
            name: 'CENTRAL_PUBLISHING_TYPE',
            choices: ['USER_MANAGED', 'AUTOMATIC'],
            description: '''USER_MANAGED : Upload + validate then stop — admin publishes/drops manually on central.sonatype.com/publishing/deployments.
AUTOMATIC    : Upload + validate + auto-publish immediately.'''
        )
        string(
            name: 'NEXUS_URL',
            defaultValue: 'https://maven3-upgrade.wso2.org/nexus',
            description: 'Nexus base URL (no trailing slash)',
            trim: true
        )
        string(
            name: 'NEXUS_STAGING_REPO',
            defaultValue: 'staging',
            description: 'Source staging repository name in Nexus',
            trim: true
        )
        string(
            name: 'NEXUS_RELEASES_REPO',
            defaultValue: 'releases',
            description: 'Target releases repository name in Nexus',
            trim: true
        )
    }

    environment {
        // Flags read by maven-central.sh
        IS_M2RELEASEBUILD        = 'true'
        CLOSE_NEXUS_STAGE        = 'true'
        MVN_RELEASE_VERSION      = "${params.VERSION}"
        M2RELEASE_GROUP_ID       = "${params.GROUP_ID}"
        ARTIFACT_ID              = "${params.ARTIFACT_ID}"
        CENTRAL_PUBLISHING_TYPE  = "${params.CENTRAL_PUBLISHING_TYPE}"
    }

    stages {

        // ----------------------------------------------------------------
        // 1. Validate inputs
        // ----------------------------------------------------------------
        stage('Validate Parameters') {
            steps {
                script {
                    if (!params.GROUP_ID?.trim()) {
                        error "GROUP_ID parameter is required"
                    }
                    if (!params.ARTIFACT_ID?.trim()) {
                        error "ARTIFACT_ID parameter is required"
                    }
                    if (!params.VERSION?.trim()) {
                        error "VERSION parameter is required"
                    }
                    echo """
=============================================================
  Promoting to Maven Central
=============================================================
  Group ID    : ${params.GROUP_ID}
  Artifact ID : ${params.ARTIFACT_ID}
  Version     : ${params.VERSION}
  Nexus URL   : ${params.NEXUS_URL}
  Staging repo: ${params.NEXUS_STAGING_REPO}
  Releases repo: ${params.NEXUS_RELEASES_REPO}
  Central type: ${params.CENTRAL_PUBLISHING_TYPE}
=============================================================
"""
                }
            }
        }

        // ----------------------------------------------------------------
        // 2. Download artifacts from Nexus staging into Maven repo layout
        //    BEFORE moving, so we always pull from the known source repo.
        // ----------------------------------------------------------------
        stage('Download Artifacts from Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-deployer-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    script {
                        def nexusBase  = params.NEXUS_URL.replaceAll('/+$', '')
                        def groupId    = params.GROUP_ID
                        def artifactId = params.ARTIFACT_ID
                        def version    = params.VERSION
                        def sourceRepo = params.NEXUS_STAGING_REPO

                        // Search Nexus staging for all assets belonging to this component
                        def searchUrl = "${nexusBase}/service/rest/v1/search/assets" +
                                        "?repository=${sourceRepo}" +
                                        "&group=${groupId}" +
                                        "&name=${artifactId}" +
                                        "&version=${version}"

                        echo "Searching Nexus staging for assets: ${searchUrl}"

                        def searchResponse = sh(
                            returnStdout: true,
                            script: """
                                curl -s -f \\
                                    -u "\${NEXUS_USER}:\${NEXUS_PASSWORD}" \\
                                    -H "Accept: application/json" \\
                                    "${searchUrl}"
                            """
                        ).trim()

                        echo "Nexus search response: ${searchResponse}"

                        // Extract download URLs from JSON response using python3
                        def downloadUrls = sh(
                            returnStdout: true,
                            script: """
                                echo '${searchResponse.replace("'", "'\\''")}' | \\
                                python3 -c "
import sys, json
data = json.load(sys.stdin)
items = data.get('items', [])
for item in items:
    url = item.get('downloadUrl', '')
    # Skip hash files — we will regenerate checksums in maven-central.sh
    if not url.endswith(('.md5', '.sha1', '.sha256', '.sha512')):
        print(url)
"
                            """
                        ).trim()

                        if (!downloadUrls) {
                            error "No artifacts found in Nexus staging for ${groupId}:${artifactId}:${version} " +
                                  "in repository '${sourceRepo}'. Ensure the release build completed and artifacts are present."
                        }

                        echo "Found artifacts to download:"
                        downloadUrls.split('\n').each { echo "  ${it}" }

                        // Build Maven local repo layout: .repository/<group/path>/<artifactId>/<version>/
                        def groupPath = groupId.replace('.', '/')
                        def localDir  = "${env.WORKSPACE}/.repository/${groupPath}/${artifactId}/${version}"

                        sh "mkdir -p '${localDir}'"

                        // Download each artifact
                        downloadUrls.split('\n').each { url ->
                            url = url.trim()
                            if (!url) return
                            def fileName = url.tokenize('/').last()
                            echo "Downloading: ${fileName}"
                            sh """
                                curl -s -f -L \\
                                    -u "\${NEXUS_USER}:\${NEXUS_PASSWORD}" \\
                                    -o "${localDir}/${fileName}" \\
                                    "${url}"
                            """
                        }

                        echo "✅ All artifacts downloaded to: ${localDir}"
                        sh "ls -lh '${localDir}'"
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 3. Move from staging → releases in Nexus
        //    Done AFTER download so artifacts are already local.
        // ----------------------------------------------------------------
        stage('Move Staging → Releases') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-deployer-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    script {
                        def nexusBase = params.NEXUS_URL.replaceAll('/+$', '')
                        def moveUrl   = "${nexusBase}/service/rest/v1/staging/move/${params.NEXUS_RELEASES_REPO}" +
                                        "?repository=${params.NEXUS_STAGING_REPO}" +
                                        "&group=${params.GROUP_ID}" +
                                        "&name=${params.ARTIFACT_ID}" +
                                        "&version=${params.VERSION}"

                        echo "Calling Nexus staging move API: ${moveUrl}"

                        def response = sh(
                            returnStdout: true,
                            script: """
                                curl -s -w "\\nHTTP_STATUS:%{http_code}" \\
                                    -X POST \\
                                    -u "\${NEXUS_USER}:\${NEXUS_PASSWORD}" \\
                                    -H "Accept: application/json" \\
                                    "${moveUrl}"
                            """
                        ).trim()

                        def httpStatus = (response =~ /HTTP_STATUS:(\d+)/)[0][1] as Integer
                        def body       = response.replaceAll(/\nHTTP_STATUS:\d+$/, '')

                        echo "Nexus response (HTTP ${httpStatus}): ${body}"

                        if (httpStatus < 200 || httpStatus >= 300) {
                            error "Nexus staging move failed with HTTP ${httpStatus}. Response: ${body}"
                        }

                        echo "✅ Artifacts moved from '${params.NEXUS_STAGING_REPO}' to '${params.NEXUS_RELEASES_REPO}' successfully."
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 4. Publish to Maven Central via managed script
        // ----------------------------------------------------------------
        stage('Publish to Maven Central') {
            steps {
                // Bind the managed settings.xml (contains gpg.homedir, gpg.passphrase,
                // central.publishingType default, and maven-central server credentials)
                // AND the managed maven-central.sh shell script.
                // Both are written to temp files by the Config File Provider plugin.
                configFileProvider([
                    configFile(fileId: '<file_id>',  variable: 'CENTRAL_SETTINGS_XML'),
                    configFile(fileId: 'maven-central-sh',       variable: 'MAVEN_CENTRAL_SCRIPT')
                ]) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'maven-central-token',
                            usernameVariable: 'CENTRAL_TOKEN_USERNAME',
                            passwordVariable: 'CENTRAL_TOKEN_PASSWORD'
                        )
                    ]) {
                        sh 'bash "$MAVEN_CENTRAL_SCRIPT"'
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Post actions
    // ----------------------------------------------------------------
    post {
        success {
            script {
                if (params.CENTRAL_PUBLISHING_TYPE == 'USER_MANAGED') {
                    echo """
=============================================================
  ✅ Artifacts validated and waiting on Publisher Portal.
  Visit https://central.sonatype.com/publishing/deployments
  to publish or drop the deployment.
=============================================================
"""
                } else {
                    echo "✅ Artifacts published to Maven Central successfully."
                }
            }
        }
        failure {
            echo "❌ Pipeline failed. Check the logs above for details."
        }
        cleanup {
            // Remove downloaded artifacts to keep workspace clean
            sh "rm -rf '${env.WORKSPACE}/.repository' || true"
        }
    }
}
