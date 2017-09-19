import com.cloudbees.hudson.plugins.folder.*
import com.chikli.hudson.plugin.naginator.*
  
void processFolder(Item folder) {
  folder.getItems().each{
    if (it instanceof Folder) {
        processFolder(it)
    } else {
        disableNaginator(it)
    }
  }
}

void disableNaginator(Item job){
  try {
    	naginatorPublisherConf = job.getPublishersList().get(NaginatorPublisher.class)
  		if (naginatorPublisherConf != null) {
  			naginatorPublisherConf=job.getPublishersList().remove(NaginatorPublisher.class)
    		job.save()
    		println("Disabled Naginator plugin for the job : $job.name")
  		} else {
            println("Naginator plugin has not configured for the job : $job.name")
        }
   } catch (MissingMethodException e) {  		
        println("Skipping the Naginator configuration for pipeline job : $job.name")
   }
}

Jenkins.instance.getItems().each{
  if (it instanceof Folder) {
      processFolder(it)
  } else {
      disableNaginator(it)
  }
}