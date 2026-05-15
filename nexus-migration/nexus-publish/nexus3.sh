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
# This script detects when it's run during an M2 release build and uploads
# the release artifacts to Nexus 3 using the REST API.

set -euo pipefail

# Log messages with formatting
log_info() {
    echo -e "\033[1;32m[WSO2 Maven Release]\033[0m $1" >&2
}

log_warn() {
    echo -e "\033[1;33m[WARNING]\033[0m $1" >&2
}

log_error() {
    echo -e "\033[1;31m[ERROR]\033[0m $1" >&2
}

# Helper function to detect Cloudflare blocks in response
detect_cloudflare_block() {
    local response_file="$1"
    
    if [[ -f "$response_file" ]]; then
        if grep -q "Cloudflare" "$response_file" || grep -q "challenge-platform" "$response_file" || grep -q "captcha" "$response_file"; then
            log_error "====================================================================================================="
            log_error "Cloudflare security block detected!"
            log_error "The request was blocked by Cloudflare security measures. This typically happens when:"
            log_error "  1. Your IP address has been flagged for suspicious activity"
            log_error "  2. There is a DDoS protection measure in place"
            log_error ""
            log_error "Possible solutions:"
            log_error "  1. Contact your network administrator to check if your IP is allowlisted"
            log_error "  2. Contact WSO2 infrastructure team for assistance"
            log_error "====================================================================================================="
            return 0
        fi
    fi
    return 1
}

# Only run this script if this is an M2 release build
if [[ "${IS_M2RELEASEBUILD:-false}" != "true" ]]; then
    log_info "This is not an M2 release build. Exiting."
    exit 0
fi

log_info "Starting Nexus 3 artifact upload for M2 release build"
log_info "====================================================="

# Get required environment variables
MVN_RELEASE_VERSION=${MVN_RELEASE_VERSION:-}
if [[ -z "$MVN_RELEASE_VERSION" ]]; then
    log_error "MVN_RELEASE_VERSION environment variable is not set"
    exit 1
fi


# Get Nexus URL primarily from Jenkins environment variables
# NEXUS_URL=${NEXUS_URL:-${M2RELEASE_NEXUS_URL:-${MVNEXT_NEXUS_URL:-}}}
# Fallback to default URL if not provided
NEXUS_URL=${NEXUS_URL:-"https://maven3-upgrade.wso2.org/nexus/"}

# Get Nexus username primarily from Jenkins environment variables
NEXUS_USER=${NEXUS_USER:-${M2RELEASE_NEXUS_USER:-${MVNEXT_NEXUS_USER:-}}}
# Fallback to default username if not provided
# NEXUS_USER=${NEXUS_USER:-"deployment"}

# Get Nexus password primarily from Jenkins environment variables
NEXUS_PASSWORD=${NEXUS_PASSWORD:-${M2RELEASE_NEXUS_PASSWORD:-${MVNEXT_NEXUS_PASSWORD:-}}}
# No default password set for security reasons - will log error if not provided
if [[ -z "$NEXUS_PASSWORD" ]]; then
    log_warn "No Nexus password provided. Upload may fail if authentication is required."
fi

IS_CLOSED=${CLOSE_NEXUS_STAGE:-}
log_info "CLOSE_NEXUS_STAGE: $IS_CLOSED"
if [[ "$IS_CLOSED" == "true" ]]; then
    NEXUS_REPOSITORY="releases"
    log_info "Using releases repository based on M2RELEASE_IS_CLOSED=$IS_CLOSED"
else
    NEXUS_REPOSITORY="staging"
    log_info "Using staging repository based on M2RELEASE_IS_CLOSED=$IS_CLOSED"
fi
log_info "Using Nexus repository: $NEXUS_REPOSITORY"

# API path
NEXUS_API_PATH=${NEXUS_API_PATH:-"service/rest/v1/components?repository="}

# Ensure the Nexus URL ends with a slash
[[ "${NEXUS_URL}" != */ ]] && NEXUS_URL="${NEXUS_URL}/"

# Define workspace directory early
WORKSPACE_DIR="$(pwd)"
log_info "Working in directory: $WORKSPACE_DIR"

# Get Maven coordinates
# First try explicitly provided values, then Jenkins environment, then try to extract from pom.xml directly
GROUP_ID=${M2RELEASE_GROUP_ID:-}
log_info "Using group ID: $GROUP_ID"

