#!/bin/sh
# script that should be executed on the slave after all files are copied

# Installing System Tools
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Installing System Tools                        *"
echo "*****************************************************************************"
echo ""
echo ""

# Updating to latest packages
sudo apt-get update

# Installing Git
echo ""
echo ""
echo "Installing git........................................,"
echo ""
echo ""
sudo apt-get -y install git
# Installing Apache ANT
echo ""
echo ""
echo "Installing ant........................................,"
echo ""
echo ""
sudo apt-get -y install ant
# Installing unzip
echo ""
echo ""
echo "Installing zip/unzip..................................,"
echo ""
echo ""
sudo apt-get -y install zip unzip
# Installing curl
echo ""
echo ""
echo "Installing curl.......................................,"
echo ""
echo ""
sudo apt-get -y install curl
# Installing xz-utils
echo ""
echo ""
echo "Installing xz-utills........................................,"
echo ""
echo ""
sudo apt-get -y install xz-utils

#Xvfb configuration
echo ""
echo ""
echo "Installing Firefox.......................................,"
echo ""
echo ""
#Install Firefox and downgrade to version 28
sudo apt-get --yes --force-yes install firefox
apt-cache show firefox | grep Version
echo ""
echo ""
echo "Downgrade Firefox version to 28.0.......................................,"
echo ""
echo ""
sudo apt-get --yes --force-yes install firefox=28.0+build2-0ubuntu2
sudo apt-mark hold firefox
#Install Xvfb X 
echo "Installing xvfb.......................................,"
sudo apt-get -y install xvfb
#Install dbus-x11
echo "Installing dbus-x11.......................................,"
sudo apt-get -y install dbus-x11

#mkdocs configuration
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
mkdir -p software/java
mkdir -p software/maven
mkdir -p software/jce
mkdir -p software/nodejs
cd

# defining JDK, MAVEN, JCE and NODEJS variables. please change here when required to add JDK/MAVEN and etc..
JDK7x=jdk-7u51-linux-x64.tar.gz
JDK8x=jdk-8u45-linux-x64.tar.gz
JDK813x=jdk-8u131-linux-x64.tar.gz
JDK814x=jdk-8u144-linux-x64.tar.gz

APACHE_MAVEN_22x=apache-maven-2.2.1-bin.tar.gz
APACHE_MAVEN_30x=apache-maven-3.0.5-bin.tar.gz
APACHE_MAVEN_31x=apache-maven-3.1.1-bin.zip
APACHE_MAVEN_32x=apache-maven-3.2.2-bin.tar.gz
APACHE_MAVEN_33x=apache-maven-3.3.9-bin.zip

JCE7=UnlimitedJCEPolicyJDK7.zip
JCE8=jce_policy-8.zip

NODEJSv6=node-v6.10.0-linux-x64.tar.xz

# unzip installation file
#unzip java files
echo ""
echo "*****************************************************************************"
echo "*                            Extracting Java files                          *"
echo "*****************************************************************************"
echo ""

wget -P /build/jenkins-home/slaveSetupFile -c --no-cookies --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com" "http://download.oracle.com/otn-pub/java/jdk/7u51-b13/jdk-7u51-linux-x64.tar.gz"
tar -zxvf /build/jenkins-home/slaveSetupFile/$JDK7x -C /build/jenkins-home/software/java
#tar -zxvf /build/jenkins-home/slaveSetupFile/jdk-8u45-linux-x64.tar.gz -C /build/jenkins-home/software/java

wget -P /build/jenkins-home/slaveSetupFile -c --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.tar.gz
tar -zxvf /build/jenkins-home/slaveSetupFile/$JDK813x -C /build/jenkins-home/software/java

wget -P /build/jenkins-home/slaveSetupFile -c --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jdk-8u144-linux-x64.tar.gz"
tar -zxvf /build/jenkins-home/slaveSetupFile/$JDK814x -C /build/jenkins-home/software/java


