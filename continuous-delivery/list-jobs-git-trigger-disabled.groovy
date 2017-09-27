package org.wso2.build;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;
import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.triggers.TriggerDescriptor;

println("List of build jobs with maven release build enabled and github trigger disabled: ");

count = 0;

Jenkins.instance.getItems().each {
    if (it instanceof Folder) {
        processFolder(it);
    } else {
        listGitTriggerDisabledJobs(it);
    }
}

void processFolder(Item folder) {
    folder.getItems().each {
        if (it instanceof Folder) {
            processFolder(it);
        } else {
            listGitTriggerDisabledJobs(it);
        }
    }
}

/**
 * Prints a list of build jobs, where 'Maven Release Build' option is enabled,
 * but "Build when a change is pushed to GitHub" option is disabled.
 * @param item Build job
 */
void listGitTriggerDisabledJobs(Item item) {
    try {
        rw = item.getBuildWrappers().get(M2ReleaseBuildWrapper.class);
        TriggerDescriptor GIT_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptor(GitHubPushTrigger.class);
        gittrigger = item.getTriggers().get(GIT_TRIGGER_DESCRIPTOR);
        if (rw != null && gittrigger == null) {
            count += 1;
            println(item.name);
        }
    } catch (MissingMethodException e) {
        println("Skipping for the Pipeline job : $item.name")
    }
}

println("Number of build jobs with maven release build enabled and github trigger disabled: $count");