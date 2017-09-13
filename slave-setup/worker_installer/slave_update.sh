#!/bin/sh
# script that should be executed on the slave after all files are copied

# Installing System Tools
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Updating System Tools                          *"
echo "*****************************************************************************"
echo ""
echo ""

# Updating to latest packages
sudo apt-get update

# Installing xz-utils
echo ""
echo ""
echo "Installing xz-utills........................................,"
echo ""
echo ""
sudo apt-get -y install xz-utils

# Installing python-pip
echo ""
echo ""
echo "Installing pip..............................................,"
echo ""
echo ""
sudo apt-get -y install python-pip
echo ""
echo ""
sudo pip --version
echo ""
echo ""

# Installing build-essential
echo ""
echo ""
echo "Installing build-essential..................................,"
echo ""
echo ""
sudo apt-get -y install build-essential python-dev
echo ""
echo ""

# Installing mkdocs
echo ""
echo ""
echo "Installing mkdocs..............................................,"
echo ""
echo ""
sudo pip install mkdocs && mkdocs --version
echo ""
echo ""

# Installing mkdocs-material
echo ""
echo ""
echo "Installing mkdocs-material....................................,"
echo ""
echo ""
sudo pip install mkdocs-material
echo ""
echo ""

#Creating Directories
cd /build/jenkins-home
mkdir -p software/nodejs
cd

# unzip installation file
#unzip nodejs files 
echo ""
echo "*****************************************************************************"
echo "*                            Extracting NodeJS files                          *"
echo "*****************************************************************************"
echo ""

tar -xf /build/jenkins-home/slaveSetupFile/node-v6.10.0-linux-x64.tar.xz -C /build/jenkins-home/software/nodejs

echo "Extracted NodeJS files"
echo ""

#unzip nodejs files 
echo ""
echo "*****************************************************************************"
echo "*                              Extracting JDK                               *"
echo "*****************************************************************************"
echo ""

tar -zxvf /build/jenkins-home/slaveSetupFile/jdk-8u144-linux-x64.tar.gz -C /build/jenkins-home/software/java

echo "Extracted jdk8.144"
echo ""

#unzip maven
echo ""
echo "*****************************************************************************"
echo "*                           Extracting Maven files                          *"
echo "*****************************************************************************"
echo ""

#tar -zxvf /build/jenkins-home/slaveSetupFile/apache-maven-3.2.2-bin.tar.gz -C /build/jenkins-home/software/maven

unzip -o /build/jenkins-home/slaveSetupFile/apache-maven-3.3.9-bin.zip -d /build/jenkins-home/software/maven

#copying gpg-keys
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Extracting GPG files                           *"
echo "*****************************************************************************"
echo ""
echo ""
#unzip -o /build/jenkins-home/slaveSetupFile/jce_policy-8.zip -d /build/jenkins-home/software/jce

#rename policy.jar

#copying gpg-keys
mv /build/jenkins-home/slaveSetupFile/gpg-keys /build/

echo "Copied gpg-keys to /build"

#mv /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy-original.jar

#copy jce files

#cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/
#cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/

#reboot node
# sudo reboot

