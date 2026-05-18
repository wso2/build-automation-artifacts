# Nexus Security Configuration Migration

A Bash script for exporting and importing **Nexus Repository security configuration** between Nexus instances. Supports content selectors, privileges, and roles. It is useful when migrating from one Nexus version to another (tested: Nexus Repository Pro 3.66.x → 3.82.x).

## Supported Objects

| Object | Nexus API Endpoint |
|---|---|
| Content Selectors | `/service/rest/v1/security/content-selectors` |
| Privileges | `/service/rest/v1/security/privileges` |
| Roles | `/service/rest/v1/security/roles` |

## Commands

### Export

Fetches all selected security objects from a Nexus instance and writes them to a JSON file.

```bash
SOURCE_PASS='oldpass' ./nexus-import-export.sh export \
  --url https://old-nexus.example.com \
  --user admin \
  --file nexus-security.json
```

### Import

Reads the JSON file and creates or updates the security objects in the target Nexus instance.

```bash
TARGET_PASS='newpass' ./nexus-import-export.sh import \
  --url https://new-nexus.example.com \
  --user admin \
  --file nexus-security.json
```

### Dry Run (Import only)

Preview what would be imported without making any changes:

```bash
TARGET_PASS='newpass' ./nexus-import-export.sh import \
  --url https://new-nexus.example.com \
  --user admin \
  --file nexus-security.json \
  --dry-run
```

## Requirements

- `bash`, `curl`, `jq`, `python3`

## Options

| Option | Description |
|---|---|
| `--url URL` | **(Required)** Nexus base URL |
| `--user USER` | **(Required)** Nexus username |
| `--file FILE` | JSON file path (default: `nexus-security.json`) |
| `--dry-run` | Import only: show what would be imported without modifying Nexus |
| `--overwrite` | Export only: overwrite the output file if it already exists |
| `--insecure` | Skip TLS certificate verification (`-k`) |
| `--no-content-selectors` | Skip content selectors |
| `--no-privileges` | Skip privileges |
| `--no-roles` | Skip roles |
| `--only-selectors` | Process content selectors only |
| `--only-privileges` | Process privileges only |
| `--only-roles` | Process roles only |

## Password Environment Variables

| Variable | Used When |
|---|---|
| `NEXUS_PASS` | Either command |
| `SOURCE_PASS` | Export command |
| `TARGET_PASS` | Import command |

If none of the above are set, the script prompts for the password interactively.

## Export File Format

The exported JSON file has the following structure:

```json
{
  "exportedAt": "2025-01-01T00:00:00Z",
  "sourceUrl": "https://old-nexus.example.com",
  "contentSelectors": [ ... ],
  "privileges": [ ... ],
  "roles": [ ... ]
}
```

## Import Behaviour

Objects are imported in dependency order:

1. **Content selectors** — created or updated via POST/PUT.
2. **Privileges** — created or updated; `repository-content-selector` privileges reference content selectors created in step 1.
3. **Roles** — imported in two passes:
   - **Pass 1**: Creates all role shells with empty `nestingRoles` arrays so every role ID exists before cross-references are wired.
   - **Pass 2**: Updates all roles with their full payload including nested role references.

System roles (`nx-*`) are handled carefully to avoid breaking built-in Nexus behaviour.
