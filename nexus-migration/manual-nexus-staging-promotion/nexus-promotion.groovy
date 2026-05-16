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
// Jenkins Pipeline: Nexus Staging → Maven Central
//
// Workflow:
//   1. Move artifact from Nexus staging → releases repository
//   2. Download all artifacts from Nexus releases (Maven repo layout)
//   3. Publish to Maven Central (USER_MANAGED — admin confirms on Publisher Portal)
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
            description: 'Maven Artifact ID to promote (e.g. mytest). Leave blank to promote ALL artifacts under GROUP_ID + VERSION.',
            trim: true
        )
        string(
            name: 'VERSION',
            defaultValue: '',
            description: 'Release version to promote (e.g. 1.0.0)',
            trim: true
        )
        booleanParam(
            name: 'GROUP_ID_EXACT',
            defaultValue: true,
            description: '''true  : Search Nexus with the GROUP_ID as an exact match (default).
false : Search with GROUP_ID as a prefix — use this when artifacts live under sub-groups
        (e.g. org.wso2.carbon.identity.* when GROUP_ID = org.wso2.carbon.identity).'''
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
                        def artifactId  = params.ARTIFACT_ID?.trim() ?: ''
                        def version     = params.VERSION
                        def sourceRepo  = params.NEXUS_STAGING_REPO
                        def groupExact  = params.GROUP_ID_EXACT

                        echo artifactId
                            ? "Searching Nexus staging for ${groupId}:${artifactId}:${version} in '${sourceRepo}' (exact=${groupExact})"
                            : "Searching Nexus staging for ALL artifacts under ${groupId}:${version} in '${sourceRepo}' (exact=${groupExact})"

                        // Collect all download URLs across all pages via continuationToken pagination.
                        // ARTIFACT_ID is optional — omitted from the query when blank so all artifacts
                        // under GROUP_ID + VERSION are returned.
                        // When GROUP_ID_EXACT=false a free-text prefix search (q=<group>*) is used
                        // so that sub-groups such as org.wso2.carbon.identity.core are also matched.
                        def downloadUrls = sh(
                            returnStdout: true,
                            script: """
                                python3 - <<'PYEOF'
import sys, json, os, traceback
import urllib.request, urllib.parse, base64

try:
    nexus_base  = '${nexusBase}'
    source_repo = '${sourceRepo}'
    group_id    = '${groupId}'
    artifact_id = '${artifactId}'
    version     = '${version}'
    group_exact = '${groupExact}'.lower() == 'true'
    user        = os.environ['NEXUS_USER']
    password    = os.environ['NEXUS_PASSWORD']

    credentials = base64.b64encode(f'{user}:{password}'.encode()).decode()
    headers     = {'Accept': 'application/json', 'Authorization': f'Basic {credentials}'}

    all_urls           = []
    continuation_token = None
    page               = 0

    while True:
        if group_exact:
            # Exact group match via structured search parameters
            params = {'repository': source_repo, 'group': group_id, 'version': version}
        else:
            # Prefix search: matches group_id and all sub-groups (e.g. org.wso2.carbon.identity.*)
            params = {'repository': source_repo, 'q': f'{group_id}*', 'version': version}

        if artifact_id:
            params['name'] = artifact_id
        if continuation_token:
            params['continuationToken'] = continuation_token

        url = f'{nexus_base}/service/rest/v1/search/assets?' + urllib.parse.urlencode(params)
        print(f'[DEBUG] page={page} GET {url}', file=sys.stderr)
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as resp:
            raw  = resp.read()
            data = json.loads(raw)

        items = data.get('items', [])
        print(f'[DEBUG] page={page} items={len(items)}', file=sys.stderr)

        for item in items:
            dl_url = item.get('downloadUrl', '')
            # Skip hash files — checksums are regenerated by maven-central.sh
            if dl_url and not dl_url.endswith(('.md5', '.sha1', '.sha256', '.sha512')):
                all_urls.append(dl_url)

        continuation_token = data.get('continuationToken')
        page += 1
        if not continuation_token:
            break

    print(f'[DEBUG] total download URLs collected: {len(all_urls)}', file=sys.stderr)
    for u in all_urls:
        print(u)

except Exception:
    traceback.print_exc(file=sys.stderr)
    sys.exit(1)
PYEOF
                            """
                        ).trim()

                        if (!downloadUrls) {
                            def target      = artifactId ? "${groupId}:${artifactId}:${version}" : "${groupId}:${version}"
                            def searchMode  = groupExact ? "exact group match" : "prefix search (${groupId}.*)"
                            error "No artifacts found in Nexus staging for ${target} " +
                                  "in repository '${sourceRepo}' using ${searchMode}.\n" +
                                  "• If artifacts are under sub-groups (e.g. org.wso2.carbon.identity.core), " +
                                  "re-run with GROUP_ID_EXACT=false.\n" +
                                  "• Check the [DEBUG] lines above for the actual API URL and item count."
                        }

                        echo "Found artifacts to download:"
                        downloadUrls.split('\n').each { echo "  ${it}" }

                        // Nexus URL layout: <nexusBase>/repository/<repo>/<groupPath>/<artifactId>/<version>/<file>
                        // Derive the actual group path directly from each URL so that sub-groups
                        // (e.g. org/wso2/carbon/identity/saml/common) are stored correctly
                        // regardless of whether an exact or prefix group search was used.
                        def repoPrefix = "${nexusBase}/repository/${sourceRepo}/"

                        downloadUrls.split('\n').each { url ->
                            url = url.trim()
                            if (!url) return
                            // Strip the Nexus repo prefix to get the relative Maven path
                            // e.g. org/wso2/carbon/identity/saml/common/my-artifact/1.0/my-artifact-1.0.jar
                            def relPath        = url.replace(repoPrefix, '')
                            def relParts       = relPath.tokenize('/')
                            // relParts: [...groupSegments..., artifactId, version, filename]
                            def fileName       = relParts[-1]
                            def effectiveArtId = artifactId ?: relParts[-3]
                            def actualGroupPath = relParts[0..(relParts.size() - 4)].join('/')
                            def localDir       = "${env.WORKSPACE}/.repository/${actualGroupPath}/${effectiveArtId}/${version}"
                            sh "mkdir -p '${localDir}'"
                            echo "Downloading: ${fileName} → ${actualGroupPath}/${effectiveArtId}/${version}/"
                            sh """
                                curl -s -f -L \\
                                    -u "\${NEXUS_USER}:\${NEXUS_PASSWORD}" \\
                                    -o "${localDir}/${fileName}" \\
                                    "${url}"
                            """
                        }

                        // Collect unique actual groups discovered (dot-notation) for use by the Move stage.
                        // The input GROUP_ID may be a prefix; the real groups are in the URL paths.
                        def actualGroups = downloadUrls.split('\n')
                            .findAll { it.trim() }
                            .collect { u ->
                                def rel   = u.trim().replace(repoPrefix, '')
                                def parts = rel.tokenize('/')
                                // parts[-3] = artifactId, parts[-2] = version, parts[-1] = file
                                // everything before those 3 is the group path
                                parts[0..(parts.size() - 4)].join('.')
                            }
                            .unique()

                        echo "Actual groups to move: ${actualGroups}"
                        env.ACTUAL_GROUPS_CSV = actualGroups.join(',')

                        echo "✅ All artifacts downloaded."
                        sh "find '${env.WORKSPACE}/.repository' -type f | sort"
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 3. Move from staging → releases in Nexus
        //    Done AFTER download so artifacts are already local.
        //
        //    The staging move API accepts any combination of search criteria
        //    (group, name, version, tag, etc.) — all are optional filters.
        //    Omitting 'name' (ARTIFACT_ID) is fully supported: Nexus will move
        //    ALL components matching group + version in a single atomic call,
        //    so no per-artifact-ID iteration is required even for multi-module
        //    projects.
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
                        def nexusBase    = params.NEXUS_URL.replaceAll('/+$', '')
                        def actualGroups = env.ACTUAL_GROUPS_CSV.split(',').toList()

                        // One move call per distinct actual group discovered during download.
                        // This correctly handles prefix searches where sub-groups differ from
                        // the input GROUP_ID (e.g. org.wso2.carbon.identity.saml.common).
                        def allMoved = true
                        actualGroups.each { grp ->
                            def moveUrl = "${nexusBase}/service/rest/v1/staging/move/${params.NEXUS_RELEASES_REPO}" +
                                          "?repository=${params.NEXUS_STAGING_REPO}" +
                                          "&group=${grp}" +
                                          (params.ARTIFACT_ID?.trim() ? "&name=${params.ARTIFACT_ID.trim()}" : "") +
                                          "&version=${params.VERSION}"

                            echo "📦 Moving ${grp}:${params.VERSION} → ${params.NEXUS_RELEASES_REPO}"

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
                                echo "  ❌ Move failed for ${grp} — HTTP ${httpStatus}"
                                allMoved = false
                            } else {
                                echo "  ✅ Moved ${grp}:${params.VERSION} successfully."
                            }
                        }

                        if (!allMoved) {
                            error "One or more staging move operations failed. See ❌ lines above."
                        }

                        echo "✅ All artifacts moved from '${params.NEXUS_STAGING_REPO}' to '${params.NEXUS_RELEASES_REPO}'."
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // 4. Publish to Maven Central via managed script
        //    One invocation of maven-central.sh per actual Maven group discovered
        //    during download. Each invocation:
        //      - sets M2RELEASE_GROUP_ID to the real sub-group
        //        (e.g. org.wso2.carbon.identity.saml.common) so find_artifacts
        //        scans the correct .repository sub-directory
        //      - sets ARTIFACT_ID to a non-empty value so maven-central.sh
        //        does not fall back to `mvn help:evaluate`
        //    All artifact IDs under that group are discovered automatically by
        //    collect_all_artifacts via recursive .pom file scanning.
        // ----------------------------------------------------------------
        stage('Publish to Maven Central') {
            steps {
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
                        script {
                            def actualGroups = env.ACTUAL_GROUPS_CSV.split(',').toList()
                            actualGroups.each { grp ->
                                // Use the last segment as a non-empty ARTIFACT_ID label
                                // (maven-central.sh only uses it for the deployment name;
                                // actual artifact IDs are always discovered from the .pom files)
                                def artLabel = grp.tokenize('.').last()
                                echo "🚀 Publishing ${grp}:${params.VERSION} to Maven Central..."
                                withEnv([
                                    "M2RELEASE_GROUP_ID=${grp}",
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
