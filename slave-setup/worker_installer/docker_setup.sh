#!/bin/sh
# script that should be executed on the slave after all files are copied

#installing system tools
echo ""
echo ""
echo "*****************************************************************************"
echo "*                    Installing Older Docker version                        *"
echo "*****************************************************************************"
echo ""
echo ""

#sudo echo 'deb [arch=amd64] https://apt.dockerproject.org/repo ubuntu-trusty main' \ > /etc/apt/sources.list.d/docker.list

sudo add-apt-repository "deb [arch=amd64] https://apt.dockerproject.org/repo ubuntu-trusty main"


# updating to latest packages
sudo apt-key update
sudo apt-get update

sudo apt-get -y install linux-image-extra-$(uname -r)

sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys F76221572C52609D

# installing zip/unzip
echo ""
echo ""
echo "Installing zip/unzip.........................................."
echo ""
echo ""
sudo apt-get -y install zip unzip

# installing docker
echo ""
echo ""
echo "Installing docker 1.11.0.....................................,"
echo ""
echo ""
sudo apt-get --force-yes -y install docker-engine=1.11.0-0~trusty


# adding current user to  docker

sudo usermod -aG docker ubuntu

# testing docker
#echo ""
#echo ""
#echo "Testing docker.....................................,"
#echo ""
#echo ""
#docker run hello-world

#creating directories
cd /build/jenkins-home
mkdir -p software/docker
cd

#copying docker image 
echo ""
echo "*****************************************************************************"
echo "*                            Copying Docker Image                          *"
echo "*****************************************************************************"
echo ""

cp /build/jenkins-home/slaveSetupFile/docker-image/wso2-jenkins-slave-*.tar /build/jenkins-home/software/docker/wso2-jenkins-slave.tar	

#unzip docker certs
echo ""
echo "*****************************************************************************"
echo "*                  Extracting + Configuring Docker Certs                    *"
echo "*****************************************************************************"
echo ""

unzip -o /build/jenkins-home/slaveSetupFile/docker_certs.zip -d /build/jenkins-home



#amend /etc/default/docker

sudo echo "DOCKER_OPTS=\"--tlsverify --tlscacert=/build/jenkins-home/docker-certs/ca.pem --tlscert=/build/jenkins-home/docker-certs/server-cert.pem --tlskey=/build/jenkins-home/docker-certs/server-key.pem -H tcp://0.0.0.0:2376"\" >> /etc/default/docker \

# testing docker
echo ""
echo ""
echo "Testing docker.....................................,"
echo ""
echo ""
sudo docker run hello-world


#restarting docker

echo "Restarting docker, please wait......................"

sudo /etc/init.d/docker restart

sudo service docker status

sudo docker version

#load the image
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Loading Docker Image                           *"
echo "*****************************************************************************"
echo ""
echo ""

cd /build/jenkins-home/software/docker/

sudo docker load --input wso2-jenkins-slave.tar

echo "Completed loading docker image!!! Thankyou for waiting!"

#reboot node
# sudo reboot

