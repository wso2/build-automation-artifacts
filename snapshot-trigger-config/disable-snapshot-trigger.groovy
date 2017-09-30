import com.cloudbees.hudson.plugins.folder.Folder;

Jenkins.instance.getItems().each {
    if (it instanceof Folder) {
        processFolder(it);
    } else {
        disableSnapshotTrigger(it);
    }
}

void processFolder(Item folder) {
    folder.getItems().each {
        if (it instanceof Folder) {
            processFolder(it);
        } else {
            disableSnapshotTrigger(it);
        }
    }
}

/**
 * Disables the "Build whenever a SNAPSHOT dependency is built" trigger from all jobs.
 */
void disableSnapshotTrigger(Item item) {
    try {
      if (!item.ignoreUpstremChanges) {
      	item.ignoreUpstremChanges = true;
      	item.save();
        println("Removed MavenSnapshotTrigger from Job : $item.name")
      }
    } catch (MissingMethodException e) {
        println("Skipping configuration for the Pipeline job : $item.name, cause : " + e.getMessage())
    } catch (MissingPropertyException e) {
        println("Skipping configuration for the Pipeline job : $item.name, cause : " + e.getMessage())
    }
}
