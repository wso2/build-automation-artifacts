# Nexus and Maven Central Migration

This repository contains scripts and pipelines for migrating Maven artifact publishing from **Nexus Repository 3** to **Maven Central**, as well as tooling for migrating Nexus security configurations between instances.

## Contents

| Script / File | Location | Description |
|---|---|---|
| `nexus3.sh` | [`nexus-publish/`](./nexus-publish/README.md) | Uploads Maven release artifacts to Nexus 3 as a Jenkins Managed Script post-build step |
| `maven-central.sh` | [`maven-central-publish/`](./maven-central-publish/README.md) | Publishes Maven release artifacts to Maven Central via the Central Publisher Portal REST API |
| `nexus-promotion.groovy` | [`manual-nexus-staging-promotion/`](./manual-nexus-staging-promotion/README.md) | Jenkins Declarative Pipeline to manually promote artifacts from Nexus staging directly to Maven Central (parameterised) |
| `auto-nexus-promotion.groovy` | [`auto-nexus-staging-promotion/`](./auto-nexus-staging-promotion/README.md) | Jenkins Declarative Pipeline to automatically promote **all** staging artifacts to Maven Central on a scheduled cron (every 6 hours) |
| `nexus-import-export.sh` | [`nexus-import-export-confs/`](./nexus-import-export-confs/README.md) | Exports and imports Nexus security configuration (content selectors, privileges, roles) between instances |
| `update-build-jobs.groovy` | [`update-build-jobs/`](./update-build-jobs/README.md) | Jenkins Script Console script to bulk-update existing Maven Build Project jobs with the Maven Central publishing configuration |

## Migration Details

### Release builds 

Artifacts are published to both Nexus and Maven Central as part of the same release build:

```
Maven Build Project (M2 Release)
  └── Post-build: nexus3.sh       → Nexus releases / staging
  └── Post-build: maven-central.sh → Maven Central or Central Publisher Portal
```

See [`nexus-publish/`](./nexus-publish/README.md) and [`maven-central-publish/`](./maven-central-publish/README.md) for setup instructions.

### Promote artifacts in Nexus Staging Repository

Artifacts already sitting in Nexus staging can be promoted to Maven Central Publisher Portal without a new release build:

```
Nexus Staging → (download) → Jenkins Agent → (move) → Nexus Releases
                                   └── maven-central.sh → Maven Central or Central Publisher Portal
```

Two pipelines are available:

- **Manual** ([`manual-nexus-staging-promotion/`](./manual-nexus-staging-promotion/README.md)) — parameterised pipeline; promotes a specific `GROUP_ID` + `VERSION` (and optional `ARTIFACT_ID`) on demand.
- **Automated** ([`auto-nexus-staging-promotion/`](./auto-nexus-staging-promotion/README.md)) — scheduled pipeline; discovers and promotes **all** components in the staging repository every 6 hours without user input.

### Nexus Instance Migration

To migrate security configuration (roles, privileges, content selectors) from one Nexus instance to another, see [`nexus-import-export-confs/`](./nexus-import-export-confs/README.md).

### Bulk Job Configuration

To apply the Maven Central publishing configuration to many existing Jenkins jobs at once, see [`update-build-jobs/`](./update-build-jobs/README.md).
