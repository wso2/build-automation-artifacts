# wso2-jenkins-slave

WSO2 Jenkins docker container based builds are run on a Docker image built with this Dockerfile.
Here, with the help of Jenkins Docker plugin, dynamically slave containers are spawned for each build
and destroyed after the build.

* Download and place `jdk-8u131-linux-x64.tar.gz` and `jdk-7u51-linux-x64.tar.gz` in the same folder as the `Dockerfile`. These can be downloaded via http://www.oracle.com/technetwork/java/javase/archive-139210.html.

* Build the docker image via the command 

  `docker build -t wso2-jenkins-slave:latest -t wso2-jenkins-slave:1.11 .`

If you are connecting to the docker instance remotely, then set the docker host info and the `--tlsverfify` flag.

  `docker -H=127.0.0.1:2376 --tlsverify build -t wso2-jenkins-slave:latest -t wso2-jenkins-slave:1.0 .`

Now, you have the docker image added to your local docker instance.

* If you have a docker registry, then you can push the newly built docker image there such that your other builds can use it. This is optional. Otherwise, you can go to each VM, and build the docker image.
  * Login to the docker registry
  
  `docker login mydockerhub.example.com`
  
  * Name your docker image appropriately
  
  `docker tag wso2-jenkins-slave:1.0 mydockerhub.example.com/kasung-wso2-jenkins-slave`

  * Now, push the image to your docker registry

  `docker push mydockerhub.example.com/kasung-wso2-jenkins-slave`
  
  * Once that is done, go to VMs that you are going to use for docker-based Jenkins builds. Run the following command to pull the newly created docker image.
  
  `docker pull mydockerhub.example.com/kasung-wso2-jenkins-slave`
