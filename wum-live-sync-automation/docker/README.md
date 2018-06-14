# wso2-jenkins-slave

WSO2 Jenkins docker container based builds are run on a Docker image built with this Dockerfile.
Here, with the help of Jenkins Docker plugin, dynamically slave containers are spawned for each build
and destroyed after the build.

* Download and place `jdk-8u151-linux-x64.tar.gz`, `jdk-8u144-linux-x64.tar.gz`, `jdk-8u131-linux-x64.tar.gz`, `jdk-8u45-linux-x64.tar.gz`, `jdk-7u79-linux-x64.tar.gz`, `jdk-7u65-linux-x64.tar.gz` and `jdk-7u51-linux-x64.tar.gz` in the same folder as the `Dockerfile`. These can be downloaded via http://www.oracle.com/technetwork/java/javase/archive-139210.html.

* Download and place `apache-ant-1.9.6-bin.tar.gz` in the same folder as the `Dockerfile`. These can be downloaded via https://archive.apache.org/dist/ant/binaries/.

* Download and place `go1.10.linux-amd64.tar.gz` in the same folder as the `Dockerfile`. ThIS can be downloaded via https://dl.google.com/go/go1.10.linux-amd64.tar.gz.

* Build the docker image via the command 

  `sudo docker build -t support:latest .`

If you are connecting to the docker instance remotely, then set the docker host info and the `--tlsverfify` flag.

  `docker -H=127.0.0.1:2376 --tlsverify build -t wso2-jenkins-slave:latest -t wso2-jenkins-slave:1.0 .`

Now, you have the docker image added to your local docker instance.

* If you have a docker registry, then you can push the newly built docker image there such that your other builds can use it. This is optional. Otherwise, you can go to each VM, and build the docker image.
  * Login to the docker registry
  
  `docker login mydockerhub.example.com`
  
  * Name your docker image appropriately
  
  `sudo docker tag <image ID> mydockerhub.example.com/support`

  * Now, push the image to your docker registry

  `sudo docker push mydockerhub.example.com/support:latest`
  
  * Once that is done, go to VMs that you are going to use for docker-based Jenkins builds. Run the following command to pull the newly created docker image.
  
  `sudo docker pull mydockerhub.example.com/support:latest`
