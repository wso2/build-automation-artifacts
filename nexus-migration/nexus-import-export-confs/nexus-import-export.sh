#!/bin/bash
# -------------------------------------------------------------------------------------
# Copyright (c) 2026 WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
#
# WSO2 LLC. licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except
# in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------------------------------

set -euo pipefail

# ==============================================================================
# Nexus Repository Security Export/Import Script
#
# Supports:
#   - Content selectors
#   - Privileges
#   - Roles
#
# Commands:
#   export - Export security objects from Nexus to JSON
#   import - Import security objects from JSON into Nexus
#
# Tested target flow:
#   Nexus Repository Pro 3.66.x -> Nexus Repository Pro 3.82.x
#
# Requirements:
#   - bash
#   - curl
#   - jq
#   - python3
#
# Password environment variables:
#   NEXUS_PASS
#   SOURCE_PASS, for export
#   TARGET_PASS, for import
#
# Examples:
#
#   SOURCE_PASS='oldpass' ./nexus-security-export-import.sh export \
#     --url https://old-nexus.example.com \
#     --user admin \
#     --file nexus-security.json
#
#   TARGET_PASS='newpass' ./nexus-security-export-import.sh import \
#     --url https://new-nexus.example.com \
#     --user admin \
#     --file nexus-security.json
# ==============================================================================

COMMAND="${1:-}"
if [[ -n "$COMMAND" ]]; then
  shift
fi

NEXUS_URL=""
NEXUS_USER=""
NEXUS_PASS_VALUE=""
FILE="nexus-security.json"

DRY_RUN="false"
OVERWRITE="false"
SKIP_TLS_VERIFY="false"

INCLUDE_SELECTORS="true"
INCLUDE_PRIVILEGES="true"
INCLUDE_ROLES="true"

usage() {
  cat <<EOF
Usage:
  $0 export --url URL --user USER [options]
  $0 import --url URL --user USER [options]

Commands:
  export                  Export Nexus security objects to JSON
  import                  Import Nexus security objects from JSON

Required:
  --url URL               Nexus base URL, e.g. https://nexus.example.com
  --user USER             Nexus username

Options:
  --file FILE             JSON file path. Default: nexus-security.json
  --dry-run               Import only: show what would be imported, do not modify Nexus
  --overwrite             Export only: overwrite file if it already exists
  --insecure              Skip TLS certificate verification

Include/exclude object types:
  --no-content-selectors  Do not export/import content selectors
  --no-privileges         Do not export/import privileges
  --no-roles              Do not export/import roles
  --only-selectors        Export/import content selectors only
  --only-privileges       Export/import privileges only
  --only-roles            Export/import roles only

Password environment variables:
  NEXUS_PASS              Password for either command
  SOURCE_PASS             Password for export command
  TARGET_PASS             Password for import command

Examples:
  SOURCE_PASS='oldpass' $0 export \\
    --url https://old-nexus.example.com \\
    --user admin \\
    --file nexus-security.json

  TARGET_PASS='newpass' $0 import \\
    --url https://new-nexus.example.com \\
    --user admin \\
    --file nexus-security.json

  TARGET_PASS='newpass' $0 import \\
    --url https://new-nexus.example.com \\
    --user admin \\
    --file nexus-security.json \\
    --dry-run
EOF
}

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

warn() {
  echo "WARNING: $*" >&2
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

strip_trailing_slash() {
  echo "$1" | sed 's:/*$::'
}

urlencode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}

case "$COMMAND" in
  export|import)
    ;;
  -h|--help|"")
    usage
    exit 0
    ;;
  *)
    fail "Unknown command: $COMMAND"
    ;;
esac

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url)
      NEXUS_URL="$2"
      shift 2
      ;;
    --user)
      NEXUS_USER="$2"
      shift 2
      ;;
    --file)
      FILE="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --overwrite)
      OVERWRITE="true"
      shift
      ;;
    --insecure)
      SKIP_TLS_VERIFY="true"
      shift
      ;;
    --no-content-selectors)
      INCLUDE_SELECTORS="false"
      shift
      ;;
    --no-privileges)
      INCLUDE_PRIVILEGES="false"
      shift
      ;;
    --no-roles)
      INCLUDE_ROLES="false"
      shift
      ;;
    --only-selectors)
      INCLUDE_SELECTORS="true"
      INCLUDE_PRIVILEGES="false"
      INCLUDE_ROLES="false"
      shift
      ;;
    --only-privileges)
      INCLUDE_SELECTORS="false"
      INCLUDE_PRIVILEGES="true"
      INCLUDE_ROLES="false"
      shift
      ;;
    --only-roles)
      INCLUDE_SELECTORS="false"
      INCLUDE_PRIVILEGES="false"
      INCLUDE_ROLES="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown argument: $1"
      ;;
  esac
