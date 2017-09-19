import com.cloudbees.hudson.plugins.folder.*
import com.chikli.hudson.plugin.naginator.*

/*Loop through all items inside a folder*/  
void processFolder(Item folder) {
  folder.getItems().each{
    if (it instanceof Folder) {
        processFolder(it)
    } else {
        configureNaginator(it)
    }
  }
}

/*Configure Naginator for a job*/
void configureNaginator(Item job){
  /*regex is the string that holds the regular expression for looking for matches in the build output log. Modify according to the requirements.*/
  String regex = ".*Return code is: 50[23], ReasonPhrase: (Bad Gateway|Service Temporarily Unavailable).*|.*Error (cloning|fetching) remote repo.*|.*Connection (reset|time out to Testlink|to repo.maven.apache.org refused for project).*|.*hudson.remoting.ChannelClosedException: channel is already closed.*|.*FATAL: command execution failed.*|.*Failed to deploy metadata:(.*Return code is: ([0-9]*))?(.*ReasonPhrase: ([^\\.]*))?";
  int progressiveIncrement = 300
  int progressiveMaxIncrement = 1800
  int maxSuccessiveFailedBuilds = 3
  boolean rerunForUnstable = false
  boolean rerunMatrixPart = false
  boolean rerunForRegex = true
  try {
    /*Get the exisitng Naginator configuration*/
    naginatorPublisherConf = job.getPublishersList().get(NaginatorPublisher.class)
    if (naginatorPublisherConf == null) {
      /*Create Naginator instance with necessary configuration*/
      ProgressiveDelay progressiveDelay = new ProgressiveDelay(progressiveIncrement, progressiveMaxIncrement);
      NaginatorPublisher naginatorPublisher = new NaginatorPublisher(regex, rerunForUnstable, rerunMatrixPart, rerunForRegex, maxSuccessiveFailedBuilds, progressiveDelay);
      /*Add the Naginator configuration to the job*/
      naginatorPublisherConf=job.getPublishersList().add(naginatorPublisher)
      job.save()
      println("Configured Naginator plugin for the job : $job.name")
    } else {
      println("Naginator plugin has already configured for the job : $job.name")
    }
  } catch (MissingMethodException e) {
      /*If the execution reaches here, that means the job is a Pipeline job. We can't configure Naginator for pipeline jobs.*/ 
      println("Skipping Naginator plugin configuration for the Pipeline job : $job.name")
  }
}

/*Loop through all available jenkins jobs*/
Jenkins.instance.getItems().each{
  if (it instanceof Folder) {
      processFolder(it)
  } else {
      configureNaginator(it)
  }
}