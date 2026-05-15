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

# Integrates with WSO2 M2 Release Plugin
#
# This script detects when it's run during an M2 release build and publishes
# the release artifacts to Maven Central (central.sonatype.com) using the
# Central Publisher Portal REST API.
#
# API Reference: https://central.sonatype.org/publish/publish-portal-api/
#
# Required environment variables:
#   IS_M2RELEASEBUILD       - Must be "true" for the script to run
#   MVN_RELEASE_VERSION     - The release version being published
#   CENTRAL_TOKEN_USERNAME  - Maven Central portal token username
#   CENTRAL_TOKEN_PASSWORD  - Maven Central portal token password
#
# Optional environment variables:
#   CENTRAL_API_URL                  - Override Maven Central API base URL
#   CENTRAL_PUBLISHING_TYPE          - AUTOMATIC or USER_MANAGED (default: USER_MANAGED).
#                                      Can also be set via <central.publishingType> in the
#                                      active Maven profile in settings.xml (env var takes
#                                      precedence if both are set).
#   CENTRAL_DEPLOYMENT_NAME          - Human-readable name for the deployment
#   GPG_REUSE_EXISTING_SIGNATURES    - Set to "true" to reuse .asc files already produced
#                                      by the Maven release build (same key used for Nexus).
#                                      No GPG key needs to be present on the agent.
#                                      If false/unset, the script signs using GPG_KEY_ID or
#                                      the agent's default GPG key.
#   GPG_KEY_ID                       - GPG key ID to use for signing (optional, uses default)
#   GPG_PASSPHRASE                   - GPG key passphrase (optional)
#   GPG_KEY_FILE                     - Path to an armored GPG private key file to import
#                                      automatically before signing (e.g. bound via Jenkins
#                                      Secret file credential). Imported once per build.
#   CENTRAL_SERVER_ID                - Server ID in settings.xml to read Central credentials
#                                      from (default: "maven-central"). Used as fallback when
#                                      CENTRAL_TOKEN_USERNAME / CENTRAL_TOKEN_PASSWORD are
#                                      not set as environment variables.
#   CLOSE_NEXUS_STAGE               - Must be "true" for Maven Central publishing to proceed.
#                                      When false/unset the build is a staging release and
#                                      artifacts should NOT be published to Maven Central.
#   CENTRAL_SETTINGS_XML             - Explicit path to a settings.xml file to read
#   MAVEN_SETTINGS_XML               - Alternative settings.xml path (lower priority)
#   M2RELEASE_GROUP_ID               - Maven group ID of the project
#   ARTIFACT_ID / M2RELEASE_ARTIFACT_ID - Maven artifact ID

set -euo pipefail

# ---------------------------------------------------------------------------
# Logging helpers
# ---------------------------------------------------------------------------
log_info() {
    echo -e "\033[1;32m[WSO2 Maven Central]\033[0m $1" >&2
}

log_warn() {
    echo -e "\033[1;33m[WARNING]\033[0m $1" >&2
}

log_error() {
    echo -e "\033[1;31m[ERROR]\033[0m $1" >&2
}

# ---------------------------------------------------------------------------
# Only run during an M2 release build
# ---------------------------------------------------------------------------
if [[ "${IS_M2RELEASEBUILD:-false}" != "true" ]]; then
    log_info "This is not an M2 release build. Exiting."
    exit 0
fi

log_info "Starting Maven Central artifact publishing for M2 release build"
log_info "================================================================"

# ---------------------------------------------------------------------------
# Only publish to Maven Central when the Nexus stage is being closed (i.e. a
# full release, not a staging-only build). This mirrors the CLOSE_NEXUS_STAGE
# flag used by nexus3.sh so both scripts behave consistently.
# ---------------------------------------------------------------------------
IS_CLOSED=${CLOSE_NEXUS_STAGE:-}
if [[ "$IS_CLOSED" != "true" ]]; then
    log_info "CLOSE_NEXUS_STAGE is not 'true' (value: '${IS_CLOSED}'). This is a staging build."
    log_info "Skipping Maven Central publishing — artifacts will not be uploaded."
    exit 0
fi
log_info "CLOSE_NEXUS_STAGE=true — proceeding with Maven Central publishing."

# ---------------------------------------------------------------------------
# Validate required environment variables
# ---------------------------------------------------------------------------
MVN_RELEASE_VERSION=${MVN_RELEASE_VERSION:-}
if [[ -z "$MVN_RELEASE_VERSION" ]]; then
    log_error "MVN_RELEASE_VERSION environment variable is not set"
    exit 1
fi

CENTRAL_TOKEN_USERNAME=${CENTRAL_TOKEN_USERNAME:-}
CENTRAL_TOKEN_PASSWORD=${CENTRAL_TOKEN_PASSWORD:-}