done

require_command curl
require_command jq
require_command python3

[[ -n "$NEXUS_URL" ]] || fail "--url is required"
[[ -n "$NEXUS_USER" ]] || fail "--user is required"

NEXUS_URL="$(strip_trailing_slash "$NEXUS_URL")"

curl_args=()
if [[ "$SKIP_TLS_VERIFY" == "true" ]]; then
  curl_args+=("-k")
fi

if [[ -n "${NEXUS_PASS:-}" ]]; then
  NEXUS_PASS_VALUE="$NEXUS_PASS"
elif [[ "$COMMAND" == "export" && -n "${SOURCE_PASS:-}" ]]; then
  NEXUS_PASS_VALUE="$SOURCE_PASS"
elif [[ "$COMMAND" == "import" && -n "${TARGET_PASS:-}" ]]; then
  NEXUS_PASS_VALUE="$TARGET_PASS"
else
  read -rsp "Nexus password for $NEXUS_USER: " NEXUS_PASS_VALUE
  echo
fi

BASE_API="$NEXUS_URL/service/rest/v1/security"
CONTENT_SELECTORS_API="$BASE_API/content-selectors"
PRIVILEGES_API="$BASE_API/privileges"
ROLES_API="$BASE_API/roles"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

response_file="$tmp_dir/response.txt"

api_get() {
  local url="$1"

  curl ${curl_args[@]+"${curl_args[@]}"} -sS -o "$response_file" -w "%{http_code}" \
    -u "$NEXUS_USER:$NEXUS_PASS_VALUE" \
    -H "Accept: application/json" \
    "$url"
}

api_post() {
  local url="$1"
  local payload="$2"

  curl ${curl_args[@]+"${curl_args[@]}"} -sS -o "$response_file" -w "%{http_code}" \
    -u "$NEXUS_USER:$NEXUS_PASS_VALUE" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "$payload" \
    "$url"
}

api_put() {
  local url="$1"
  local payload="$2"

  curl ${curl_args[@]+"${curl_args[@]}"} -sS -o "$response_file" -w "%{http_code}" \
    -u "$NEXUS_USER:$NEXUS_PASS_VALUE" \
    -X PUT \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "$payload" \
    "$url"
}

api_delete() {
  local url="$1"

  curl ${curl_args[@]+"${curl_args[@]}"} -sS -o "$response_file" -w "%{http_code}" \
    -u "$NEXUS_USER:$NEXUS_PASS_VALUE" \
    -X DELETE \
    -H "Accept: application/json" \
    "$url"
}

check_api_access() {
  local url="$1"
  local label="$2"

  local status
  status="$(api_get "$url")"

  if [[ "$status" != "200" ]]; then
    cat "$response_file" >&2
    fail "Failed to access $label API. HTTP $status"
  fi
}

export_json_array() {
  local url="$1"
  local label="$2"
  local output_file="$3"
  local jq_filter="$4"

  log "Exporting $label..."

  local status
  status="$(api_get "$url")"

  if [[ "$status" != "200" ]]; then
    cat "$response_file" >&2
    fail "Failed to export $label. HTTP $status"
  fi

  jq "$jq_filter" "$response_file" > "$output_file"

  local count
  count="$(jq 'length' "$output_file")"
  log "Exported $label: $count"
}

