package org.wso2.build;


import com.cloudbees.hudson.plugins.folder.*
import org.jvnet.hudson.plugins.m2release.M2ReleaseBuildWrapper;

println("List of build jobs with maven release build option disabled: ")

count = 0;

Jenkins.instance.getItems().each {
    if (it instanceof Folder) {
        processFolder(it);
    } else {
        listMavenReleaseDisabledJobs(it);
    }
}

void processFolder(Item folder) {
    folder.getItems().each {
        if (it instanceof Folder) {
            processFolder(it);
        } else {
            listMavenReleaseDisabledJobs(it);
        }
    }
}

/**
 * Prints a list of build jobs, where 'Maven Release Build' option is disabled.
 * @param item Build job
 */
void listMavenReleaseDisabledJobs(Item item) {
    try {
        rw = item.getBuildWrappers().get(M2ReleaseBuildWrapper.class);
        if (rw == null) {
            count += 1;
            println(item.name);
        }
    } catch (MissingMethodException e) {
        println("Skipping for the Pipeline job : $item.name")
    }
}

println("Number of build jobs with maven release build option disabled: $count");
