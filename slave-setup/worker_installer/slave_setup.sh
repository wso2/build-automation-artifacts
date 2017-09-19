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


#Creating Directories
cd /build/jenkins-home
mkdir -p software/java
mkdir -p software/maven
mkdir -p software/jce
cd

# unzip installation file
#unzip java files
echo ""
echo "*****************************************************************************"
echo "*                            Extracting Java files                          *"
echo "*****************************************************************************"
echo ""

tar -zxvf /build/jenkins-home/slaveSetupFile/jdk-7u51-linux-x64.tar.gz -C /build/jenkins-home/software/java
#tar -zxvf /build/jenkins-home/slaveSetupFile/jdk-8u45-linux-x64.tar.gz -C /build/jenkins-home/software/java



#unzip maven
echo ""
echo "*****************************************************************************"
echo "*                           Extracting Maven files                          *"
echo "*****************************************************************************"
echo ""
tar -zxvf /build/jenkins-home/slaveSetupFile/apache-maven-2.2.1-bin.tar.gz -C /build/jenkins-home/software/maven
tar -zxvf /build/jenkins-home/slaveSetupFile/apache-maven-3.0.5-bin.tar.gz -C /build/jenkins-home/software/maven
tar -zxvf /build/jenkins-home/slaveSetupFile/apache-maven-3.2.2-bin.tar.gz -C /build/jenkins-home/software/maven

unzip -o /build/jenkins-home/slaveSetupFile/apache-maven-3.1.1-bin.zip -d /build/jenkins-home/software/maven


#unzip jce
echo ""
echo ""
echo "*****************************************************************************"
echo "*                            Extracting JCE files                           *"
echo "*****************************************************************************"
echo ""
echo ""
unzip -o /build/jenkins-home/slaveSetupFile/jce_policy-8.zip -d /build/jenkins-home/software/jce
unzip -o /build/jenkins-home/slaveSetupFile/UnlimitedJCEPolicyJDK7.zip -d /build/jenkins-home/software/jce

#rename policy.jar
#jdk7
mv /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/local_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/local_policy-original.jar
mv /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy-original.jar
#jdk8
mv /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/local_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/local_policy-original.jar
mv /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy-original.jar

#copy jce files

cp /build/jenkins-home/software/jce/UnlimitedJCEPolicy/local_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicy/US_export_policy.jar /build/jenkins-home/software/java/jdk1.7.0_51/jre/lib/security/


cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/
cp /build/jenkins-home/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/jenkins-home/software/java/jdk1.8.0_45/jre/lib/security/

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