# ---------------------------------------------------------------------------
# settings.xml fallback: read Central credentials from Maven settings if the
# env vars are not already set.
# ---------------------------------------------------------------------------
read_settings_xml_credentials() {
    local server_id="${CENTRAL_SERVER_ID:-maven-central}"

    # Candidate settings.xml locations (first found wins).
    # NOTE: $WORKSPACE/settings.xml is listed first so the in-repo settings
    # (which contains the Central server entry) always wins over any global
    # settings file that may be set in MAVEN_SETTINGS_XML on the agent.
    local candidates=(
        "${WORKSPACE:-$(pwd)}/settings.xml"
        "${CENTRAL_SETTINGS_XML:-}"
        "${MAVEN_SETTINGS_XML:-}"
        "${M2_HOME:-}/conf/settings.xml"
        "${HOME}/.m2/settings.xml"
    )

    local settings_file=""
    for candidate in "${candidates[@]}"; do
        [[ -n "$candidate" && -f "$candidate" ]] && { settings_file="$candidate"; break; }
    done

    if [[ -z "$settings_file" ]]; then
        log_warn "No settings.xml found in any candidate location:"
        for candidate in "${candidates[@]}"; do
            [[ -n "$candidate" ]] && log_warn "  (not found) $candidate"
        done
        log_warn "Set CENTRAL_SETTINGS_XML or MAVEN_SETTINGS_XML to point to your settings.xml."
        return 1
    fi

    log_info "Reading Central credentials from settings.xml: $settings_file (server id: $server_id)"

    # Extract <username> and <password> from the matching <server> block using awk
    local parsed_user parsed_pass
    parsed_user=$(awk -v id="$server_id" '
        /<server>/{in_server=1; found=0; usr=""; pass=""}
        in_server && /<id>/{gsub(/.*<id>|<\/id>.*/,""); if ($0==id) found=1}
        in_server && found && /<username>/{gsub(/.*<username>|<\/username>.*/,""); usr=$0}
        in_server && found && /<password>/{gsub(/.*<password>|<\/password>.*/,""); pass=$0}
        in_server && /<\/server>/{if(found){print usr; exit} in_server=0}
    ' "$settings_file")
    parsed_pass=$(awk -v id="$server_id" '
        /<server>/{in_server=1; found=0; usr=""; pass=""}
        in_server && /<id>/{gsub(/.*<id>|<\/id>.*/,""); if ($0==id) found=1}
        in_server && found && /<password>/{gsub(/.*<password>|<\/password>.*/,""); pass=$0}
        in_server && /<\/server>/{if(found){print pass; exit} in_server=0}
    ' "$settings_file")

    if [[ -n "$parsed_user" && -n "$parsed_pass" ]]; then
        CENTRAL_TOKEN_USERNAME="$parsed_user"
        CENTRAL_TOKEN_PASSWORD="$parsed_pass"
        log_info "  Loaded Central credentials from settings.xml for server '$server_id'."
        return 0
    else
        log_warn "Server '$server_id' not found or missing username/password in $settings_file"
        return 1
    fi
}

if [[ -z "$CENTRAL_TOKEN_USERNAME" || -z "$CENTRAL_TOKEN_PASSWORD" ]]; then
    read_settings_xml_credentials || true
fi

if [[ -z "$CENTRAL_TOKEN_USERNAME" || -z "$CENTRAL_TOKEN_PASSWORD" ]]; then
    log_error "CENTRAL_TOKEN_USERNAME and CENTRAL_TOKEN_PASSWORD must both be set."
    log_error "Set them as environment variables, or add a <server id=\"${CENTRAL_SERVER_ID:-maven-central}\">"
    log_error "entry with <username> and <password> to your settings.xml."
    log_error "Generate a user token at: https://central.sonatype.com/account"
    exit 1
fi

# ---------------------------------------------------------------------------
# Read GPG configuration from settings.xml (wso2-release profile properties)
# Reads gpg.homedir and gpg.passphrase so the same key used by the Maven build
# is reused here — no separate GPG setup needed on the agent.
# ---------------------------------------------------------------------------
read_settings_xml_gpg() {
    # NOTE: $WORKSPACE/settings.xml is listed first so the in-repo settings
    # (which contains gpg.homedir/gpg.passphrase) always wins over a global file.
    local candidates=(
        "${WORKSPACE:-$(pwd)}/settings.xml"
        "${CENTRAL_SETTINGS_XML:-}"
        "${MAVEN_SETTINGS_XML:-}"
        "${M2_HOME:-}/conf/settings.xml"
        "${HOME}/.m2/settings.xml"
    )
    local settings_file=""
    for candidate in "${candidates[@]}"; do
        [[ -n "$candidate" && -f "$candidate" ]] && { settings_file="$candidate"; break; }
    done
    if [[ -z "$settings_file" ]]; then
        log_warn "No settings.xml found; cannot read GPG configuration."
        log_warn "Set CENTRAL_SETTINGS_XML or MAVEN_SETTINGS_XML to point to your settings.xml."
        return 1
    fi
    log_info "Reading GPG configuration from settings.xml: $settings_file"

    local homedir passphrase publishing_type
    homedir=$(awk '/<gpg.homedir>/{gsub(/.*<gpg.homedir>|<\/gpg.homedir>.*/,""); gsub(/^[[:space:]]+|[[:space:]]+$/,""); print; exit}' "$settings_file")
    passphrase=$(awk '/<gpg.passphrase>/{gsub(/.*<gpg.passphrase>|<\/gpg.passphrase>.*/,""); gsub(/^[[:space:]]+|[[:space:]]+$/,""); print; exit}' "$settings_file")
    publishing_type=$(awk '/<central.publishingType>/{gsub(/.*<central.publishingType>|<\/central.publishingType>.*/,""); gsub(/^[[:space:]]+|[[:space:]]+$/,""); print; exit}' "$settings_file")

    if [[ -n "$homedir" ]]; then
        GPG_HOMEDIR="${GPG_HOMEDIR:-$homedir}"
        log_info "  GPG homedir: $GPG_HOMEDIR"
    else
        log_warn "  <gpg.homedir> not found in settings.xml – GPG will use default keyring."
    fi
    if [[ -n "$passphrase" && -z "${GPG_PASSPHRASE:-}" ]]; then
        GPG_PASSPHRASE="$passphrase"
        log_info "  GPG passphrase loaded."
    fi
    if [[ -n "$publishing_type" && -z "${CENTRAL_PUBLISHING_TYPE:-}" ]]; then
        CENTRAL_PUBLISHING_TYPE="$publishing_type"
        log_info "  Central publishing type: $CENTRAL_PUBLISHING_TYPE (from settings.xml)"
    fi
}

GPG_HOMEDIR=${GPG_HOMEDIR:-}
CENTRAL_PUBLISHING_TYPE=${CENTRAL_PUBLISHING_TYPE:-}
read_settings_xml_gpg || true

if [[ -n "${GPG_HOMEDIR:-}" ]]; then
    log_info "GPG homedir in use: $GPG_HOMEDIR"
    # GnuPG 2.1+ no longer reads secring.gpg (legacy GnuPG 1.x secret key store).
    # If the keyring was set up with GnuPG 1.x (or maven-gpg-plugin's secring config),
    # import secring.gpg once so GnuPG 2.1+ can find the secret key.
    if [[ -f "${GPG_HOMEDIR}/secring.gpg" ]]; then
        if ! gpg --homedir "$GPG_HOMEDIR" --batch --list-secret-keys >/dev/null 2>&1; then
            log_info "Importing legacy secring.gpg into GnuPG 2.1+ keybox format..."
            gpg --homedir "$GPG_HOMEDIR" --batch --import "${GPG_HOMEDIR}/secring.gpg" 2>&1 | while IFS= read -r line; do log_info "  gpg: $line"; done || true
        fi
    fi
else
    log_warn "GPG_HOMEDIR is not set – will use the agent's default GPG keyring (~/.gnupg)."
    log_warn "Ensure <gpg.homedir>/build/gpg-keys/.gnupg</gpg.homedir> is present in your settings.xml."
fi

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
CENTRAL_API_URL=${CENTRAL_API_URL:-"https://central.sonatype.com"}
# Remove trailing slash
CENTRAL_API_URL="${CENTRAL_API_URL%/}"

# AUTOMATIC: validate → auto-publish; USER_MANAGED: validate → wait for manual publish
# Value precedence: env var > settings.xml <central.publishingType> > default (USER_MANAGED)
CENTRAL_PUBLISHING_TYPE=${CENTRAL_PUBLISHING_TYPE:-"USER_MANAGED"}

# Build Bearer token: base64(username:password)
BEARER_TOKEN=$(printf "%s:%s" "$CENTRAL_TOKEN_USERNAME" "$CENTRAL_TOKEN_PASSWORD" | base64 | tr -d '\n')

# GPG key ID – optional, only set if you need to select a specific key from the keyring.
# When GPG_HOMEDIR is set (read from settings.xml), GPG uses the default key in that keyring.
GPG_KEY_ID=${GPG_KEY_ID:-}

GROUP_ID=${M2RELEASE_GROUP_ID:-}
if [[ -z "$GROUP_ID" ]]; then
    log_error "M2RELEASE_GROUP_ID is not set."
    log_error "Without a group ID the repository scan root broadens to the entire"
    log_error ".repository directory and can pick up unrelated artifacts."
    log_error "Set M2RELEASE_GROUP_ID to the Maven group ID of the project being published."
    exit 1
fi

ARTIFACT_ID=${ARTIFACT_ID:-${M2RELEASE_ARTIFACT_ID:-$(grep -m1 "<artifactId>" pom.xml 2>/dev/null \
    | sed -e 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/' \
    || mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)}}

WORKSPACE_DIR="$(pwd)"
log_info "Working in directory: $WORKSPACE_DIR"

log_info "Release details:"
log_info "  Group ID:         $GROUP_ID"
log_info "  Artifact ID:      $ARTIFACT_ID"
log_info "  Version:          $MVN_RELEASE_VERSION"
log_info "  Central API URL:  $CENTRAL_API_URL"
log_info "  Publishing type:  $CENTRAL_PUBLISHING_TYPE"

# ---------------------------------------------------------------------------
# Dependency checks
# ---------------------------------------------------------------------------
check_dependencies() {
    local missing=()
    command -v gpg     >/dev/null 2>&1 || missing+=('gpg')
    command -v zip     >/dev/null 2>&1 || missing+=('zip')
    command -v curl    >/dev/null 2>&1 || missing+=('curl')
    command -v python3 >/dev/null 2>&1 || missing+=('python3')
    command -v md5sum  >/dev/null 2>&1 || \
        command -v md5    >/dev/null 2>&1 || missing+=('md5sum or md5')
    command -v sha1sum >/dev/null 2>&1 || \
        command -v shasum  >/dev/null 2>&1 || missing+=('sha1sum or shasum')

    if [[ "${#missing[@]}" -gt 0 ]]; then
        log_error "Missing required tools: ${missing[*]}"
        exit 1
    fi

    # Verify a GPG secret key is actually available before attempting to sign anything.
    # Maven Central rejects bundles without valid GPG signatures.
    # Skip this check if GPG_REUSE_EXISTING_SIGNATURES=true, which instructs the script
    # to reuse .asc files already produced by the Maven release build (e.g. from Nexus signing).
    # In that case, gpg does not need to be invoked at all.
    if [[ "${GPG_REUSE_EXISTING_SIGNATURES:-false}" != "true" ]]; then
        local gpg_check_opts=()
        [[ -n "${GPG_HOMEDIR:-}" ]] && gpg_check_opts+=("--homedir" "$GPG_HOMEDIR")
        [[ -n "${GPG_KEY_ID:-}" ]] && gpg_check_opts+=("--local-user" "$GPG_KEY_ID")
        if ! gpg --batch --list-secret-keys "${gpg_check_opts[@]}" >/dev/null 2>&1; then
            log_error "====================================================================="
            log_error "No GPG secret key found. Maven Central requires all artifacts to be"
            log_error "signed with a GPG key."
            log_error ""
            log_error "Options:"
            log_error "  A) Reuse the .asc files already produced by the Maven release build"
            log_error "     (the same key used when uploading to Nexus):"
            log_error "       Set GPG_REUSE_EXISTING_SIGNATURES=true"
            log_error "     The script will copy existing .asc files into the bundle instead"
            log_error "     of re-signing, so no GPG key is needed on this agent."
            log_error ""
            log_error "  B) Sign with a key available on this agent:"
            log_error "       gpg --import <private-key.asc>"
            log_error "       Set GPG_KEY_ID=<fingerprint>  (optional, uses default key)"
            log_error "       Set GPG_PASSPHRASE=<passphrase>  (if key is passphrase-protected)"
            log_error "====================================================================="
            exit 1
        fi
    else
        log_info "GPG_REUSE_EXISTING_SIGNATURES=true: will reuse .asc files from the build (no re-signing)."
    fi
}

# Cross-platform md5 helper
compute_md5() {
    local file="$1"
    if command -v md5sum >/dev/null 2>&1; then
        md5sum "$file" | awk '{print $1}'
    else
        md5 -q "$file"
    fi
}

# Cross-platform sha1 helper
compute_sha1() {
    local file="$1"
    if command -v sha1sum >/dev/null 2>&1; then
        sha1sum "$file" | awk '{print $1}'
    else
        shasum -a 1 "$file" | awk '{print $1}'
    fi
}

# ---------------------------------------------------------------------------
# GPG key auto-import
# If GPG_KEY_FILE is set (e.g. bound via Jenkins Secret file credential),
# import the private key and set ultimate trust automatically.
# ---------------------------------------------------------------------------
if [[ -n "${GPG_KEY_FILE:-}" ]]; then
    if [[ ! -f "$GPG_KEY_FILE" ]]; then
        log_error "GPG_KEY_FILE is set but the file does not exist: $GPG_KEY_FILE"
        exit 1
    fi
    log_info "Importing GPG private key from: $GPG_KEY_FILE"
    _gpg_import_opts=("--batch")
    [[ -n "${GPG_HOMEDIR:-}" ]] && _gpg_import_opts+=("--homedir" "$GPG_HOMEDIR")
    gpg "${_gpg_import_opts[@]}" --import "$GPG_KEY_FILE"
    # Grant ultimate trust so non-interactive signing works without prompts
    if [[ -n "${GPG_KEY_ID:-}" ]]; then
        echo "${GPG_KEY_ID}:6:" | gpg "${_gpg_import_opts[@]}" --import-ownertrust
    else
        # Trust all keys just imported (extract fingerprints from the file)
        gpg "${_gpg_import_opts[@]}" --with-colons --with-fingerprint --import-options show-only --import "$GPG_KEY_FILE" 2>/dev/null \
            | awk -F: '/^fpr/{print $10":6:"}' \
            | gpg "${_gpg_import_opts[@]}" --import-ownertrust
    fi
    log_info "GPG key imported and trusted."
fi

check_dependencies

# ---------------------------------------------------------------------------
# find_artifacts  – identical logic to nexus3.sh
# ---------------------------------------------------------------------------
find_artifacts() {
    local workspace_dir="$1"
    local group_id="$2"
    local artifact_id="$3"
    local version="$4"
    local artifacts=()

    log_info "Looking for artifacts in possible locations..."

    # 1. Custom artifacts directory (primary – matches ReleaseEnvironment.java)
    local artifacts_dir="${workspace_dir}/artifacts/${artifact_id}/${version}"
    if [[ -d "$artifacts_dir" ]]; then
        log_info "Found artifacts directory at: $artifacts_dir"
        artifacts+=("$artifacts_dir")
        echo "${artifacts[*]}"
        return 0
    fi

    # 2. target/checkout
    local target_dir="${workspace_dir}/target/checkout"
    if [[ -d "$target_dir" ]]; then
        log_info "Found target directory at: $target_dir"
        artifacts+=("$target_dir")
    fi

    # 3. target/checkout/target
    local checkout_target="${workspace_dir}/target/checkout/target"
    if [[ -d "$checkout_target" ]]; then
        log_info "Found checkout target directory at: $checkout_target"
        artifacts+=("$checkout_target")
    fi

    # 4. Maven local repository
    # Pass the group-level directory so all sibling modules at the same version are discovered.
    # The version filter passed to collect_all_artifacts ensures only the release version is uploaded.
    local group_path
    group_path=$(echo "$group_id" | tr '.' '/')
    local repo_group_dir="${workspace_dir}/.repository/${group_path}"
    if [[ -d "$repo_group_dir" ]]; then
        log_info "Found Maven repository group directory at: $repo_group_dir"
        artifacts+=("$repo_group_dir")
    fi

    echo "${artifacts[*]}"
}

# ---------------------------------------------------------------------------
# collect_all_artifacts BASE_DIR [FILTER_VERSION]
# If FILTER_VERSION is provided, only artifacts matching that version are emitted.
# Supports two layouts:
#   1. Maven local repository layout (.pom files) – coordinates derived from path.
#   2. Maven project checkout layout (pom.xml files) – coordinates read from XML.
# Output format (one line per file): groupId|artifactId|version|type|filepath
# ---------------------------------------------------------------------------
collect_all_artifacts() {
    local base_dir="$1"
    local filter_version="${2:-}"
    local found_any=0

    log_info "Recursively scanning for Maven artifacts in: $base_dir"
    [[ -n "$filter_version" ]] && log_info "  (filtering to version: $filter_version)"

    # Determine the root used for stripping group paths in Maven repo layout.
    # If base_dir is inside a .repository directory, use that as the strip root
    # so that e.g. .repository/com/wso2/test yields group com.wso2.test.
    local strip_root
    if [[ "$base_dir" == *"/.repository"* || "$base_dir" == *"/.repository" ]]; then
        strip_root="${base_dir%%/.repository*}/.repository"
    else
        strip_root="$base_dir"
    fi

    # --- Strategy 1: Maven local repository layout (.pom files) ---
    while IFS= read -r pom_file; do
        local dir version artifact_id group_path_dir rel_path group_id
        dir=$(dirname "$pom_file")
        version=$(basename "$dir")
        artifact_id=$(basename "$(dirname "$dir")")
        group_path_dir=$(dirname "$(dirname "$dir")")
        rel_path="${group_path_dir#${strip_root}}"
        rel_path="${rel_path#/}"
        group_id=$(echo "$rel_path" | tr '/' '.')

        [[ -z "$group_id" || -z "$artifact_id" || -z "$version" ]] && continue
        # Skip versions that don't match the release version (avoids uploading cached deps)
        [[ -n "$filter_version" && "$version" != "$filter_version" ]] && continue

        found_any=1
        echo "${group_id}|${artifact_id}|${version}|pom|${pom_file}"

        while IFS= read -r artifact_file; do
            local fname
            fname=$(basename "$artifact_file")
            if   [[ "$fname" == *"-sources.jar" ]]; then
                echo "${group_id}|${artifact_id}|${version}|sources|${artifact_file}"
            elif [[ "$fname" == *"-javadoc.jar" ]]; then
                echo "${group_id}|${artifact_id}|${version}|javadoc|${artifact_file}"
            elif [[ "$fname" == *".war" ]]; then
                echo "${group_id}|${artifact_id}|${version}|war|${artifact_file}"
            elif [[ "$fname" == *".zip" ]]; then
                echo "${group_id}|${artifact_id}|${version}|zip|${artifact_file}"
            elif [[ "$fname" == *".jar" ]]; then
                echo "${group_id}|${artifact_id}|${version}|jar|${artifact_file}"
            fi
        done < <(find "$dir" -maxdepth 1 -type f \( -name "*.jar" -o -name "*.war" -o -name "*.zip" \) 2>/dev/null)

    done < <(find "$base_dir" -type f -name "*.pom" 2>/dev/null | sort)

    # --- Strategy 2: Maven project checkout layout (pom.xml files) ---
    if [[ "$found_any" -eq 0 ]]; then
        log_info "No .pom files found; falling back to pom.xml-based scan"
        while IFS= read -r pom_xml; do
            local module_dir target_dir group_id artifact_id version
            module_dir=$(dirname "$pom_xml")
            target_dir="${module_dir}/target"

            [[ -d "$target_dir" ]] || continue

            group_id=$(grep -m1 '<groupId>' "$pom_xml" \
                | sed 's/.*<groupId>\(.*\)<\/groupId>.*/\1/' | tr -d '[:space:]')
            artifact_id=$(grep -m1 '<artifactId>' "$pom_xml" \
                | sed 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/' | tr -d '[:space:]')
            version=$(grep -m1 '<version>' "$pom_xml" \
                | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')

            [[ -z "$group_id" || -z "$artifact_id" || -z "$version" ]] && continue
            # Skip snapshot versions
            [[ "$version" == *"-SNAPSHOT" ]] && continue
            # Skip versions that don't match the release version filter
            [[ -n "$filter_version" && "$version" != "$filter_version" ]] && continue

            local pom_file="${target_dir}/${artifact_id}-${version}.pom"
            [[ -f "$pom_file" ]] || pom_file="$pom_xml"

            echo "${group_id}|${artifact_id}|${version}|pom|${pom_file}"

            while IFS= read -r artifact_file; do
                local fname
                fname=$(basename "$artifact_file")
                if   [[ "$fname" == *"-sources.jar" ]]; then
                    echo "${group_id}|${artifact_id}|${version}|sources|${artifact_file}"
                elif [[ "$fname" == *"-javadoc.jar" ]]; then
                    echo "${group_id}|${artifact_id}|${version}|javadoc|${artifact_file}"
                elif [[ "$fname" == *".war" ]]; then
                    echo "${group_id}|${artifact_id}|${version}|war|${artifact_file}"
                elif [[ "$fname" == *".zip" ]]; then
                    echo "${group_id}|${artifact_id}|${version}|zip|${artifact_file}"
                elif [[ "$fname" == *".jar" ]]; then
                    echo "${group_id}|${artifact_id}|${version}|jar|${artifact_file}"
                fi
            done < <(find "$target_dir" -maxdepth 1 -type f \( -name "*.jar" -o -name "*.war" -o -name "*.zip" \) 2>/dev/null)

        done < <(find "$base_dir" -name "pom.xml" -not -path "*/target/*" 2>/dev/null | sort)
    fi
}

# ---------------------------------------------------------------------------
# generate_checksums_and_signature
# Creates .md5, .sha1, and .asc files alongside the given staged file.
#
# Arguments:
#   $1 - staged destination file (inside bundle_dir)
#   $2 - original source file path (to look for a pre-existing .asc)
#
# Signature strategy:
#   - If a .asc already exists next to the SOURCE file AND GPG_KEY_ID is NOT
#     explicitly set, reuse that signature (avoids re-signing with a different
#     key than the one used during the Nexus build / Maven release).
#   - Otherwise, generate a fresh signature (GPG_KEY_ID override requested, or
#     no pre-existing .asc was found).
# ---------------------------------------------------------------------------
generate_checksums_and_signature() {
    local dest_file="$1"
    local src_file="${2:-$dest_file}"

    # MD5
    compute_md5 "$dest_file" > "${dest_file}.md5"

    # SHA1
    compute_sha1 "$dest_file" > "${dest_file}.sha1"

    # GPG signature
    local src_asc="${src_file}.asc"
    if [[ "${GPG_REUSE_EXISTING_SIGNATURES:-false}" == "true" ]]; then
        # Reuse mode: expect a pre-existing .asc from the Maven release build.
        if [[ -f "$src_asc" ]]; then
            log_info "    Reusing existing GPG signature: $(basename "$src_asc")"
            cp "$src_asc" "${dest_file}.asc"
        else
            log_error "GPG_REUSE_EXISTING_SIGNATURES=true but no .asc found for: $src_file"
            log_error "  Expected: $src_asc"
            log_error "  Ensure the Maven release build produced GPG signatures (maven-gpg-plugin)."
            return 1
        fi
    elif [[ -f "$src_asc" && -z "${GPG_KEY_ID:-}" ]]; then
        # Auto-reuse: .asc already exists and no explicit key override requested.
        log_info "    Reusing existing GPG signature: $(basename "$src_asc")"
        cp "$src_asc" "${dest_file}.asc"
    else
        if [[ -f "$src_asc" && -n "${GPG_KEY_ID:-}" ]]; then
            log_warn "    Pre-existing .asc found but GPG_KEY_ID is set – re-signing with key: $GPG_KEY_ID"
        fi
        local gpg_opts=("--batch" "--yes" "--armor" "--detach-sign")
        [[ -n "${GPG_HOMEDIR:-}" ]] && gpg_opts+=("--homedir" "$GPG_HOMEDIR")
        [[ -n "${GPG_KEY_ID:-}" ]]  && gpg_opts+=("--local-user" "$GPG_KEY_ID")
        if [[ -n "${GPG_PASSPHRASE:-}" ]]; then
            gpg_opts+=("--passphrase-fd" "0" "--pinentry-mode" "loopback")
            echo "$GPG_PASSPHRASE" | gpg "${gpg_opts[@]}" --output "${dest_file}.asc" "$dest_file"
        else
            gpg "${gpg_opts[@]}" --output "${dest_file}.asc" "$dest_file"
        fi
    fi
}

# ---------------------------------------------------------------------------
# build_bundle_zip
#
# Assembles a deployment ZIP in Maven repository layout:
#   <groupId path>/<artifactId>/<version>/<files + checksums + signatures>
#
# Arguments:
#   $1 - path to the temp artifacts file (pipe-separated lines)
#   $2 - output ZIP file path
# ---------------------------------------------------------------------------
build_bundle_zip() {
    local artifacts_file="$1"
    local zip_path="$2"

    local bundle_dir
    bundle_dir=$(mktemp -d)
    # shellcheck disable=SC2064
    trap "rm -rf '$bundle_dir'" RETURN

    log_info "Assembling Maven Central bundle in: $bundle_dir"

    while IFS='|' read -r gid aid ver type src_file; do
        [[ -z "$src_file" || ! -f "$src_file" ]] && continue

        # Determine destination filename
        local dest_name
        local src_name
        src_name=$(basename "$src_file")

        case "$type" in
            pom)
                dest_name="${aid}-${ver}.pom"
                ;;
            sources)
                dest_name="${aid}-${ver}-sources.jar"
                ;;
            javadoc)
                dest_name="${aid}-${ver}-javadoc.jar"
                ;;
            war)
                dest_name="${aid}-${ver}.war"
                ;;
            zip)
                dest_name="${aid}-${ver}.zip"
                ;;
            jar)
                dest_name="${aid}-${ver}.jar"
                ;;
            *)
                dest_name="$src_name"
                ;;
        esac

        # Build Maven repository path: com/example/foo/1.0/
        local group_path
        group_path=$(echo "$gid" | tr '.' '/')
        local dest_dir="${bundle_dir}/${group_path}/${aid}/${ver}"
        mkdir -p "$dest_dir"

        local dest_file="${dest_dir}/${dest_name}"
        cp "$src_file" "$dest_file"

        log_info "  Staged: ${group_path}/${aid}/${ver}/${dest_name}"

        # Generate checksums and GPG signature (passing src_file so existing .asc can be reused)
        generate_checksums_and_signature "$dest_file" "$src_file"

    done < "$artifacts_file"

    # Zip the entire bundle_dir contents
    log_info "Creating ZIP bundle: $zip_path"
    (cd "$bundle_dir" && zip -r "$zip_path" . -x "*.DS_Store")

    log_info "Bundle ZIP created: $zip_path ($(du -sh "$zip_path" | cut -f1))"
}