export_security() {
  if [[ -f "$FILE" && "$OVERWRITE" != "true" ]]; then
    fail "Output file already exists: $FILE. Use --overwrite to replace it."
  fi

  selectors_file="$tmp_dir/content-selectors.json"
  privileges_file="$tmp_dir/privileges.json"
  roles_file="$tmp_dir/roles.json"

  echo '[]' > "$selectors_file"
  echo '[]' > "$privileges_file"
  echo '[]' > "$roles_file"

  log "Exporting security objects from: $NEXUS_URL"

  if [[ "$INCLUDE_SELECTORS" == "true" ]]; then
    export_json_array \
      "$CONTENT_SELECTORS_API" \
      "content selectors" \
      "$selectors_file" \
      '
        if type == "array" then
          [.[] | {name, description, expression}]
        else
          error("Expected content selectors response to be a JSON array")
        end
      '
  fi

  if [[ "$INCLUDE_PRIVILEGES" == "true" ]]; then
    export_json_array \
      "$PRIVILEGES_API" \
      "privileges" \
      "$privileges_file" \
      '
        if type == "array" then
          [
            .[]
            | del(.readOnly)
            | select(.name != null)
            | select(.type != null)
          ]
        else
          error("Expected privileges response to be a JSON array")
        end
      '
  fi

  if [[ "$INCLUDE_ROLES" == "true" ]]; then
    export_json_array \
      "$ROLES_API" \
      "roles" \
      "$roles_file" \
      '
        if type == "array" then
          [
            .[]
            | del(.readOnly)
            | select(.id != null)
            | {
                id,
                name,
                description,
                privileges: (.privileges // []),
                roles: (.roles // [])
              }
          ]
        else
          error("Expected roles response to be a JSON array")
        end
      '
  fi

  jq -n \
    --arg exportedAt "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" \
    --arg sourceUrl "$NEXUS_URL" \
    --slurpfile contentSelectors "$selectors_file" \
    --slurpfile privileges "$privileges_file" \
    --slurpfile roles "$roles_file" \
    '{
      exportedAt: $exportedAt,
      sourceUrl: $sourceUrl,
      contentSelectors: $contentSelectors[0],
      privileges: $privileges[0],
      roles: $roles[0]
    }' > "$FILE"

  log "Export complete."
  log "Output file: $FILE"
  log "Content selectors: $(jq '.contentSelectors | length' "$FILE")"
  log "Privileges:        $(jq '.privileges | length' "$FILE")"
  log "Roles:             $(jq '.roles | length' "$FILE")"
}

import_content_selectors() {
  local count
  count="$(jq '.contentSelectors // [] | length' "$FILE")"

  if [[ "$count" -eq 0 ]]; then
    log "No content selectors to import."
    return
  fi

  log "Importing content selectors: $count"

  local created=0
  local updated=0
  local failed=0

  while IFS= read -r selector; do
    local name
    name="$(echo "$selector" | jq -r '.name')"

    if [[ -z "$name" || "$name" == "null" ]]; then
      warn "Skipping content selector with missing name: $selector"
      failed=$((failed + 1))
      continue
    fi

    log "Content selector: $name"

    local post_status
    post_status="$(api_post "$CONTENT_SELECTORS_API" "$selector")"

    case "$post_status" in
      200|201|204)
        log "Created content selector: $name"
        created=$((created + 1))
        ;;
      400|409)
        log "Content selector already exists, skipping: $name"
        updated=$((updated + 1))
        ;;
      *)
        warn "Failed creating content selector '$name'. HTTP $post_status"
        cat "$response_file" >&2
        failed=$((failed + 1))
        ;;
    esac
  done < <(jq -c '.contentSelectors // [] | .[]' "$FILE")

  log "Content selectors complete. Created=$created Updated=$updated Failed=$failed"

  if [[ "$failed" -gt 0 ]]; then
    return 1
  fi
}

import_privileges() {
  local count
  count="$(jq '.privileges // [] | length' "$FILE")"

  if [[ "$count" -eq 0 ]]; then
    log "No privileges to import."
    return
  fi

  log "Importing privileges: $count"

  local created=0
  local updated=0
  local failed=0

  while IFS= read -r privilege; do
    local name
    local type
    name="$(echo "$privilege" | jq -r '.name')"
    type="$(echo "$privilege" | jq -r '.type')"

    if [[ -z "$name" || "$name" == "null" || -z "$type" || "$type" == "null" ]]; then
      warn "Skipping privilege with missing name or type: $privilege"
      failed=$((failed + 1))
      continue
    fi

    log "Privilege: $name [$type]"

    local payload
    payload="$(echo "$privilege" | jq '
      del(.readOnly)
      | if .type == "repository-content-selector" and (.format == null or .format == "") then
          .format = "*"
        else
          .
        end
      | .name = (.name | gsub("[^a-zA-Z0-9_.\\-]"; "-") | gsub("-+"; "-") | ltrimstr("-") | rtrimstr("-"))
    ')"

    # Use the sanitized name for all API calls
    local sanitized_name
    sanitized_name="$(echo "$payload" | jq -r '.name')"

    # For custom (non-nx-*) privileges: always delete first (if exists) then recreate.
    # For nx-* built-ins: attempt create and skip if already present.
    if [[ "$name" != nx-* ]]; then
      local encoded_name
      encoded_name="$(urlencode "$sanitized_name")"
      local delete_status
      delete_status="$(api_delete "$PRIVILEGES_API/$encoded_name")"
      if [[ "$delete_status" == "200" || "$delete_status" == "204" ]]; then
        log "Deleted existing privilege: $sanitized_name"
      elif [[ "$delete_status" == "404" ]]; then
        log "Privilege does not exist yet, will create: $sanitized_name"
      else
        warn "Unexpected status deleting privilege '$sanitized_name'. HTTP $delete_status"
        cat "$response_file" >&2
        failed=$((failed + 1))
        continue
      fi

      local post_status
      post_status="$(api_post "$PRIVILEGES_API/$type" "$payload")"
      case "$post_status" in
        200|201|204)
          log "Created privilege: $sanitized_name"
          created=$((created + 1))
          ;;
        *)
          warn "Failed creating privilege '$sanitized_name'. HTTP $post_status"
          cat "$response_file" >&2
          failed=$((failed + 1))
          ;;
      esac
    else
      local post_status
      post_status="$(api_post "$PRIVILEGES_API/$type" "$payload")"
      case "$post_status" in
        200|201|204)
          log "Created privilege: $name"
          created=$((created + 1))
          ;;
        400|409)
          log "Privilege already exists, skipping: $name"
          updated=$((updated + 1))
          ;;
        *)
          warn "Failed creating privilege '$name'. HTTP $post_status"
          cat "$response_file" >&2
          failed=$((failed + 1))
          ;;
      esac
    fi
  done < <(jq -c '.privileges // [] | .[]' "$FILE")

  log "Privileges complete. Created=$created Updated=$updated Failed=$failed"

  if [[ "$failed" -gt 0 ]]; then
    return 1
  fi
}

