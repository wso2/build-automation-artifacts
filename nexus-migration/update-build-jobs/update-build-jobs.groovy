#!/usr/bin/env groovy
// -------------------------------------------------------------------------------------
// Copyright (c) 2026 WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
// --------------------------------------------------------------------------------------

import jenkins.model.*
import hudson.model.*
import hudson.util.DescribableList
import org.jvnet.hudson.plugins.m2release.*

// Credentials Binding plugin
import org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper
import org.jenkinsci.plugins.credentialsbinding.MultiBinding
import org.jenkinsci.plugins.credentialsbinding.impl.UsernamePasswordMultiBinding

// Config File Provider plugin
import org.jenkinsci.plugins.configfiles.buildwrapper.ConfigFileBuildWrapper
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile

// EnvInject plugin
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper
import org.jenkinsci.plugins.envinject.EnvInjectJobPropertyInfo

// Managed Scripts plugin (post-build steps)
import org.jenkinsci.plugins.managedscripts.ScriptBuildStep

// ============================================================
// === CONFIGURATION — edit these before running           ===
// ============================================================

// Mode: "single" to update one job, "folder" to update all
//       Maven-Release jobs inside a Jenkins folder (by full path).
def MODE = "folder"   // "single" | "folder"

// Full job path for single-job mode  (e.g. "my-folder/my-job")
def SINGLE_JOB_NAME = "test-jobs/maven-tester-support"

// Jenkins folder path for folder mode  (e.g. "my-folder")
// Every direct or nested child that has an M2ReleaseBuildWrapper will be updated.
def FOLDER_PATH = "test-jobs/test"

// Folders whose jobs must never be touched (prefix match on full job name)
def EXCLUDED_FOLDERS = ["iam-cloud"]

// Set to false to actually apply changes; true = report only (safe default)
def DRY_RUN = true

// ============================================================
// === HELPERS
// ============================================================

/** Returns true when a job's full name starts with any excluded prefix. */
def isExcluded(Job job, List<String> excluded) {
    return excluded.any { job.getFullName().startsWith(it + "/") || job.getFullName() == it }
}

/**
 * Apply all 6 configuration steps to a single FreeStyle/Maven job.
 * Returns true if any change was made (or would be made in dry-run).
 */