# ---------------------------------------------------------------------------
# upload_bundle
# Uploads the ZIP to Maven Central and returns the deployment ID.
# ---------------------------------------------------------------------------
upload_bundle() {
    local zip_path="$1"
    local deployment_name="$2"

    local upload_url="${CENTRAL_API_URL}/api/v1/publisher/upload"
    upload_url+="?publishingType=${CENTRAL_PUBLISHING_TYPE}"
    upload_url+="&name=$(printf '%s' "$deployment_name" | python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.stdin.read()))')"

    log_info "Uploading bundle to Maven Central: $upload_url"

    local response_file
    response_file=$(mktemp /tmp/central_upload_response.XXXXXX)
    local http_code

    http_code=$(curl -s \
        -o "$response_file" \
        -w "%{http_code}" \
        --request POST \
        --header "Authorization: Bearer ${BEARER_TOKEN}" \
        --header "User-Agent: wso2-m2release-deployer" \
        --form "bundle=@${zip_path};type=application/octet-stream" \
        "$upload_url")

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        local deployment_id
        deployment_id=$(cat "$response_file" | tr -d '[:space:]')
        rm -f "$response_file"
        log_info "✅ Bundle uploaded successfully (HTTP $http_code)."
        log_info "   Deployment ID: $deployment_id"
        echo "$deployment_id"
        return 0
    else
        log_error "Bundle upload failed. HTTP $http_code"
        if [[ -f "$response_file" ]]; then
            log_error "Response: $(cat "$response_file")"
            rm -f "$response_file"
        fi
        return 1
    fi
}