import_roles() {
  local count
  count="$(jq '.roles // [] | length' "$FILE")"

  if [[ "$count" -eq 0 ]]; then
    log "No roles to import."
    return
  fi

  log "Importing roles: $count"

  local created=0
  local updated=0
  local failed=0

  # Pass 1: Create all role shells with empty nested-roles array.
  # This ensures every role ID exists before pass 2 wires up cross-references.
  # Non-nx-* roles are always deleted first then recreated fresh.
  log "Roles pass 1/2: creating role shells..."
  while IFS= read -r role; do
    local id
    id="$(echo "$role" | jq -r '.id')"

    if [[ -z "$id" || "$id" == "null" ]]; then
      warn "Skipping role with missing id: $role"
      failed=$((failed + 1))
      continue
    fi

    local encoded_id
    encoded_id="$(urlencode "$id")"

    local shell_payload
    shell_payload="$(
      echo "$role" | jq '{
        id,
        name,
        description,
        privileges: (.privileges // []),
        roles: []
      }'
    )"

    if [[ "$id" != nx-* ]]; then
      # Non-nx-* roles: delete first (ignore 404), then always create fresh
      local delete_status
      delete_status="$(api_delete "$ROLES_API/$encoded_id")"
      if [[ "$delete_status" == "200" || "$delete_status" == "204" ]]; then
        log "Deleted existing role: $id"
      elif [[ "$delete_status" == "404" ]]; then
        log "Role does not exist yet, will create: $id"
      else
        warn "Unexpected status deleting role '$id'. HTTP $delete_status"
        cat "$response_file" >&2
        failed=$((failed + 1))
        continue
      fi

      local post_status
      post_status="$(api_post "$ROLES_API" "$shell_payload")"
      case "$post_status" in
        200|201|204)
          log "Created role shell: $id"
          created=$((created + 1))
          ;;
        *)
          warn "Failed creating role shell '$id'. HTTP $post_status"
          cat "$response_file" >&2
          failed=$((failed + 1))
          ;;
      esac
    else
      # nx-* roles: attempt create, skip if already present
      local post_status
      post_status="$(api_post "$ROLES_API" "$shell_payload")"
      case "$post_status" in
        200|201|204)
          log "Created role shell: $id"
          created=$((created + 1))
          ;;
        400|409)
          if grep -q 'PARAMETER id' "$response_file" 2>/dev/null; then
            log "Role '$id' has corrupt id on server — deleting and recreating shell..."
            local delete_status
            delete_status="$(api_delete "$ROLES_API/$encoded_id")"
            if [[ "$delete_status" == "200" || "$delete_status" == "204" ]]; then
              local recreate_status
              recreate_status="$(api_post "$ROLES_API" "$shell_payload")"
              case "$recreate_status" in
                200|201|204)
                  log "Recreated role shell: $id"
                  created=$((created + 1))
                  ;;
                *)
                  warn "Failed recreating role shell '$id'. HTTP $recreate_status"
                  cat "$response_file" >&2
                  failed=$((failed + 1))
                  ;;
              esac
            else
              warn "Failed deleting corrupt role '$id'. HTTP $delete_status"
              cat "$response_file" >&2
              failed=$((failed + 1))
            fi
          else
            log "Role shell already exists, skipping: $id"
            updated=$((updated + 1))
          fi
          ;;
        *)
          warn "Failed creating role shell '$id'. HTTP $post_status"
          cat "$response_file" >&2
          failed=$((failed + 1))
          ;;
      esac
    fi
  done < <(jq -c '.roles // [] | .[]' "$FILE")

  # Pass 2: Update every role with its full payload including nested role references.
  log "Roles pass 2/2: applying nested role references..."
  while IFS= read -r role; do
    local id
    id="$(echo "$role" | jq -r '.id')"

    if [[ -z "$id" || "$id" == "null" ]]; then
      continue
    fi

    local encoded_id
    encoded_id="$(urlencode "$id")"

    local full_payload
    full_payload="$(
      echo "$role" | jq '{
        id,
        name,
        description,
        privileges: (.privileges // []),
        roles: (.roles // [])
      }'
    )"

    local put_status
    put_status="$(api_put "$ROLES_API/$encoded_id" "$full_payload")"

    case "$put_status" in
      200|204)
        log "Updated role: $id"
        ;;
      *)
        warn "Failed updating role '$id' with nested references. HTTP $put_status"
        cat "$response_file" >&2
        failed=$((failed + 1))
        ;;
    esac
  done < <(jq -c '.roles // [] | .[]' "$FILE")

  log "Roles complete. Created=$created Updated=$updated Failed=$failed"

  if [[ "$failed" -gt 0 ]]; then
    return 1
  fi
}

