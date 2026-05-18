# Update Maven Build Job Configurations

A Jenkins Script Console Groovy script that **bulk-updates existing Maven Build Jenkins jobs** to add the configuration required for publishing to Nexus 3 and Maven Central. 

## What It Does

For each qualifying job (those that already have an `M2ReleaseBuildWrapper`), the script applies up to **6 configuration changes**:

| Step | Change | Details |
|---|---|---|
| 1 | **Credentials Binding** | Adds a `UsernamePassword` binding for credential ID `maven-central-token`, exposing `CENTRAL_TOKEN_USERNAME` and `CENTRAL_TOKEN_PASSWORD` |
| 2 | **Config File Provider** | Binds managed settings file `<file_id>` to the variable `CENTRAL_SETTINGS_XML` |
| 3 | **EnvInject** | Adds `CENTRAL_PUBLISHING_TYPE=USER_MANAGED` to the job's injected environment properties |
| 4 | **Release Goals** | Injects `-Dmaven.deploy.skip=true` inside the `-Darguments="..."` block of the M2 release goals to suppress the default Maven deploy during a release build |
| 4b | **Nexus 3 Upload flag** | Enables the "Use Nexus3 Upload" boolean flag on the `M2ReleaseBuildWrapper` via reflection |
| 5 & 6 | **Post-build Steps** | Adds `nexus-sh` and `maven-central-sh` as `ScriptBuildStep` post-build steps (in that order) if not already present |

Each change is idempotent — already-configured jobs are left unchanged and logged as skipped.

## Configuration

Edit the variables at the top of the script before running:

```groovy
def MODE          = "single"                      // "single" | "folder" | "list"
def SINGLE_JOB_NAME = "test-jobs/maven-tester-support"  // full job path for single mode
def FOLDER_PATH   = "my-folder"                   // Jenkins folder path for folder mode
def EXCLUDED_FOLDERS = ["iam-cloud"]              // folders whose jobs must never be touched
def DRY_RUN       = false                         // true = report only, false = apply changes
```

## Modes

| Mode | Behaviour |
|---|---|
| `single` | Updates one specific job identified by `SINGLE_JOB_NAME` |
| `folder` | Updates all qualifying jobs (with `M2ReleaseBuildWrapper`) found anywhere under `FOLDER_PATH` |
| `list` | Diagnostic mode — prints all visible job paths with a `[M2Release]` marker for qualifying jobs. No changes are made. |

## Dry Run

Set `DRY_RUN = true` to preview all changes that _would_ be made without writing anything to Jenkins. The output clearly marks each change as pending. Set to `false` to apply.

## Required Jenkins Plugins

| Plugin | Used For |
|---|---|
| [M2 Release Plugin](https://plugins.jenkins.io/m2release/) | Detecting and modifying `M2ReleaseBuildWrapper` |
| [Credentials Binding Plugin](https://plugins.jenkins.io/credentials-binding/) | `SecretBuildWrapper` / `UsernamePasswordMultiBinding` |
| [Config File Provider Plugin](https://plugins.jenkins.io/config-file-provider/) | `ConfigFileBuildWrapper` / `ManagedFile` |
| [EnvInject Plugin](https://plugins.jenkins.io/envinject/) | `EnvInjectBuildWrapper` |
| [Managed Scripts Plugin](https://plugins.jenkins.io/managed-scripts/) | `ScriptBuildStep` (post-build steps) |

## Usage

1. Open Jenkins → **Manage Jenkins** → **Script Console**.
2. Paste the contents of `update-build-jobs.groovy`.
3. Set `DRY_RUN = true` and run to preview changes.
4. Review the output, then set `DRY_RUN = false` and run again to apply.

## Notes

- Jobs under `EXCLUDED_FOLDERS` are skipped by prefix match on the full job name.
- The script saves the job to disk (`job.save()`) only when `DRY_RUN = false` and a change was actually made.