# ---------------------------------------------------------------------------
# poll_deployment_status
# Polls the deployment status until it reaches a terminal state or times out.
# Returns 0 on PUBLISHED, 1 on FAILED or timeout.
# ---------------------------------------------------------------------------
poll_deployment_status() {
    local deployment_id="$1"
    # Maximum wait time in seconds (default 30 minutes)
    local max_wait="${CENTRAL_STATUS_TIMEOUT:-1800}"
    local poll_interval="${CENTRAL_POLL_INTERVAL:-30}"
    local elapsed=0

    local status_url="${CENTRAL_API_URL}/api/v1/publisher/status?id=${deployment_id}"
    local response_file
    response_file=$(mktemp /tmp/central_status_response.XXXXXX)

    log_info "Polling deployment status for ID: $deployment_id"
    log_info "  (max wait: ${max_wait}s, interval: ${poll_interval}s)"

    while [[ "$elapsed" -lt "$max_wait" ]]; do
        local http_code
        http_code=$(curl -s \
            -o "$response_file" \
            -w "%{http_code}" \
            --request POST \
            --header "Authorization: Bearer ${BEARER_TOKEN}" \
            --header "Content-Type: application/json" \
            --header "User-Agent: wso2-m2release-deployer" \
            "$status_url")

        if [[ "$http_code" -lt 200 || "$http_code" -ge 300 ]]; then
            log_warn "Status check returned HTTP $http_code – retrying..."
            rm -f "$response_file"
            sleep "$poll_interval"
            elapsed=$((elapsed + poll_interval))
            continue
        fi

        local state errors
        state=$(grep -o '"deploymentState":"[^"]*"' "$response_file" \
            | sed 's/"deploymentState":"//;s/"//' 2>/dev/null || true)
        errors=$(grep -o '"errors":\[[^]]*\]' "$response_file" 2>/dev/null || true)
        rm -f "$response_file"

        log_info "  Deployment state: ${state:-UNKNOWN} (${elapsed}s elapsed)"

        case "$state" in
            PUBLISHED)
                log_info "✅ Deployment PUBLISHED to Maven Central."
                return 0
                ;;
            FAILED)
                log_error "Deployment FAILED."
                [[ -n "$errors" ]] && log_error "Errors: $errors"
                return 1
                ;;
            VALIDATED)
                if [[ "$CENTRAL_PUBLISHING_TYPE" == "USER_MANAGED" ]]; then
                    log_info "✅ Deployment VALIDATED and awaiting manual action on the Publisher Portal."
                    log_info "   Visit ${CENTRAL_API_URL}/publishing/deployments to publish or drop."
                    return 0
                fi
                # AUTOMATIC publishing type: Central will auto-publish after validation.
                ;;
            PENDING|VALIDATING|PUBLISHING)
                # Still in progress – keep polling
                ;;
            *)
                log_warn "Unknown state: '$state' – continuing to poll..."
                ;;
        esac

        sleep "$poll_interval"
        elapsed=$((elapsed + poll_interval))
    done

    log_error "Timed out waiting for deployment $deployment_id to complete (${max_wait}s)."
    return 1
}

