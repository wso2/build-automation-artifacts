package org.wso2.build;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;
import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.triggers.TriggerDescriptor;
import hudson.triggers.SCMTrigger;

println("Updating  maven release build option with wso2-release profile ... ");

Jenkins.instance.getItems().each {
    if (it instanceof Folder) {
        processFolder(it);
    } else {
        configureReleaseProfile(it);
    }
}

void processFolder(Item folder) {
    folder.getItems().each {
        if (it instanceof Folder) {
            processFolder(it);
        } else {
            configureReleaseProfile(it);
        }
    }
}

/**
 * Add wso2-release profile for maven release build configuration
 * Enable GitHub hook trigger for GITScm polling
 * Disable Poll SCM
 * @param item Build job
 */
void configureReleaseProfile(Item item) {
    try {
        //String projName="charon_3.0.x";//Uncomment this if you want to update a single project
        //if(item.name == projName) { //Uncomment this if you want to update a single project
        if((item.name ==~ /(.*)carbon-(.*)/) || (item.name ==~ /(.*)identity-(.*)/)) {//Comment this if you are uncommenting line above.
            rw = item.getBuildWrappers().get(M2ReleaseBuildWrapper.class);
            TriggerDescriptor GIT_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptor(GitHubPushTrigger.class);
            gittrigger = item.getTriggers().get(GIT_TRIGGER_DESCRIPTOR);
            TriggerDescriptor SCM_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptorOrDie(SCMTrigger.class)
            scmtrigger = item.getTriggers().get(SCM_TRIGGER_DESCRIPTOR)
            if (rw != null) {
                releasegoals = rw.getReleaseGoals();
                dryRungoals = rw.getDryRunGoals();
                if (releasegoals != null && !releasegoals.contains("wso2-release")) {
                    def DEFAULT_RELEASE_GOALS = "-Dresume=false release:prepare release:perform -P wso2-release";
                    rw.releaseGoals = DEFAULT_RELEASE_GOALS;
                    item.save();
                    println("Updated the release goals with wso2-release profile for: $item.name");
                }
                if (dryRungoals != null && !dryRungoals.contains("wso2-release")) {
                    def DEFAULT_DRYRUN_GOALS = "-Dresume=false -DdryRun=true release:prepare -P wso2-release";
                    rw.dryRunGoals = DEFAULT_DRYRUN_GOALS;
                    item.save();
                    println("Updated the dry run goals with wso2-release profile for: $item.name");
                }
                if (gittrigger == null) {
                    item.addTrigger(new GitHubPushTrigger());
                    item.save();
                    println("GitHub trigger applied for build job: $item.name");
                }
                if (scmtrigger != null) {
                    item.removeTrigger(SCM_TRIGGER_DESCRIPTOR);
                    item.save();
                    println("SCM triggers removed for build job: $item.name");
                }
            }
        }
    } catch (MissingMethodException e) {
        println("Skipping wso2 release profile configuration for the Pipeline job : $item.name")
    }
}

println("Updating of wso2-release profile is completed ");