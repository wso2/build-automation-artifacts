#!/bin/bash

export USER_ID=${LOCAL_USER_ID:-2000}
export GROUP_ID=${LOCAL_GROUP_ID:-2000}

groupadd -g $GROUP_ID -r jenkins
useradd -u $USER_ID -g $GROUP_ID -d /home/jenkins -s /bin/sh -g jenkins jenkins
echo "jenkins:jenkins" | chpasswd

chown -R jenkins:jenkins /home/jenkins

chown -R jenkins:jenkins /build/software/maven

chown -R jenkins:jenkins /build/gpg-keys/.gnupg
exec "$@"