# ---------------------------------------------------------------------------
# publish_deployment
# Explicitly publishes a USER_MANAGED deployment that has been validated.
# ---------------------------------------------------------------------------
publish_deployment() {
    local deployment_id="$1"
    local publish_url="${CENTRAL_API_URL}/api/v1/publisher/deployment/${deployment_id}"

    log_info "Publishing deployment: $deployment_id"

    local response_file
    response_file=$(mktemp /tmp/central_publish_response.XXXXXX)
    local http_code
    http_code=$(curl -s \
        -o "$response_file" \
        -w "%{http_code}" \
        --request POST \
        --header "Authorization: Bearer ${BEARER_TOKEN}" \
        --header "User-Agent: wso2-m2release-deployer" \
        "$publish_url")

    rm -f "$response_file"

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        log_info "✅ Publish request accepted (HTTP $http_code)."
        return 0
    else
        log_error "Failed to publish deployment $deployment_id. HTTP $http_code"
        return 1
    fi
}

# ---------------------------------------------------------------------------
# drop_deployment
# Drops a deployment (VALIDATED or FAILED state) to clean up.
# ---------------------------------------------------------------------------
drop_deployment() {
    local deployment_id="$1"
    local drop_url="${CENTRAL_API_URL}/api/v1/publisher/deployment/${deployment_id}"

    log_warn "Dropping deployment: $deployment_id"

    local response_file
    response_file=$(mktemp /tmp/central_drop_response.XXXXXX)
    local http_code
    http_code=$(curl -s \
        -o "$response_file" \
        -w "%{http_code}" \
        --request DELETE \
        --header "Authorization: Bearer ${BEARER_TOKEN}" \
        --header "User-Agent: wso2-m2release-deployer" \
        "$drop_url")

    rm -f "$response_file"

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        log_info "Deployment $deployment_id dropped."
    else
        log_warn "Could not drop deployment $deployment_id. HTTP $http_code"
    fi
}

