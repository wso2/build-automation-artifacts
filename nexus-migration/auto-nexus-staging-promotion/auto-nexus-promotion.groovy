#!/usr/bin/env groovy
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
// Jenkins Pipeline: Automated Nexus Staging → Maven Central (Scheduled)
//
// Workflow (runs every 6 hours — no user input required):
//   1. Discover all unique group:artifactId:version components in the staging repo
//   2. For each component: download all assets into Maven repo layout
//   3. Move all discovered components from staging → releases in one API call
//   4. Publish to Maven Central (USER_MANAGED — admin confirms on Publisher Portal)
//
// Required Jenkins credentials:
//   nexus-deployer-credentials   : Username/Password  – Nexus user with staging move rights
//   maven-central-token          : Username/Password  – Central Publisher Portal token
//
// Required Jenkins Config File Provider managed files:
//   <file_id>                    : settings.xml with gpg.homedir, gpg.passphrase etc.
//   maven-central-sh             : maven-central.sh publish script
// -------------------------------------------------------------------------------------

pipeline {

    agent { label 'TEST_PRODUCT_FOCAL_ECS' }

    triggers {
        // Poll every 6 hours: midnight, 06:00, 12:00, 18:00
        cron('0 0,6,12,18 * * *')
    }

    environment {
        NEXUS_URL            = 'https://maven3-upgrade.wso2.org/nexus'
        NEXUS_STAGING_REPO   = 'staging'
        NEXUS_RELEASES_REPO  = 'releases'

        // Always USER_MANAGED in automated mode — a human confirms/drops on the
        // Central Publisher Portal before anything is publicly released.
        CENTRAL_PUBLISHING_TYPE = 'USER_MANAGED'

        // Flags read by maven-central.sh
        IS_M2RELEASEBUILD    = 'true'
        CLOSE_NEXUS_STAGE    = 'true'
        MVN_RELEASE_VERSION  = ''   // overwritten per-component at publish time
        M2RELEASE_GROUP_ID   = ''   // overwritten per-component at publish time
        ARTIFACT_ID          = ''   // overwritten per-component at publish time
    }

    stages {

        // ----------------------------------------------------------------
        // 1. Discover all components in the staging repository
        // ----------------------------------------------------------------
        stage('Discover Staging Components') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-deployer-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    script {
                        echo "🔍 Scanning staging repository '${env.NEXUS_STAGING_REPO}' for promotable components..."

                        // Use /v1/search (component-level, not asset-level) with pagination
                        // to get distinct group:name:version tuples.
                        def componentsCsv = sh(
                            returnStdout: true,
                            script: """
                                python3 - <<'PYEOF'
import json, os, sys
import urllib.request, urllib.parse, base64

nexus_base  = os.environ.get('NEXUS_URL',          '${env.NEXUS_URL}')
source_repo = os.environ.get('NEXUS_STAGING_REPO', '${env.NEXUS_STAGING_REPO}')
user        = os.environ['NEXUS_USER']
password    = os.environ['NEXUS_PASSWORD']

credentials = base64.b64encode(f'{user}:{password}'.encode()).decode()
headers     = {'Accept': 'application/json', 'Authorization': f'Basic {credentials}'}

components         = []
seen               = set()
continuation_token = None

while True:
    params = {'repository': source_repo}
    if continuation_token:
        params['continuationToken'] = continuation_token

    url = f'{nexus_base}/service/rest/v1/search?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        data = json.load(resp)

    for item in data.get('items', []):
        key = (item.get('group',''), item.get('name',''), item.get('version',''))
        if all(key) and key not in seen:
            seen.add(key)
            components.append(key)

    continuation_token = data.get('continuationToken')
    if not continuation_token:
        break

for g, n, v in components:
    print(f'{g}|{n}|{v}')
PYEOF
                            """
                        ).trim()

                        // Filter to non-empty lines
                        def compLines = componentsCsv
                            ? componentsCsv.split('\n').findAll { it.trim() }
                            : []

                        if (!compLines) {
                            echo "✅ No components found in staging repository '${env.NEXUS_STAGING_REPO}'. Nothing to promote."
                            currentBuild.result = 'SUCCESS'
                            return
                        }

                        echo "Found ${compLines.size()} component(s) to promote:"
                        compLines.each { line ->
                            def p = line.trim().split('[|]')
                            echo "  • ${p[0]}:${p[1]}:${p[2]}"
                        }

                        // Store as pipe-delimited CSV for subsequent stages
                        env.COMPONENTS_CSV = componentsCsv
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 2. Download all assets for every discovered component
        // ----------------------------------------------------------------
        stage('Download Artifacts from Nexus') {
            when {
                expression { env.COMPONENTS_CSV?.trim() }
            }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-deployer-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    script {
                        def compLines = env.COMPONENTS_CSV.split('\n').findAll { it.trim() }

                        compLines.each { line ->
                            def p          = line.trim().split('[|]')
                            def groupId    = p[0]
                            def artifactId = p[1]
                            def version    = p[2]
                            def groupPath  = groupId.replace('.', '/')
                            def localDir   = "${env.WORKSPACE}/.repository/${groupPath}/${artifactId}/${version}"

                            echo "⬇️  Downloading: ${groupId}:${artifactId}:${version}"

                            def downloadUrls = sh(
                                returnStdout: true,
                                script: """
                                    python3 - <<'PYEOF'
import json, os
import urllib.request, urllib.parse, base64

nexus_base  = '${env.NEXUS_URL}'
source_repo = '${env.NEXUS_STAGING_REPO}'
group_id    = '${groupId}'
artifact_id = '${artifactId}'
version     = '${version}'
user        = os.environ['NEXUS_USER']
password    = os.environ['NEXUS_PASSWORD']

credentials = base64.b64encode(f'{user}:{password}'.encode()).decode()
headers     = {'Accept': 'application/json', 'Authorization': f'Basic {credentials}'}

all_urls           = []
continuation_token = None

while True:
    params = {
        'repository': source_repo,
        'group':      group_id,
        'name':       artifact_id,
        'version':    version
    }
    if continuation_token:
        params['continuationToken'] = continuation_token

    url = f'{nexus_base}/service/rest/v1/search/assets?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        data = json.load(resp)

    for item in data.get('items', []):
        dl_url = item.get('downloadUrl', '')
        # Skip hash files — checksums are regenerated by maven-central.sh
        if dl_url and not dl_url.endswith(('.md5', '.sha1', '.sha256', '.sha512')):
            all_urls.append(dl_url)

    continuation_token = data.get('continuationToken')
    if not continuation_token:
        break

for u in all_urls:
    print(u)
PYEOF
                                """
                            ).trim()

                            if (!downloadUrls) {
                                error "No assets found for ${groupId}:${artifactId}:${version} in '${env.NEXUS_STAGING_REPO}'."
                            }

                            sh "mkdir -p '${localDir}'"

                            downloadUrls.split('\n').each { url ->
                                url = url.trim()
                                if (!url) return
                                def fileName = url.tokenize('/').last()
                                sh """
                                    curl -s -f -L \\
                                        -u "\${NEXUS_USER}:\${NEXUS_PASSWORD}" \\
                                        -o "${localDir}/${fileName}" \\
                                        "${url}"
                                """
                            }

                            echo "  ✅ Downloaded to: ${localDir}"
                        }

                        echo "✅ All artifacts downloaded."
                        sh "find '${env.WORKSPACE}/.repository' -type f | sort"
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 3. Move all staging components to releases in a single API call
        //
        //    Omitting 'name' means Nexus moves ALL components that match
        //    repository + version in one atomic call. Since components
        //    in staging may span different groups, we group the move calls
        //    by unique group:version pairs to keep the filter precise.
        // ----------------------------------------------------------------
        stage('Move Staging → Releases') {
            when {
                expression { env.COMPONENTS_CSV?.trim() }
            }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-deployer-credentials',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    script {
                        // Deduplicate to unique "group|version" strings
                        def groupVersionKeys = env.COMPONENTS_CSV.split('\n')
                            .findAll { it.trim() }
                            .collect { line ->
                                def p = line.trim().split('[|]')
                                "${p[0]}|${p[2]}"
                            }
                            .unique()

                        def nexusBase = env.NEXUS_URL.replaceAll('/+$', '')
                        def allMoved  = true

                        groupVersionKeys.each { gvKey ->
                            def gvp     = gvKey.split('[|]')
                            def grp     = gvp[0]
                            def ver     = gvp[1]
                            def moveUrl = "${nexusBase}/service/rest/v1/staging/move/${env.NEXUS_RELEASES_REPO}" +
                                          "?repository=${env.NEXUS_STAGING_REPO}" +
                                          "&group=${grp}" +
                                          "&version=${ver}"

                            echo "📦 Moving ${grp}:${ver} → ${env.NEXUS_RELEASES_REPO}"

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

                            echo "  Nexus response (HTTP ${httpStatus}): ${body}"

                            if (httpStatus < 200 || httpStatus >= 300) {
                                echo "  ❌ Move failed for ${grp}:${ver} — HTTP ${httpStatus}"
                                allMoved = false
                            } else {
                                echo "  ✅ Moved ${grp}:${ver} successfully."
                            }
                        }

                        if (!allMoved) {
                            error "One or more staging move operations failed. See ❌ lines above."
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 4. Publish to Maven Central via managed script
        //    One invocation of maven-central.sh per unique group:version pair.
        //    M2RELEASE_GROUP_ID, ARTIFACT_ID, and MVN_RELEASE_VERSION are all
        //    set via withEnv so maven-central.sh finds the correct .repository
        //    sub-directory and never falls back to `mvn help:evaluate`.
        // ----------------------------------------------------------------
        stage('Publish to Maven Central') {
            when {
                expression { env.COMPONENTS_CSV?.trim() }
            }
            steps {
                configFileProvider([
                    configFile(
                        fileId: '<file_id>',
                        variable: 'CENTRAL_SETTINGS_XML'
                    ),
                    configFile(
                        fileId: 'maven-central-sh',
                        variable: 'MAVEN_CENTRAL_SCRIPT'
                    )
                ]) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'maven-central-token',
                            usernameVariable: 'CENTRAL_TOKEN_USERNAME',
                            passwordVariable: 'CENTRAL_TOKEN_PASSWORD'
                        )
                    ]) {
                        script {
                            // Deduplicate to unique "group|version" strings
                            def groupVersionKeys = env.COMPONENTS_CSV.split('\n')
                                .findAll { it.trim() }
                                .collect { line ->
                                    def p = line.trim().split('[|]')
                                    "${p[0]}|${p[2]}"
                                }
                                .unique()

                            groupVersionKeys.each { gvKey ->
                                def gvp      = gvKey.split('[|]')
                                def grp      = gvp[0]
                                def ver      = gvp[1]
                                def artLabel = grp.tokenize('.').last()
                                echo "🚀 Publishing ${grp}:${ver} to Maven Central..."
                                withEnv([
                                    "M2RELEASE_GROUP_ID=${grp}",
                                    "MVN_RELEASE_VERSION=${ver}",
                                    "ARTIFACT_ID=${artLabel}"
                                ]) {
                                    sh 'bash "$MAVEN_CENTRAL_SCRIPT"'
                                }
                            }
                        }
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
                if (env.COMPONENTS_CSV?.trim()) {
                    def summary = env.COMPONENTS_CSV.split('\n')
                        .findAll { it.trim() }
                        .collect { line ->
                            def p = line.trim().split('[|]')
                            "  • ${p[0]}:${p[1]}:${p[2]}"
                        }.join('\n')
                    echo """
=============================================================
  ✅ Artifacts validated and waiting on Publisher Portal.
  Components promoted:
${summary}

  Visit https://central.sonatype.com/publishing/deployments
  to publish or drop each deployment.
=============================================================
"""
                }
            }
        }
        failure {
            echo "❌ Automated promotion pipeline failed. Check the logs above for details."
        }
        cleanup {
            sh "rm -rf '${env.WORKSPACE}/.repository' || true"
        }
    }
}