ARTIFACT_ID=${ARTIFACT_ID:-${M2RELEASE_ARTIFACT_ID:-$(grep -m1 "<artifactId>" pom.xml 2>/dev/null | sed -e 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/' || mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)}}

# Function to find artifacts from various possible locations,
# based on the same logic used in ReleaseEnvironment.java
find_artifacts() {
    local workspace_dir="$1"
    local group_id="$2"
    local artifact_id="$3"
    local version="$4"
    local artifacts=()
    
    log_info "Looking for artifacts in possible locations..."
    
    # 1. First check in custom artifacts directory (primary location used by ReleaseEnvironment.java)
    local artifacts_dir="${workspace_dir}/artifacts/${artifact_id}/${version}"
    if [[ -d "$artifacts_dir" ]]; then
        log_info "Found artifacts directory at: $artifacts_dir"
        artifacts+=("$artifacts_dir")
        return 0
    fi
    
    # 2. Check in target directory
    local target_dir="${workspace_dir}/target/checkout"
    if [[ -d "$target_dir" ]]; then
        log_info "Found target directory at: $target_dir"
        artifacts+=("$target_dir")
    fi

    # 3. Check in checkout target directory
    local checkout_target="${workspace_dir}/target/checkout/target"
    if [[ -d "$checkout_target" ]]; then
        log_info "Found checkout target directory at: $checkout_target"
        artifacts+=("$checkout_target")
    fi
    
    # 4. Check in Maven local repository
    # Pass the group-level directory so all sibling modules at the same version are discovered.
    # The version filter passed to collect_all_artifacts ensures only the release version is uploaded.
    local group_path
    group_path=$(echo "$group_id" | tr '.' '/')
    local repo_group_dir="${workspace_dir}/.repository/${group_path}"
    if [[ -d "$repo_group_dir" ]]; then
        log_info "Found Maven repository group directory at: $repo_group_dir"
        artifacts+=("$repo_group_dir")
    fi
    
    # Return as space-separated string
    echo "${artifacts[*]}"
}

log_info "Release details:"
log_info "  Group ID:    $GROUP_ID"
log_info "  Artifact ID: $ARTIFACT_ID"
log_info "  Version:     $MVN_RELEASE_VERSION"
log_info "  Nexus URL:   $NEXUS_URL"
log_info "  Repository:  $NEXUS_REPOSITORY"

# Recursively collect all Maven artifacts from a base directory.
# Supports two layouts:
#   1. Maven local repository layout (.repository/):
#      <base>/<group/path>/<artifactId>/<version>/<files>
#      Coordinates are derived from the directory path.
#   2. Maven project checkout layout (target/checkout/):
#      Coordinates are read from each module's pom.xml.
# Output format (one line per file): groupId|artifactId|version|type|filepath
# collect_all_artifacts BASE_DIR [FILTER_VERSION]
# If FILTER_VERSION is provided, only artifacts matching that version are emitted.
# This is used when scanning a broad directory (e.g. the .repository group dir)
# to avoid uploading unrelated cached dependencies.
collect_all_artifacts() {
    local base_dir="$1"
    local filter_version="${2:-}"
    local found_any=0

    log_info "Recursively scanning for Maven artifacts in: $base_dir"
    [[ -n "$filter_version" ]] && log_info "  (filtering to version: $filter_version)"

    # Determine the root used for stripping group paths in Maven repo layout.
    # If base_dir is inside a .repository directory, use that as the strip root
    # so that e.g. .repository/com/wso2/test/mytest/2.9.18 yields group com.wso2.test.
    local strip_root
    if [[ "$base_dir" == *"/.repository"* || "$base_dir" == *"/.repository" ]]; then
        strip_root="${base_dir%%/.repository*}/.repository"
    else
        strip_root="$base_dir"
    fi

    # --- Strategy 1: Maven local repository layout ---
    # Identify by presence of .pom files (not pom.xml)
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
            if [[ "$fname" == *"-sources.jar" ]]; then
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

    # --- Strategy 2: Maven project checkout layout ---
    # Used when no .pom files are found (e.g. target/checkout multi-module tree)
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

            # Prefer a generated .pom in target/, fall back to pom.xml
            local pom_file="${target_dir}/${artifact_id}-${version}.pom"
            [[ -f "$pom_file" ]] || pom_file="$pom_xml"

            echo "${group_id}|${artifact_id}|${version}|pom|${pom_file}"

            while IFS= read -r artifact_file; do
                local fname
                fname=$(basename "$artifact_file")
                if [[ "$fname" == *"-sources.jar" ]]; then
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

# Validate that all expected artifacts are present for every discovered component.
# Each component must have at least a .pom AND a main artifact (.jar or .war).
# Missing files indicate an incomplete Maven build, not a deployment issue.
# Returns 1 and logs errors if any component is incomplete.
validate_all_components() {
    local artifacts_file="$1"
    local validation_failed=0

    log_info "Validating all discovered components before upload..."

    while IFS='|' read -r gid aid ver; do
        local has_pom=0 has_main=0 missing=()

        while IFS='|' read -r _gid _aid _ver type file_path; do
            if [[ ! -f "$file_path" ]]; then
                missing+=("[$type] $file_path")
                continue
            fi
            [[ "$type" == "pom" ]]                              && has_pom=1
            [[ "$type" == "jar" || "$type" == "war" || "$type" == "zip" ]] && has_main=1
        done < <(grep "^${gid}|${aid}|${ver}|" "$artifacts_file")

        local component_ok=1

        if [[ "$has_pom" -eq 0 ]]; then
            log_error "Component ${gid}:${aid}:${ver} is missing its POM file."
            log_error "  This indicates an incomplete Maven build. Check the Maven release build logs."
            component_ok=0
        fi

        # A pom-only component (has_main=0) is valid — it is a pom-packaging aggregator/parent module.
        # Do NOT require a main artifact; only require the POM itself.

        if [[ "${#missing[@]}" -gt 0 ]]; then
            log_error "Component ${gid}:${aid}:${ver} has artifact file(s) listed but not found on disk:"
            for m in "${missing[@]}"; do
                log_error "    $m"
            done
            log_error "  This indicates an incomplete Maven build. Check the Maven release build logs."
            component_ok=0
        fi

        if [[ "$component_ok" -eq 1 ]]; then
            if [[ "$has_main" -eq 0 ]]; then
                log_info "  ✅ ${gid}:${aid}:${ver} — pom-only module (aggregator/parent), no main artifact required."
            else
                log_info "  ✅ ${gid}:${aid}:${ver} — all required artifacts present."
            fi
        else
            validation_failed=1
        fi

    done < <(cut -d'|' -f1-3 "$artifacts_file" | sort -u)

    if [[ "$validation_failed" -ne 0 ]]; then
        log_error "====================================================================================================="
        log_error "Artifact validation failed. One or more components are incomplete."
        log_error "This is likely caused by a failure during the Maven build/release phase, not the upload phase."
        log_error "Please review the Maven release build output above for compilation or packaging errors."
        log_error "====================================================================================================="
        return 1
    fi

    log_info "All components validated successfully."
    return 0
}

# Upload a single Maven component (one groupId:artifactId:version) to Nexus 3.
# artifacts_lines: newline-separated lines in the format groupId|artifactId|version|type|filepath
upload_to_nexus() {
    local group_id="$1"
    local artifact_id="$2"
    local version="$3"
    local artifacts_lines="$4"

    local upload_url="${NEXUS_URL}${NEXUS_API_PATH}${NEXUS_REPOSITORY}"

    log_info "Uploading component ${group_id}:${artifact_id}:${version} to: $upload_url"

    # Build curl command with form parameters
    local curl_cmd="curl -s -o /tmp/nexus_response.txt -w \"%{http_code}\" -u \"$NEXUS_USER:$NEXUS_PASSWORD\""
    curl_cmd+=" -H \"Accept: application/json\""
    curl_cmd+=" -H \"User-Agent: wso2-nexus-deployer\""

    # Add Maven coordinates
    curl_cmd+=" -F \"maven2.groupId=$group_id\""
    curl_cmd+=" -F \"maven2.artifactId=$artifact_id\""
    curl_cmd+=" -F \"maven2.version=$version\""
    curl_cmd+=" -F \"maven2.generate-pom=false\""

    # Set packaging based on found artifacts
    if echo "$artifacts_lines" | grep -q "|war|"; then
        curl_cmd+=" -F \"maven2.packaging=war\""
    elif echo "$artifacts_lines" | grep -q "|zip|"; then
        curl_cmd+=" -F \"maven2.packaging=zip\""
    elif echo "$artifacts_lines" | grep -qE "\|jar\|"; then
        curl_cmd+=" -F \"maven2.packaging=jar\""
    else
        # pom-only module (aggregator/parent)
        curl_cmd+=" -F \"maven2.packaging=pom\""
    fi

    local asset_index=1
    while IFS='|' read -r _gid _aid _ver type file_path; do
        [[ -z "$type" || -z "$file_path" ]] && continue

        if [[ ! -f "$file_path" ]]; then
            log_warn "File not found, skipping: $file_path"
            continue
        fi

        local file_name extension
        file_name=$(basename "$file_path")

        if [[ "$type" == "pom" ]]; then
            extension="pom"
            # Normalise pom.xml → <artifactId>-<version>.pom for Nexus
            if [[ "$file_name" == "pom.xml" ]]; then
                local temp_pom="/tmp/${artifact_id}-${version}.pom"
                cp "$file_path" "$temp_pom"
                file_path="$temp_pom"
                file_name="${artifact_id}-${version}.pom"
            fi
        elif [[ "$type" == "war" ]]; then
            extension="war"
        elif [[ "$type" == "zip" ]]; then
            extension="zip"
        else
            extension="jar"
        fi

        log_info "  [asset${asset_index}] $file_name  (type: $type)"

        curl_cmd+=" -F \"maven2.asset${asset_index}=@${file_path};filename=${file_name}\""
        curl_cmd+=" -F \"maven2.asset${asset_index}.extension=${extension}\""

        if [[ "$type" == "sources" || "$type" == "javadoc" ]]; then
            curl_cmd+=" -F \"maven2.asset${asset_index}.classifier=${type}\""
        fi

        ((asset_index++))
    done <<< "$artifacts_lines"

    if [[ "$asset_index" -eq 1 ]]; then
        log_warn "No valid asset files found for ${group_id}:${artifact_id}:${version}, skipping upload."
        return 0
    fi

    log_info "Executing upload to Nexus repository..."
    local http_code
    http_code=$(eval "$curl_cmd \"$upload_url\"")

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        log_info "✅ ${group_id}:${artifact_id}:${version} published successfully (HTTP $http_code)."
        rm -f /tmp/nexus_response.txt
        return 0
    else
        log_error "Failed to publish ${group_id}:${artifact_id}:${version}. HTTP $http_code"
        if [[ -f /tmp/nexus_response.txt ]]; then
            if detect_cloudflare_block "/tmp/nexus_response.txt"; then
                rm -f /tmp/nexus_response.txt
                return 1
            fi
            log_error "Response: $(cat /tmp/nexus_response.txt)"
            rm -f /tmp/nexus_response.txt
        fi
        return 1
    fi
}

# Main script execution
WORKSPACE_DIR="$(pwd)"
log_info "Working in directory: $WORKSPACE_DIR"

# Find the base artifact directory/directories
ARTIFACT_DIRS=$(find_artifacts "$WORKSPACE_DIR" "$GROUP_ID" "$ARTIFACT_ID" "$MVN_RELEASE_VERSION")

if [[ -z "$ARTIFACT_DIRS" ]]; then
    log_error "No artifact base directories found"
    exit 1
fi

# Collect all artifacts recursively from every base directory.
# Each line: groupId|artifactId|version|type|filepath
TEMP_ARTIFACTS=$(mktemp)
trap 'rm -f "$TEMP_ARTIFACTS"' EXIT

while IFS= read -r base_dir; do
    [[ -d "$base_dir" ]] || continue
    collect_all_artifacts "$base_dir" "$MVN_RELEASE_VERSION" >> "$TEMP_ARTIFACTS"
done <<< "$(echo "$ARTIFACT_DIRS" | tr ' ' '\n')"

# Remove duplicate lines (same file appearing via multiple base dirs)
sort -u -o "$TEMP_ARTIFACTS" "$TEMP_ARTIFACTS"

if [[ ! -s "$TEMP_ARTIFACTS" ]]; then
    log_error "No artifacts found to upload"
    exit 1
fi

# Display all discovered artifacts
log_info "Discovered the following artifacts:"
while IFS='|' read -r gid aid ver type fpath; do
    [[ -n "$fpath" ]] && log_info "  [${type}] ${gid}:${aid}:${ver}  ->  $fpath"
done < "$TEMP_ARTIFACTS"

# Validate all components are complete before attempting any uploads
if ! validate_all_components "$TEMP_ARTIFACTS"; then
    exit 1
fi

# Upload each unique Maven component (groupId:artifactId:version) separately
UPLOAD_FAILED=0
while IFS='|' read -r gid aid ver; do
    log_info "-------------------------------------------------------------"
    log_info "Processing component: ${gid}:${aid}:${ver}"
    component_artifacts=$(grep "^${gid}|${aid}|${ver}|" "$TEMP_ARTIFACTS")
    if ! upload_to_nexus "$gid" "$aid" "$ver" "$component_artifacts"; then
        log_error "Upload failed for component: ${gid}:${aid}:${ver}"
        UPLOAD_FAILED=1
    fi
done < <(cut -d'|' -f1-3 "$TEMP_ARTIFACTS" | sort -u)

if [[ "$UPLOAD_FAILED" -ne 0 ]]; then
    log_error "One or more components failed to upload. See errors above."
    exit 1
fi

log_info "M2 release artifact upload process completed successfully"
