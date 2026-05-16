# Promote Staging to Releases and Maven Central

A Jenkins Declarative Pipeline that promotes Maven artifacts from a Nexus 'staging' repository to Nexus 'releases' repository and **Central Publisher Portal**. This pipeline is used for artifacts that were already published to Nexus 'staging' and need to be propagate to Nexus 'releases' and Central Publisher Portal manually.

## Workflow

```
Nexus Staging  ──(download)──▶  Jenkins Agent  ──(move)──▶  Nexus Releases
                                      │
                                      └──(maven-central.sh)──▶  Maven Central or Central Publisher Portal
```

1. **Validate Parameters** — ensures `GROUP_ID` and `VERSION` are provided (`ARTIFACT_ID` is optional; omit it to promote all artifacts under the group and version).
2. **Download Artifacts from Nexus** — searches the Nexus staging repository using the asset search API and downloads all artifacts (excluding hash files) into Maven local repository layout at `.repository/<group/path>/<artifactId>/<version>/`. Hash files are intentionally skipped as `maven-central.sh` regenerates them.
3. **Move Staging → Releases** — calls the Nexus staging move API (`/service/rest/v1/staging/move/<releases-repo>`) to promote the artifacts from staging to the releases repository. This is done **after** downloading so artifacts are safely local first.
4. **Publish to Maven Central** — runs `maven-central.sh` (provided as a Jenkins managed config file) to bundle, sign, upload, and validate the artifacts on Maven Central.
5. **Cleanup** — removes the downloaded `.repository` directory from the workspace regardless of outcome.

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `GROUP_ID` | — | Maven group ID of the artifact (e.g. `com.wso2.test`) |
| `ARTIFACT_ID` | — | Maven artifact ID (e.g. `mytest`). Leave blank to promote **all** artifacts under `GROUP_ID` + `VERSION` |
| `VERSION` | — | Release version to promote (e.g. `1.0.0`) |
| `GROUP_ID_EXACT` | `true` | `true`: match `GROUP_ID` exactly; `false`: use as a prefix to include sub-groups (e.g. `org.wso2.carbon.identity.*`) |
| `CENTRAL_PUBLISHING_TYPE` | `USER_MANAGED` | `USER_MANAGED`: validate only, manual publish via portal; `AUTOMATIC`: auto-publish immediately |
| `NEXUS_URL` | `https://maven3-upgrade.wso2.org/nexus` | Nexus base URL (no trailing slash) |
| `NEXUS_STAGING_REPO` | `staging` | Source repository in Nexus |
| `NEXUS_RELEASES_REPO` | `releases` | Target repository in Nexus |

## Required Jenkins Credentials

Create the following credentials in **Manage Jenkins → Credentials**:

| Credential ID | Type | Used For |
|---|---|---|
| `nexus-deployer-credentials` | Username/Password | Nexus REST API authentication (`NEXUS_USER` / `NEXUS_PASSWORD`) |
| `maven-central-token` | Username/Password | Central Publisher Portal token (`CENTRAL_TOKEN_USERNAME` / `CENTRAL_TOKEN_PASSWORD`) |

## Required Managed Config Files

Create the following managed files in **Manage Jenkins → Managed files** using the [Config File Provider](https://plugins.jenkins.io/config-file-provider/) plugin:

| File ID | Type | Used For |
|---|---|---|
| `file_id` | Maven settings.xml | Injected as `CENTRAL_SETTINGS_XML`; contains `gpg.homedir`, `gpg.passphrase`, and Central server credentials |
| `maven-central-sh` | Unix Shell Script | The `maven-central.sh` script, written to a temp path and executed via `bash "$MAVEN_CENTRAL_SCRIPT"` |

## Agent

Runs on a Jenkins agent with label `TEST_PRODUCT_FOCAL_ECS`. Update this label to match your environment.

## Environment Variables Set for `maven-central.sh`

| Variable | Value |
|---|---|
| `IS_M2RELEASEBUILD` | `true` |
| `CLOSE_NEXUS_STAGE` | `true` |
| `MVN_RELEASE_VERSION` | `params.VERSION` |
| `M2RELEASE_GROUP_ID` | `params.GROUP_ID` (overridden per actual sub-group during publish) |
| `ARTIFACT_ID` | `params.ARTIFACT_ID` |
| `CENTRAL_PUBLISHING_TYPE` | `params.CENTRAL_PUBLISHING_TYPE` |

## Post-Build Behaviour

| Outcome | `USER_MANAGED` | `AUTOMATIC` |
|---|---|---|
| **Success** | Prints a reminder to visit [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) to publish or drop | Confirms artifacts were published |
| **Failure** | Logs a failure message | Logs a failure message |
| **Always** | Removes `.repository` directory from workspace | Same |
