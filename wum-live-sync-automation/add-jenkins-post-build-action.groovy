/*******************************************
*                                          *
* This will install post build action of   *
* Jenkins Nexus deployer plugin            *
*                                          *
*******************************************/
import com.cloudbees.hudson.plugins.folder.*
import hudson.maven.*

/*Loop through all items inside a folder*/  
void processFolder(Item folder) {
  folder.getItems().each{
    if (it instanceof Folder) {
        processFolder(it)
    } else {
        configureRedeployPublisher(it)
    }
  }
}

/*Configure maven deployer for a job*/
void configureRedeployPublisher(Item job){
  
  try {
    /*Get the exisitng RedeployPublisher () configuration*/
    redeployPublisherConf = job.getPublishersList().get(RedeployPublisher.class)
    if (redeployPublisherConf == null) {
      /*Create RedeployPublisher instance with necessary configuration*/
      RedeployPublisher redeployPublisher = new RedeployPublisher("uat-nexus", "https://support-maven.wso2.org/nexus/content/repositories/releases", true, false, null);
      /*Add the RedeployPublisher configuration to the job*/
      redeployPublisherConf=job.getPublishersList().add(redeployPublisher)
      job.save()
      println("Configured RedeployPublisher plugin for the job : $job.name")
    } else {
      println("RedeployPublisher plugin has already configured for the job : $job.name")
    }
  } catch (MissingMethodException e) {
      /*If the execution reaches here, that means the job has already configured for RedeployPublisher. We can't configure RedeployPublisher for this jobs.*/ 
      println("Skipping RedeployPublisher plugin configuration for the job : $job.name")
  }
}

/*Loop through all Wilkes jenkins jobs*/
hudson.model.Hudson.instance.getView('Wilkes').items.each() { 
  println it.fullDisplayName
  def wilkesJob = Jenkins.instance.getItem(it.fullDisplayName)
  if (wilkesJob instanceof Folder) {
      processFolder(wilkesJob)
  } else {
      configureRedeployPublisher(wilkesJob)
  }
}
