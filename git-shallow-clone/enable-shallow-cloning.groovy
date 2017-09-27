import com.cloudbees.hudson.plugins.folder.*
import hudson.plugins.git.GitSCM
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.extensions.impl.CloneOption
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

NO_TAGS = true
SHALLOW_CLONE = true
REFERENCE = null
TIMEOUT = 10
DEPTH = 100

/*Loop through all items inside a folder*/
void processFolder(Item folder) {
  folder.getItems().each{
    if (it instanceof Folder) {
        processFolder(it)
    } else {
        configureCloningOptions(it)
    }
  }
}

/*Configure Shallow Cloning options for a job*/
void configureCloningOptions(Item job) {
  try {
    AbstractBuild build = job.getLastBuild();
    if (build != null) {
      GitSCM gitSCM = (GitSCM) build.getProject().getScm()
      List<GitSCMExtension> gitExtensions = gitSCM.getExtensions()
      if (gitExtensions != null) {
        CloneOption cloneOptions = gitExtensions.get(CloneOption.class)
        if (cloneOptions != null && cloneOptions.isShallow()) {
          println("Advanced Cloning options has already configured for the job: $job.name")
        } else {
          CloneOption cloneOption = new CloneOption(SHALLOW_CLONE, NO_TAGS, REFERENCE, TIMEOUT)
          cloneOption.setDepth(DEPTH)
          gitExtensions.add(cloneOption)
          job.save();
          println("Configured Advanced Cloning options for the job: $job.name")
        }
      } else {
        println("Unable to configure Advanced cloning options for the job:  $job.name")
      }
    }
  } catch (GroovyCastException e) {
    println("Unable to configure Advanced cloning options for the job:  $job.name, cause : " + e.getMessage())
  } 
}

/*Loop through all available jenkins jobs*/
Jenkins.instance.getItems().each{
  if (it instanceof Folder) {
      processFolder(it)
  } else {
      configureCloningOptions(it)
  }
}
