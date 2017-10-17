#Setting up slaves using Slave Setup Plugin

##Pre-requisites to setup a slave 

Once you have obtained a VM;
 1. Install latest jdk
 2. Create a directory /build/jenkins-home at root level
 3. Configure slave setup configuration in the global configuration through the Jenkins UI (Manage Jenkins >> Configure System).
 4. Select Manage Jenkins >> Manage Nodes >> New Node to add a new node and configure. The node will launch once node configuration is saved.

The following is downloaded from /build/worker_installer/slaveSetupFile location to the new node;
 1. jdk-7u51-linux-x64.tar.gz
 2. jdk-8u144-linux-x64.tar.gz
 3. apache-maven-2.2.1-bin.tar.gz
 4. apache-maven-3.0.5-bin.tar.gz
 5. apache-maven-3.1.1-bin.zip
 6. apache-maven-3.2.2-bin.tar.gz
 7. apache-maven-3.3.9-bin.zip
 8. UnlimitedJCEPolicyJDK7.zip
 9. node-v6.10.0-linux-x64.tar.xz
 
 While the following will be copied from /build/worker_installer/slaveSetupFile location from Jenkins master to the new node;
 1. gpg keys
 2. docker certs
 3. jce_policy-8.zip

NOTE : docker.sh is use to configure docker in Ubuntu 14.04 version.