def updateJob(job, boolean dryRun) {
    def wrappers   = job.getBuildWrappersList()
    def m2Release  = wrappers.find { it instanceof M2ReleaseBuildWrapper }

    if (!m2Release) {
        println "  ⏭️  No M2ReleaseBuildWrapper found — skipping."
        return false
    }

    def changed = false

    // ------------------------------------------------------------------
    // 1. Credentials Binding — UsernamePassword: CENTRAL_TOKEN_USERNAME /
    //    CENTRAL_TOKEN_PASSWORD  (credential id: maven-central-token)
    // ------------------------------------------------------------------
    def credWrapper = wrappers.find { it instanceof SecretBuildWrapper }
    def targetCredId   = "maven-central-token"
    def targetUserVar  = "CENTRAL_TOKEN_USERNAME"
    def targetPassVar  = "CENTRAL_TOKEN_PASSWORD"

    if (credWrapper) {
        def bindings = credWrapper.getBindings()
        def existingBinding = bindings.find {
            it instanceof UsernamePasswordMultiBinding &&
            it.credentialsId == targetCredId &&
            it.usernameVariable == targetUserVar &&
            it.passwordVariable == targetPassVar
        }
        if (!existingBinding) {
            println "  ➕ [1] Adding UsernamePassword binding (${targetUserVar}/${targetPassVar}) to existing SecretBuildWrapper."
            if (!dryRun) {
                bindings.add(new UsernamePasswordMultiBinding(targetUserVar, targetPassVar, targetCredId))
            }
            changed = true
        } else {
            println "  ✅ [1] UsernamePassword binding already present."
        }
    } else {
        println "  ➕ [1] Creating new SecretBuildWrapper with UsernamePassword binding (${targetUserVar}/${targetPassVar})."
        if (!dryRun) {
            def newBinding = new UsernamePasswordMultiBinding(targetUserVar, targetPassVar, targetCredId)
            def newWrapper = new SecretBuildWrapper([newBinding])
            wrappers.add(newWrapper)
        }
        changed = true
    }

    // ------------------------------------------------------------------
    // 2. Config File Provider — file id: maven-settings-xml →
    //    variable: CENTRAL_SETTINGS_XML
    // ------------------------------------------------------------------
    def cfgFileId  = "<file_id>"
    def cfgFileVar = "CENTRAL_SETTINGS_XML"

    def cfgWrapper = wrappers.find { it instanceof ConfigFileBuildWrapper }
    if (cfgWrapper) {
        def managedFiles = cfgWrapper.getManagedFiles()
        def existingFile = managedFiles.find {
            it.fileId == cfgFileId && it.variable == cfgFileVar
        }
        if (!existingFile) {
            println "  ➕ [2] Adding managed config file '${cfgFileId}' → \$${cfgFileVar} to existing ConfigFileBuildWrapper."
            if (!dryRun) {
                managedFiles.add(new ManagedFile(cfgFileId, "", cfgFileVar))
            }
            changed = true
        } else {
            println "  ✅ [2] Config file '${cfgFileId}' already bound to \$${cfgFileVar}."
        }
    } else {
        println "  ➕ [2] Creating new ConfigFileBuildWrapper with managed file '${cfgFileId}' → \$${cfgFileVar}."
        if (!dryRun) {
            def newWrapper = new ConfigFileBuildWrapper([new ManagedFile(cfgFileId, "", cfgFileVar)])
            wrappers.add(newWrapper)
        }
        changed = true
    }

    // ------------------------------------------------------------------
    // 3. EnvInject — add CENTRAL_PUBLISHING_TYPE=USER_MANAGED to the
    //    Properties Content of the EnvInjectBuildWrapper.
    // ------------------------------------------------------------------
    def envKey   = "CENTRAL_PUBLISHING_TYPE"
    def envValue = "USER_MANAGED"
    def envLine  = "${envKey}=${envValue}"

    def envWrapper = wrappers.find { it instanceof EnvInjectBuildWrapper }
    if (envWrapper) {
        def info    = envWrapper.getInfo()
        def content = info?.getPropertiesContent() ?: ""
        // Check if the key is already set (to any value)
        def alreadySet = content.split(/[\r\n]+/).any { it.trim().startsWith("${envKey}=") }
        if (!alreadySet) {
            println "  ➕ [3] Adding '${envLine}' to EnvInject Properties Content."
            if (!dryRun) {
                def newContent = content ? "${content.stripTrailing()}\n${envLine}" : envLine
                info.setPropertiesContent(newContent)
            }
            changed = true
        } else {
            println "  ✅ [3] '${envKey}' already present in EnvInject Properties Content."
        }
    } else {
        println "  ➕ [3] Creating new EnvInjectBuildWrapper with '${envLine}'."
        if (!dryRun) {
            def info = new EnvInjectJobPropertyInfo("", envLine, "", "", "", false)
            def newWrapper = new EnvInjectBuildWrapper(info)
            wrappers.add(newWrapper)
        }
        changed = true
    }

    // ------------------------------------------------------------------
    // 4. M2ReleaseBuildWrapper — ensure -Dmaven.deploy.skip=true is
    //    present inside the -Darguments="..." block of Goals and Options.
    //
    //    getReleaseGoals() exists but there is no public setReleaseGoals()
    //    in this plugin version, so we write the field via reflection.
    // ------------------------------------------------------------------
    def skipFlag = "-Dmaven.deploy.skip=true"

    // Helper: find a declared field by name walking up the class hierarchy
    def findField = { Class cls, String fieldName ->
        while (cls != null) {
            try { return cls.getDeclaredField(fieldName) } catch (NoSuchFieldException e) {}
            cls = cls.getSuperclass()
        }
        return null
    }

    def releaseGoalsField = findField(m2Release.getClass(), "releaseGoals")
    if (releaseGoalsField == null) {
        // Fallback: print all fields so we can identify the correct name
        println "  ⚠️  [4] Cannot find field 'releaseGoals' via reflection — skipping goals update."
        def allFields = []
        def cls = m2Release.getClass()
        while (cls != null) { allFields.addAll(cls.getDeclaredFields()*.name); cls = cls.getSuperclass() }
        println "       Declared fields: ${allFields.sort()}"
    } else {
        releaseGoalsField.setAccessible(true)
        def goals = (releaseGoalsField.get(m2Release) ?: "") as String

        def matcher = (goals =~ /-Darguments\s*=\s*(["'])(.*?)\1|-Darguments\s*=\s*(\S+)/)
        if (matcher.find()) {
            def quote     = matcher.group(1) ?: ""
            def innerArgs = matcher.group(2) ?: matcher.group(3) ?: ""
            if (innerArgs.contains(skipFlag)) {
                println "  ✅ [4] '${skipFlag}' already present in -Darguments."
            } else {
                def newInner = innerArgs ? "${innerArgs} ${skipFlag}" : skipFlag
                def newDargs = quote ? "-Darguments=${quote}${newInner}${quote}" : "-Darguments=${newInner}"
                def newGoals = goals.replaceFirst(/-Darguments\s*=\s*(["'])(.*?)\1|-Darguments\s*=\s*(\S+)/, newDargs)
                println "  🛠️  [4] Updating -Darguments in Goals and Options:"
                println "       Old: ${goals}"
                println "       New: ${newGoals}"
                if (!dryRun) {
                    releaseGoalsField.set(m2Release, newGoals)
                }
                changed = true
            }
        } else {
            def newGoals = "${goals} -Darguments=\"${skipFlag}\"".trim()
            println "  ➕ [4] No -Darguments found. Appending to Goals and Options:"
            println "       Old: ${goals}"
            println "       New: ${newGoals}"
            if (!dryRun) {
                releaseGoalsField.set(m2Release, newGoals)
            }
            changed = true
        }
    }

    // ------------------------------------------------------------------
    // 4b. M2ReleaseBuildWrapper — ensure "Use Nexus3 Upload" is enabled.
    //
    //    No public getter/setter exists in this plugin version; write the
    //    boolean field directly via reflection.
    //    Common field names: useNexus3, nexus3Upload, useNexus3Upload
    // ------------------------------------------------------------------
    def nexus3FieldNames = ["useNexus3", "nexus3Upload", "useNexus3Upload"]
    def nexus3Field = nexus3FieldNames.findResult { findField(m2Release.getClass(), it) }

    if (nexus3Field == null) {
        println "  ⚠️  [4b] Cannot find Nexus3 boolean field via reflection — skipping."
        def allFields = []
        def cls = m2Release.getClass()
        while (cls != null) { allFields.addAll(cls.getDeclaredFields()*.name); cls = cls.getSuperclass() }
        println "        Declared fields: ${allFields.sort()}"
    } else {
        nexus3Field.setAccessible(true)
        def currentValue = nexus3Field.get(m2Release)
        if (!currentValue) {
            println "  ➕ [4b] Enabling 'Use Nexus3 Upload' (field: ${nexus3Field.name}) on M2ReleaseBuildWrapper."
            if (!dryRun) {
                nexus3Field.set(m2Release, true)
            }
            changed = true
        } else {
            println "  ✅ [4b] 'Use Nexus3 Upload' is already enabled (field: ${nexus3Field.name})."
        }
    }

    // ------------------------------------------------------------------
    // 5 & 6. Post-build Steps — ensure both managed scripts are present
    //        in the correct order: nexus-sh first, then maven-central-sh.
    //
    //    ScriptBuildStep constructor varies by plugin version; use
    //    reflection to pick the right one at runtime.
    // ------------------------------------------------------------------
    def requiredScripts = ["nexus-sh", "maven-central-sh"]
    def postSteps       = job.getPostbuilders()

    // Print available constructors once for diagnostics (only when a script is missing)
    def printedConstructors = false

    requiredScripts.eachWithIndex { scriptId, idx ->
        def stepNum = idx + 5   // steps 5 and 6
        def existing = postSteps.find {
            it instanceof ScriptBuildStep && it.buildStepId == scriptId
        }
        if (!existing) {
            println "  ➕ [${stepNum}] Adding post-build ScriptBuildStep id='${scriptId}'."
            if (!dryRun) {
                // Discover the constructor that takes a single String (the script id)
                def ctors = ScriptBuildStep.class.getConstructors()
                def ctor  = ctors.find { it.parameterTypes.length == 1 && it.parameterTypes[0] == String }
                          ?: ctors.find { it.parameterTypes.length == 2 && it.parameterTypes[0] == String }
                          ?: ctors.find { it.parameterTypes.length > 0  && it.parameterTypes[0] == String }

                if (ctor == null) {
                    if (!printedConstructors) {
                        println "  ⚠️  [${stepNum}] No suitable ScriptBuildStep constructor found."
                        println "       Available constructors:"
                        ctors.each { println "         ${it}" }
                        printedConstructors = true
                    }
                } else {
                    def args = ctor.parameterTypes.collect { type ->
                        if (type == String)            return scriptId
                        if (type == boolean || type == Boolean) return false
                        if (type == List || type.isAssignableFrom(ArrayList)) return []
                        return null
                    }
                    def step = ctor.newInstance(args as Object[])
                    postSteps.add(step)
                }
            }
            changed = true
        } else {
            println "  ✅ [${stepNum}] Post-build script '${scriptId}' already present."
        }
    }

    return changed
}

// ============================================================
// === MAIN
// ============================================================

def jenkins = Jenkins.get()

def jobsToProcess = []

if (MODE == "single") {
    def job = jenkins.getItemByFullName(SINGLE_JOB_NAME)
    if (job == null) {
        // Attempt a case-insensitive search and print candidates to help diagnose
        println "❌ Job not found: '${SINGLE_JOB_NAME}'"
        println "   Tip: job names are case-sensitive. Searching for close matches..."
        def lowerTarget = SINGLE_JOB_NAME.toLowerCase()
        jenkins.getAllItems(hudson.model.AbstractProject).each { candidate ->
            if (candidate.getFullName().toLowerCase().contains(lowerTarget.tokenize('/').last())) {
                println "   Candidate: '${candidate.getFullName()}'"
            }
        }
        println "   Run MODE=\"list\" to print all available job paths."
        return
    }
    if (!(job instanceof hudson.model.AbstractProject)) {
        println "❌ '${SINGLE_JOB_NAME}' is not a build job (type: ${job.getClass().simpleName}). It may be a Pipeline or folder."
        return
    }
    jobsToProcess << job

} else if (MODE == "folder") {
    def folder = jenkins.getItemByFullName(FOLDER_PATH)
    if (folder == null) {
        println "❌ Folder not found: '${FOLDER_PATH}'"
        println "   Run MODE=\"list\" to print all available job paths."
        return
    }
    // Collect all AbstractProject descendants inside the folder,
    // excluding MavenModule sub-modules (they have no getBuildWrappersList)
    jenkins.getAllItems(hudson.model.AbstractProject).each { job ->
        if (job instanceof hudson.maven.MavenModule) return
        if (job.getFullName().startsWith(FOLDER_PATH + "/")) {
            jobsToProcess << job
        }
    }
    if (jobsToProcess.isEmpty()) {
        println "⚠️  No build jobs found under folder '${FOLDER_PATH}'."
        println "   Run MODE=\"list\" to see all available job paths."
        return
    }

} else if (MODE == "list") {
    // ── Diagnostic mode ──────────────────────────────────────────────
    // Prints all job full names so you can copy the exact path needed
    // for SINGLE_JOB_NAME or FOLDER_PATH.
    println "All jobs visible to this script:"
    jenkins.getAllItems(hudson.model.AbstractProject).each { job ->
        // Skip MavenModule sub-modules — they are not top-level jobs and
        // do not have getBuildWrappersList()
        if (job instanceof hudson.maven.MavenModule) return
        def marker = ""
        try {
            marker = job.getBuildWrappersList().any { it instanceof M2ReleaseBuildWrapper } ? " [M2Release]" : ""
        } catch (e) { marker = " [unsupported]" }
        println "  ${job.getFullName()}  (${job.getClass().simpleName})${marker}"
    }
    return

} else {
    println "❌ Unknown MODE '${MODE}'. Use 'single', 'folder', or 'list'."
    return
}

println "=" * 70
println "Mode      : ${MODE}"
println "Dry-run   : ${DRY_RUN}"
println "Jobs found: ${jobsToProcess.size()}"
println "=" * 70

int updatedCount = 0
int skippedCount = 0

int errorCount = 0

jobsToProcess.each { job ->
    if (isExcluded(job, EXCLUDED_FOLDERS)) {
        println "\n🚫 Skipping excluded job: ${job.fullName}"
        skippedCount++
        return
    }

    println "\n🔍 Processing: ${job.fullName}"

    try {
        def changed = updateJob(job, DRY_RUN)

        if (changed) {
            if (!DRY_RUN) {
                job.save()
                println "  💾 Changes saved."
            } else {
                println "  📝 [DRY-RUN] Changes would be applied."
            }
            updatedCount++
        } else {
            println "  🔸 No changes required."
            skippedCount++
        }
    } catch (Exception e) {
        println "  ❌ ERROR processing '${job.fullName}': ${e.getClass().simpleName}: ${e.getMessage()}"
        e.printStackTrace()
        errorCount++
    }
}

println "\n" + "=" * 70
println "Summary: ${updatedCount} job(s) updated, ${skippedCount} skipped, ${errorCount} error(s)."
if (errorCount > 0) {
    println "⚠️  ${errorCount} job(s) failed — see ❌ lines above for details."
}
if (DRY_RUN) {
    println "⚠️  DRY_RUN=true — no changes were written. Set DRY_RUN=false to apply."
}
println "=" * 70
