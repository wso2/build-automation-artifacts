#!/bin/bash

export USER_ID=${LOCAL_USER_ID:-2000}
export GROUP_ID=${LOCAL_GROUP_ID:-2000}

groupadd -g $GROUP_ID -r jenkins
useradd -u $USER_ID -g $GROUP_ID -d /home/jenkins -s /bin/sh -g jenkins jenkins
echo "jenkins:jenkins" | chpasswd

chown -R jenkins:jenkins /home/jenkins

chown -R jenkins:jenkins /build/software/maven

chown -R jenkins:jenkins /build/gpg-keys/.gnupg


if [[ $# -eq 1 ]]; then

	# if `docker run` only has one arguments, we assume user is running alternate command like `bash` to inspect the image
	exec "$@"

else

	# if -tunnel is not provided try env vars
	if [[ "$@" != *"-tunnel "* ]]; then
		if [[ ! -z "$JENKINS_TUNNEL" ]]; then
			TUNNEL="-tunnel $JENKINS_TUNNEL"		
		fi
	fi

	if [[ ! -z "$JENKINS_URL" ]]; then
		URL="-url $JENKINS_URL"
	fi

	exec java $JAVA_OPTS -Duser.home=/home/jenkins -cp /usr/share/jenkins/slave.jar hudson.remoting.jnlp.Main -headless $TUNNEL $URL "$@"
fi
