# Publish to Maven Central 

A Bash script that publishes Maven release artifacts to [Maven Central](https://central.sonatype.com) via the **Central Publisher Portal REST API**. It is registered as a **Jenkins Managed Script** and executed as a post-build step inside a **Maven Build Project** that uses the **M2 Release Plugin**.

## How It Works

1. **Guard checks** — exits silently if `IS_M2RELEASEBUILD` is not `true`, or if `CLOSE_NEXUS_STAGE` is not `true` (staging-only builds are skipped).
2. **Credential resolution** — reads Central portal token from environment variables (`CENTRAL_TOKEN_USERNAME` / `CENTRAL_TOKEN_PASSWORD`). Falls back to reading a Maven `settings.xml` file (path resolved from `CENTRAL_SETTINGS_XML`, `MAVEN_SETTINGS_XML`, or the default `~/.m2/settings.xml`).
3. **GPG configuration** — reads `gpg.homedir` and `gpg.passphrase` from the active Maven profile in `settings.xml`. If a `GPG_KEY_FILE` is provided, it is imported and trusted automatically before signing.
4. **Artifact discovery** — scans for release artifacts in several locations (custom `artifacts/` dir, `target/checkout/`, Maven local repository `.repository/`) supporting both Maven repository layout (`.pom` files) and project checkout layout (`pom.xml` files).
5. **Bundle assembly** — copies all discovered artifacts into a temporary directory in Maven repository layout, generates `.md5` and `.sha1` checksums, and attaches GPG `.asc` signatures (reusing existing signatures when available, or signing fresh).
6. **Upload** — POSTs the ZIP bundle to the Central Publisher Portal (`/api/v1/publisher/upload`).
7. **Status polling** — polls `/api/v1/publisher/status` until the deployment reaches a terminal state (`VALIDATED`, `PUBLISHED`, or `FAILED`).
8. **Cleanup** — drops the deployment on failure; removes temp files on exit.

## Requirements

- `bash` 4+, `curl`, `zip`, `gpg`, `python3`
- A valid Maven Central Publisher Portal token

## Environment Variables

### Required

| Variable | Description |
|---|---|
| `IS_M2RELEASEBUILD` | Must be `true` for the script to execute |
| `MVN_RELEASE_VERSION` | The release version being published (e.g. `1.2.3`) |
| `CENTRAL_TOKEN_USERNAME` | Maven Central portal token username |
| `CENTRAL_TOKEN_PASSWORD` | Maven Central portal token password |

### Optional

| Variable | Default | Description |
|---|---|---|
| `CLOSE_NEXUS_STAGE` | — | Must be `true` to proceed; staging builds are skipped |
| `CENTRAL_API_URL` | `https://central.sonatype.com` | Override the Central API base URL |
| `CENTRAL_PUBLISHING_TYPE` | `USER_MANAGED` | `AUTOMATIC` (auto-publish after validation) or `USER_MANAGED` (validate only, manual publish) |
| `CENTRAL_DEPLOYMENT_NAME` | `groupId:artifactId:version` | Human-readable label for the deployment |
| `CENTRAL_SERVER_ID` | `maven-central` | Server ID in `settings.xml` used as credential fallback |
| `CENTRAL_SETTINGS_XML` | — | Explicit path to a `settings.xml` file |
| `MAVEN_SETTINGS_XML` | — | Alternative `settings.xml` path (lower priority) |
| `GPG_REUSE_EXISTING_SIGNATURES` | `false` | Set `true` to reuse `.asc` files produced by the Maven build |
| `GPG_KEY_ID` | — | Specific GPG key ID to use for signing |
| `GPG_PASSPHRASE` | — | GPG key passphrase |
| `GPG_KEY_FILE` | — | Path to an armored GPG private key file to import before signing |
| `M2RELEASE_GROUP_ID` | — | Maven group ID of the project |
| `ARTIFACT_ID` / `M2RELEASE_ARTIFACT_ID` | — | Maven artifact ID |

## Publishing Types

| Type | Behaviour |
|---|---|
| `USER_MANAGED` | Uploads and validates the bundle, then stops. An admin must visit [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) to publish or drop it manually. |
| `AUTOMATIC` | Uploads, validates, and auto-publishes the bundle without any manual intervention. |

## GPG Signature Strategy

The script uses the following priority to attach signatures:

1. If `GPG_REUSE_EXISTING_SIGNATURES=true` — copy the `.asc` file already present next to the source artifact (produced by `maven-gpg-plugin` during the release build).
2. If a `.asc` exists next to the source file **and** `GPG_KEY_ID` is **not** set — auto-reuse the existing signature.
3. Otherwise — generate a fresh GPG signature using the configured key.

## Jenkins Setup

### 1. Register as a Managed Script

The script must be stored in Jenkins as a **Managed Script** using the [Config File Provider](https://plugins.jenkins.io/config-file-provider/) plugin:

1. Go to **Manage Jenkins → Managed files → Add a new Config**.
2. Select type **Unix Shell Script**.
3. Set the **ID** to `maven-central-sh`.
4. Paste the contents of `maven-central.sh` and save.

### 2. Required Jenkins Plugins

| Plugin | Purpose |
|---|---|
| [M2 Release Plugin](https://plugins.jenkins.io/m2release/) | Triggers release builds; sets `IS_M2RELEASEBUILD`, `MVN_RELEASE_VERSION`, `M2RELEASE_GROUP_ID` automatically |
| [Config File Provider](https://plugins.jenkins.io/config-file-provider/) | Stores the managed script and the `settings.xml` config file; injects both at build time |
| [Credentials Binding](https://plugins.jenkins.io/credentials-binding/) | Injects `CENTRAL_TOKEN_USERNAME` / `CENTRAL_TOKEN_PASSWORD` from a stored credential |
| [Managed Scripts](https://plugins.jenkins.io/managed-scripts/) | Adds the managed script as a post-build step in the job UI |
| [EnvInject](https://plugins.jenkins.io/envinject/) | Injects `CLOSE_NEXUS_STAGE` and `CENTRAL_PUBLISHING_TYPE` environment properties |

### 3. Store the settings.xml Config File

The script reads GPG configuration (`gpg.homedir`, `gpg.passphrase`) and optionally Central credentials from a Maven `settings.xml`. Store it as a **Maven settings.xml** managed file:

1. Go to **Manage Jenkins → Managed files → Add a new Config**.
2. Select type **Maven settings.xml**.
3. Note the auto-generated file ID  — you will reference this ID in the job.
4. Add the `<server>` entry for Central credentials and the `<profile>` properties for `gpg.homedir` / `gpg.passphrase`.


### 4. Configure the Maven Build Project

In the job configuration of your **Maven Build Project**:

#### Build Environment

- **✅ Provide Config files** *(Config File Provider)*
  - Add the `settings.xml` managed file:
    - File: *(select your stored settings.xml by ID)*
    - Variable: `CENTRAL_SETTINGS_XML`

- **✅ Inject passwords / credentials** *(Credentials Binding)*
  - Add a **Username and password (separated)** binding:
    - Credential: `maven-central-token`
    - Username variable: `CENTRAL_TOKEN_USERNAME`
    - Password variable: `CENTRAL_TOKEN_PASSWORD`

- **✅ Inject environment variables** *(EnvInject)* — add to *Properties Content*:
  ```
  CENTRAL_PUBLISHING_TYPE=USER_MANAGED
  ```

#### Post-build Actions

Both managed scripts must be added as post-build steps **in this order**:

1. **Execute managed script** → `nexus-sh` *(uploads to Nexus)*
2. **Execute managed script** → `maven-central-sh` *(publishes to Maven Central)*

The M2 Release Plugin automatically exports the following variables that the script reads:

| Variable set by M2 Release Plugin | Description |
|---|---|
| `IS_M2RELEASEBUILD` | Set to `true` during a release build (script exits silently otherwise) |
| `MVN_RELEASE_VERSION` | The version being released |
| `M2RELEASE_GROUP_ID` | The Maven group ID of the project |

### 5. Stored Credentials

Create the following credential in **Manage Jenkins → Credentials**:

| ID | Type | Description |
|---|---|---|
| `maven-central-token` | Username/Password | Central Publisher Portal user token (generated at [central.sonatype.com/account](https://central.sonatype.com/account)) |

### 6. Bulk Job Update

To apply this configuration across many existing jobs at once, use the `update-build-jobs.groovy` script from the `update-build-jobs/` directory via the Jenkins Script Console.

## API Reference

[https://central.sonatype.org/publish/publish-portal-api/](https://central.sonatype.org/publish/publish-portal-api/)
