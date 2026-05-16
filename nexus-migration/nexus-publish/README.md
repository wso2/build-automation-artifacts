# Publish to Nexus 3

A Bash script that uploads Maven release artifacts to a **Nexus 3 Repository** instance via its REST API. It is registered as a **Jenkins Managed Script** and executed as a post-build step inside a **Maven Build Project** that uses the **M2 Release Plugin**.

## How It Works

1. **Guard check** — exits silently if `IS_M2RELEASEBUILD` is not `true`.
2. **Repository selection** — if `CLOSE_NEXUS_STAGE=true` uploads to the `releases` repository; otherwise uploads to `staging`.
3. **Artifact discovery** — scans for release artifacts in the following locations (in priority order):
   - `artifacts/<artifactId>/<version>/` — custom artifacts directory used by `ReleaseEnvironment.java`
   - `target/checkout/` — Maven release checkout directory
   - `target/checkout/target/` — build output inside the checkout
   - `.repository/<group/path>/` — Maven local repository layout
4. **Artifact collection** — recursively collects all files, deriving Maven coordinates from `.pom` filenames (repository layout) or by parsing `pom.xml` files (project layout). Outputs one pipe-separated line per file: `groupId|artifactId|version|type|filepath`.
5. **Validation** — verifies every discovered component has at least a `.pom` and a main artifact (`.jar` or `.war`) before any upload is attempted.
6. **Upload** — POSTs each unique `groupId:artifactId:version` component as a multipart form to the Nexus 3 Components REST API (`/service/rest/v1/components?repository=<repo>`). Detects and surfaces Cloudflare security blocks if encountered.

## Requirements

- `bash` 4+, `curl`
- Network access to the Nexus instance

## Environment Variables

### Required

| Variable | Description |
|---|---|
| `IS_M2RELEASEBUILD` | Must be `true` for the script to execute |
| `MVN_RELEASE_VERSION` | The release version being published (e.g. `1.2.3`) |

### Optional

| Variable | Default | Description |
|---|---|---|
| `NEXUS_URL` | `https://maven3-upgrade.wso2.org/nexus/` | Nexus base URL |
| `NEXUS_USER` | — | Nexus username (also read from `M2RELEASE_NEXUS_USER` or `MVNEXT_NEXUS_USER`) |
| `NEXUS_PASSWORD` | — | Nexus password (also read from `M2RELEASE_NEXUS_PASSWORD` or `MVNEXT_NEXUS_PASSWORD`) |
| `CLOSE_NEXUS_STAGE` | — | `true` → upload to `releases`; anything else → upload to `staging` |
| `NEXUS_API_PATH` | `service/rest/v1/components?repository=` | REST API path |
| `M2RELEASE_GROUP_ID` | — | Maven group ID of the project |
| `ARTIFACT_ID` / `M2RELEASE_ARTIFACT_ID` | — | Maven artifact ID (falls back to parsing `pom.xml`) |

## Artifact Discovery Locations

| Priority | Path | Notes |
|---|---|---|
| 1 | `artifacts/<artifactId>/<version>/` | Primary location set by `ReleaseEnvironment.java` |
| 2 | `target/checkout/` | Maven release plugin checkout directory |
| 3 | `target/checkout/target/` | Build output inside the checkout |
| 4 | `.repository/<group/path>/` | Maven local repository (all modules at the release version) |

## Validation Rules

Before any upload begins, every component is checked for:

- Presence of a `.pom` file
- Presence of at least one main artifact (`.jar` or `.war`)
- All listed files actually exist on disk

If validation fails, the script exits with an error and no uploads are performed.

## Jenkins Setup

### 1. Register as a Managed Script

The script must be stored in Jenkins as a **Managed Script** using the [Config File Provider](https://plugins.jenkins.io/config-file-provider/) plugin:

1. Go to **Manage Jenkins → Managed files → Add a new Config**.
2. Select type **Unix Shell Script**.
3. Set the **ID** to `nexus-sh`.
4. Paste the contents and save.

### 2. Required Jenkins Plugins

| Plugin | Purpose |
|---|---|
| [M2 Release Plugin](https://plugins.jenkins.io/m2release/) | Triggers release builds; sets `IS_M2RELEASEBUILD`, `MVN_RELEASE_VERSION`, `M2RELEASE_GROUP_ID` automatically |
| [Config File Provider](https://plugins.jenkins.io/config-file-provider/) | Stores and injects the managed script at build time |
| [Credentials Binding](https://plugins.jenkins.io/credentials-binding/) | Injects `NEXUS_USER` / `NEXUS_PASSWORD` from a stored credential |
| [Managed Scripts](https://plugins.jenkins.io/managed-scripts/) | Adds the managed script as a post-build step in the job UI |
| [EnvInject](https://plugins.jenkins.io/envinject/) | (Optional) Injects `CLOSE_NEXUS_STAGE` and other properties |

### 3. Configure the Maven Build Project

In the job configuration of your **Maven Build Project**:

#### Build Environment

- **✅ Inject passwords / credentials** *(Credentials Binding)*
  - Add a **Username and password (separated)** binding:
    - Credential: `nexus-deployer-credentials`
    - Username variable: `NEXUS_USER`
    - Password variable: `NEXUS_PASSWORD`

#### Post-build Actions

- **✅ Execute managed script** *(Managed Scripts)*
  - Script: `nexus-sh`

The M2 Release Plugin automatically exports the following variables that the script reads:

| Variable set by M2 Release Plugin | Description |
|---|---|
| `IS_M2RELEASEBUILD` | Set to `true` during a release build (script exits silently otherwise) |
| `MVN_RELEASE_VERSION` | The version being released |
| `M2RELEASE_GROUP_ID` | The Maven group ID of the project |

### 4. Stored Credentials

Create the following credential in **Manage Jenkins → Credentials**:

| ID | Type | Description |
|---|---|---|
| `nexus-deployer-credentials` | Username/Password | Nexus user with write access to the target repository |

### 5. Bulk Job Update

To apply this configuration across many existing jobs at once, use the `update-build-jobs.groovy` script from the `update-build-jobs/` directory via the Jenkins Script Console.
