import com.cloudbees.hudson.plugins.folder.*
import hudson.plugins.git.GitSCM
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.extensions.impl.CloneOption
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/*Loop through all items inside a folder*/  
void processFolder(Item folder) {
  folder.getItems().each{
    if (it instanceof Folder) {
        processFolder(it)
    } else {
        disableCloningOptions(it)
    }
  }
}

/*Remove Shallow Cloning options for a job*/
void disableCloningOptions(Item job) {
  try {
    AbstractBuild build = job.getLastBuild()
    if (build != null) {
      GitSCM gitSCM = (GitSCM) build.getProject().getScm()
      List<GitSCMExtension> gitExtensions = gitSCM.getExtensions()
      if (gitExtensions != null) {
        CloneOption cloneOption = gitExtensions.get(CloneOption.class)
        if (cloneOption != null && cloneOption.isShallow()) {
          gitExtensions.remove(cloneOption)
          job.save();
          println("Removed Advanced Cloning options for the job: $job.name")
        }  
      } 
    }
  } catch (GroovyCastException e) {
    println("Unable to configure Advanced cloning options for the job:  $job.name")
  }
}

/*Loop through all available jenkins jobs*/
Jenkins.instance.getItems().each{
  if (it instanceof Folder) {
      processFolder(it)
  } else {
      disableCloningOptions(it)
  }
}