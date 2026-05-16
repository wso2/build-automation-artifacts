# Automated Promotion Pipeline from Staging to Releases and Maven Central

A Jenkins Declarative Pipeline that automatically promotes **all** Maven artifacts found in a Nexus 'staging' repository to Nexus 'releases and **Central Publisher Portal** on a scheduled basis (every 6 hours). The pipeline discovers, downloads, moves, and uploads everything. Thereafter the Admin then confirms publishing to **Maven Central** or drops the deployment on the Central Publisher Portal.

## Workflow

```
Nexus Staging  ──(discover)──▶  Jenkins Agent
                                      │
                               (download all)
                                      │
                               (move → Nexus Releases)
                                      │
                               (maven-central.sh)
                                      │
                               Central Publisher Portal  ──(admin's action)──▶  Maven Central
```

1. **Discover Staging Components** — paginates the Nexus `/v1/search` API to collect every unique `group:name:version` tuple present in the staging repository. If nothing is found the pipeline exits cleanly.
2. **Download Artifacts from Nexus** — for each discovered component, fetches all assets (excluding hash files) into Maven local-repository layout at `.repository/<group/path>/<artifactId>/<version>/`. Checksums are intentionally skipped; `maven-central.sh` regenerates them.
3. **Move Staging → Releases** — calls the Nexus staging move API (`/service/rest/v1/staging/move/<releases-repo>`) once per unique `group:version` pair. Artifacts are moved **after** downloading so they are safely local first.
4. **Publish to Maven Central** — for each unique `group:version` pair, invokes `maven-central.sh` (Jenkins managed config file) once with `M2RELEASE_GROUP_ID`, `MVN_RELEASE_VERSION`, and `ARTIFACT_ID` set via `withEnv`. Uses `CENTRAL_PUBLISHING_TYPE=USER_MANAGED` so artifacts are validated and held on the Portal without auto-releasing.
5. **Cleanup** — removes the `.repository` directory from the workspace regardless of pipeline outcome.

## Schedule

The pipeline runs on a cron trigger four times a day:

| Time (UTC) | Cron expression |
|---|---|
| 00:00, 06:00, 12:00, 18:00 | `0 0,6,12,18 * * *` |

Adjust the `triggers { cron(...) }` block to change the frequency.

## Publishing Mode

The pipeline always uses `CENTRAL_PUBLISHING_TYPE=USER_MANAGED`. Artifacts are **validated and held** on the Portal; the admin must visit [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) to publish or drop each deployment. Change to `AUTOMATIC` if you want artifacts released immediately without manual confirmation.

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
| `<file_id` | Maven settings.xml | Injected as `CENTRAL_SETTINGS_XML`; contains `gpg.homedir`, `gpg.passphrase`, and Central server credentials |
| `maven-central-sh` | Unix Shell Script | The `maven-central.sh` script, written to a temp path and executed via `bash "$MAVEN_CENTRAL_SCRIPT"` |

## Agent

Runs on a Jenkins agent with label `TEST_PRODUCT_FOCAL_ECS`. Update this label in the `agent` block to match your environment.

## Environment Variables Set for `maven-central.sh`

| Variable | Value | Notes |
|---|---|---|
| `IS_M2RELEASEBUILD` | `true` | Tells the script this is a release build |
| `CLOSE_NEXUS_STAGE` | `true` | Closes the Nexus staging repo after upload |
| `MVN_RELEASE_VERSION` | overwritten per group:version | Set to the version of the current group:version pair |
| `M2RELEASE_GROUP_ID` | overwritten per group:version | Set to the group ID of the current group:version pair |
| `ARTIFACT_ID` | overwritten per group:version | Derived as the last dot-segment of the group ID (e.g. `carbon.identity` → `identity`) |
| `CENTRAL_PUBLISHING_TYPE` | `USER_MANAGED` | Controls whether artifacts are auto-released or held for review |

## Differences from Manual Pipeline

| Feature | Manual (`nexus-promotion.groovy`) | Automated (`auto-nexus-promotion.groovy`) |
|---|---|---|
| Trigger | User-initiated (Jenkins build with parameters) | Scheduled cron (every 6 hours) |
| Scope | Single `GROUP_ID` + `VERSION` (+ optional `ARTIFACT_ID`) | **All** components in the staging repo |
| Input parameters | `GROUP_ID`, `ARTIFACT_ID`, `VERSION`, `GROUP_ID_EXACT`, `CENTRAL_PUBLISHING_TYPE` | None — fully automated discovery |
| Publishing type | Configurable (`USER_MANAGED` or `AUTOMATIC`) | Always `USER_MANAGED` |

## Post-Build Behaviour

| Outcome | Action |
|---|---|
| **Success** | Prints a summary of promoted components and a link to the Publisher Portal |
| **Failure** | Logs a failure message with instructions to check the build log |
| **Always** | Removes the `.repository` directory from the workspace |