# ===========================================================================
# Main execution
# ===========================================================================

WORKSPACE_DIR="$(pwd)"
log_info "Working in directory: $WORKSPACE_DIR"

# Locate artifact base directories
ARTIFACT_DIRS=$(find_artifacts "$WORKSPACE_DIR" "$GROUP_ID" "$ARTIFACT_ID" "$MVN_RELEASE_VERSION")

if [[ -z "$ARTIFACT_DIRS" ]]; then
    log_error "No artifact base directories found"
    exit 1
fi

# Collect all artifacts from all base directories
TEMP_ARTIFACTS=$(mktemp)
trap 'rm -f "$TEMP_ARTIFACTS"' EXIT

while IFS= read -r base_dir; do
    [[ -d "$base_dir" ]] || continue
    collect_all_artifacts "$base_dir" "$MVN_RELEASE_VERSION" >> "$TEMP_ARTIFACTS"
done <<< "$(echo "$ARTIFACT_DIRS" | tr ' ' '\n')"

# Deduplicate
sort -u -o "$TEMP_ARTIFACTS" "$TEMP_ARTIFACTS"

if [[ ! -s "$TEMP_ARTIFACTS" ]]; then
    log_error "No artifacts found to publish"
    exit 1
fi

# Show discovered artifacts
log_info "Discovered the following artifacts:"
while IFS='|' read -r gid aid ver type fpath; do
    [[ -n "$fpath" ]] && log_info "  [${type}] ${gid}:${aid}:${ver}  ->  $fpath"
