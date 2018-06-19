# wso2-jenkins-slave

WSO2 Jenkins docker container based builds are run on a Docker image built with this Dockerfile.
Here, with the help of Jenkins Docker plugin, dynamically slave containers are spawned for each build
and destroyed after the build.

* This Docker image will be created from extending production WUM update testing environment's docker image. Thus to build this image, you need to have that base docker image downloaded to local docker engine first.

* Download and place `jdk-8u151-linux-x64.tar.gz`, `jdk-8u131-linux-x64.tar.gz`, `jdk-7u79-linux-x64.tar.gz` and `jdk-7u65-linux-x64.tar.gz` in the same folder as the `Dockerfile`. These can be downloaded via http://www.oracle.com/technetwork/java/javase/archive-139210.html.

* Download and place `apache-ant-1.9.6-bin.tar.gz` in the same folder as the `Dockerfile`. These can be downloaded via https://archive.apache.org/dist/ant/binaries/.

* Download the docker base image via the command 

  `sudo docker pull 8878565184191.dkr.ecr.ap-southeast-1.amazonaws.com/wso2-jenkins-ecs-slave:latest`

* Build the docker image via the command 

  `sudo docker build -t support:latest .`