#unzip maven
echo ""
echo "*****************************************************************************"
echo "*                           Extracting Maven files                          *"
echo "*****************************************************************************"
echo ""
wget -P /build/jenkins-home/slaveSetupFile -c https://archive.apache.org/dist/maven/binaries/apache-maven-2.2.1-bin.tar.gz
tar -zxvf /build/jenkins-home/slaveSetupFile/$APACHE_MAVEN_22x -C /build/jenkins-home/software/maven

wget -P /build/jenkins-home/slaveSetupFile -c https://archive.apache.org/dist/maven/binaries/apache-maven-3.0.5-bin.tar.gz
tar -zxvf /build/jenkins-home/slaveSetupFile/$APACHE_MAVEN_30x -C /build/jenkins-home/software/maven

wget -P /build/jenkins-home/slaveSetupFile -c https://archive.apache.org/dist/maven/binaries/apache-maven-3.1.1-bin.zip 
unzip -o /build/jenkins-home/slaveSetupFile/$APACHE_MAVEN_31x -d /build/jenkins-home/software/maven

wget -P /build/jenkins-home/slaveSetupFile -c https://archive.apache.org/dist/maven/binaries/apache-maven-3.2.2-bin.tar.gz 
tar -zxvf /build/jenkins-home/slaveSetupFile/$APACHE_MAVEN_32x -C /build/jenkins-home/software/maven

wget -P /build/jenkins-home/slaveSetupFile -c https://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip 
unzip -o /build/jenkins-home/slaveSetupFile/$APACHE_MAVEN_33x -d /build/jenkins-home/software/maven

#unzip jce
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Extracting JCE files                           *"
echo "*****************************************************************************"
echo ""
echo ""
unzip -o /build/jenkins-home/slaveSetupFile/$JCE8 -d /build/jenkins-home/software/jce
unzip -o /build/jenkins-home/slaveSetupFile/$JCE7 -d /build/jenkins-home/software/jce

#rename policy.jar
#jdk7
mv /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/local_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/local_policy-original.jar
mv /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy-original.jar
#jdk8
#mv /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/local_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/local_policy-original.jar
#mv /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy-original.jar

#copy jce files
#jdk7
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicy/local_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicy/US_export_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security
#jdk8
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security

#copying gpg-keys
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Extracting GPG files                           *"
echo "*****************************************************************************"
echo ""
echo ""
mv /build/jenkins-home/slaveSetupFile/gpg-keys /build/

# unzip installation file
#unzip nodejs files 
echo ""
echo "*****************************************************************************"
echo "*                            Extracting NodeJS files                          *"
echo "*****************************************************************************"
echo ""
wget -P /build/jenkins-home/slaveSetupFile https://nodejs.org/dist/v6.10.0/node-v6.10.0-linux-x64.tar.xz
tar -xf /build/jenkins-home/slaveSetupFile/$NODEJSv6 -C /build/jenkins-home/software/nodejs

#/etc/sysctl.conf

#backup file
sudo cp /etc/sysctl.conf /etc/sysctl.conf.backup.$(date +%F_%R)

#adding Configurations
str1="fs.file-max = 2097152"
if ! (( $(grep -c "$str1" /etc/sysctl.conf) )) ; 
then
	sudo -- sh -c "echo $str1 >> /etc/sysctl.conf"	
else
	command
        command
fi

#/etc/pam.d/su
 
#backup file
sudo cp /etc/pam.d/su /etc/pam.d/su.backup.$(date +%F_%R)

#adding Configurations
str2="session    required   pam_limits.so"
str22="# session    required   pam_limits.so"
if  (( $(grep -c "$str22" /etc/pam.d/su) )) ; 
then
	sudo -- sh -c "echo $str2 >> /etc/pam.d/su"
	sed '/# session    required   pam_limits.so/d' /etc/pam.d/su
else
	command
        command
fi


#/etc/hosts
# For appfactory integration test 

#backup file
#sudo cp /etc/hosts /etc/hosts.backup.$(date +%F_%R)

#reboot node
# sudo reboot