done < "$TEMP_ARTIFACTS"

# Build the deployment bundle ZIP
BUNDLE_ZIP="/tmp/central-bundle-${MVN_RELEASE_VERSION}-$$.zip"
trap 'rm -f "$TEMP_ARTIFACTS" "$BUNDLE_ZIP"' EXIT

build_bundle_zip "$TEMP_ARTIFACTS" "$BUNDLE_ZIP"

# Determine a human-readable deployment name
DEPLOYMENT_NAME="${CENTRAL_DEPLOYMENT_NAME:-${GROUP_ID}:${ARTIFACT_ID}:${MVN_RELEASE_VERSION}}"
log_info "Deployment name: $DEPLOYMENT_NAME"

# Upload the bundle to Maven Central
DEPLOYMENT_ID=$(upload_bundle "$BUNDLE_ZIP" "$DEPLOYMENT_NAME")
if [[ -z "$DEPLOYMENT_ID" ]]; then
    log_error "Failed to retrieve deployment ID after upload."
    exit 1
fi

# Poll until the deployment reaches a terminal state
if ! poll_deployment_status "$DEPLOYMENT_ID"; then
    log_error "Maven Central publishing did not complete successfully."
    log_error "Deployment ID: $DEPLOYMENT_ID"
    log_error "Check the deployment at: ${CENTRAL_API_URL}/publishing/deployments"
    exit 1
fi

log_info "Maven Central artifact publishing process completed successfully."
log_info "Deployment ID: $DEPLOYMENT_ID"