import_security() {
  [[ -f "$FILE" ]] || fail "Input file does not exist: $FILE"
  jq empty "$FILE" >/dev/null 2>&1 || fail "Input file is not valid JSON: $FILE"

  log "Input file: $FILE"
  log "Target Nexus: $NEXUS_URL"

  local selectors_count
  local privileges_count
  local roles_count

  selectors_count="$(jq '.contentSelectors // [] | length' "$FILE")"
  privileges_count="$(jq '.privileges // [] | length' "$FILE")"
  roles_count="$(jq '.roles // [] | length' "$FILE")"

  log "Content selectors in file: $selectors_count"
  log "Privileges in file:        $privileges_count"
  log "Roles in file:             $roles_count"

  if [[ "$DRY_RUN" == "true" ]]; then
    log "Dry run enabled. No changes will be made."

    if [[ "$INCLUDE_SELECTORS" == "true" ]]; then
      echo
      echo "Content selectors:"
      jq -r '.contentSelectors // [] | .[] | "- " + .name' "$FILE"
    fi

    if [[ "$INCLUDE_PRIVILEGES" == "true" ]]; then
      echo
      echo "Privileges:"
      jq -r '.privileges // [] | .[] | "- " + .name + " [" + .type + "]"' "$FILE"
    fi

    if [[ "$INCLUDE_ROLES" == "true" ]]; then
      echo
      echo "Roles:"
      jq -r '.roles // [] | .[] | "- " + .id' "$FILE"
    fi

    exit 0
  fi

  if [[ "$INCLUDE_SELECTORS" == "true" ]]; then
    check_api_access "$CONTENT_SELECTORS_API" "content selectors"
  fi

  if [[ "$INCLUDE_PRIVILEGES" == "true" ]]; then
    check_api_access "$PRIVILEGES_API" "privileges"
  fi

  if [[ "$INCLUDE_ROLES" == "true" ]]; then
    check_api_access "$ROLES_API" "roles"
  fi

  local failed=0

  # Correct order:
  #   1. content selectors
  #   2. privileges, including repository-content-selector privileges
  #   3. roles, which reference privileges and nested roles

  if [[ "$INCLUDE_SELECTORS" == "true" ]]; then
    import_content_selectors || failed=$((failed + 1))
  fi

  if [[ "$INCLUDE_PRIVILEGES" == "true" ]]; then
    import_privileges || failed=$((failed + 1))
  fi

  if [[ "$INCLUDE_ROLES" == "true" ]]; then
    import_roles || failed=$((failed + 1))
  fi

  echo
  log "Import finished."

  if [[ "$failed" -gt 0 ]]; then
    fail "One or more import sections had failures. Review output above."
  fi
}

case "$COMMAND" in
  export)
    export_security
    ;;
  import)
    import_security
    ;;
esac
