# ------------------------------------------------------------------------
#
# Copyright 2016 WSO2, Inc. (http://wso2.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# ------------------------------------------------------------------------

FROM ubuntu:trusty

RUN \ 
apt-get -q update && apt-get install -y openssh-server software-properties-common git ant curl zip unzip xvfb dbus-x11 ttf-ancient-fonts \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/* \
&& mkdir /var/run/sshd


RUN \
apt-get update \
&& apt-get --yes --force-yes install firefox \
&& apt-get --yes --force-yes install firefox=28.0+build2-0ubuntu2 \
&& apt-mark hold firefox \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/*

RUN \
mkdir -p /build/software/maven \
&& wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.0.5-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.0.5-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.2.2/binaries/apache-maven-3.2.2-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.2.2-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.2.2-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.3.9-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.3.9-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.5.4-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.5.4-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-2/2.2.1/binaries/apache-maven-2.2.1-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-2.2.1-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-2.2.1-bin.tar.gz

RUN mkdir -p /build/software/java

COPY jdk-8u171-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-8u171-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-8u171-linux-x64.tar.gz \
&& mkdir -p /build/software/jce \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
&& unzip -o /build/software/jce/jce_policy-8.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/local_policy.jar /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/local_policy-original.jar \
&& mv /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/US_export_policy.jar /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/ \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/software/java/jdk1.8.0_171/jre/lib/security/policy/unlimited/ \
&& rm  /build/software/jce/jce_policy-8.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicyJDK8

COPY jdk-8u144-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-8u144-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-8u144-linux-x64.tar.gz \
&& mkdir -p /build/software/jce \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
&& unzip -o /build/software/jce/jce_policy-8.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.8.0_144/jre/lib/security/local_policy.jar /build/software/java/jdk1.8.0_144/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.8.0_144/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.8.0_144/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/software/java/jdk1.8.0_144/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/software/java/jdk1.8.0_144/jre/lib/security/ \
&& rm  /build/software/jce/jce_policy-8.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicyJDK8

COPY jdk-8u45-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-8u45-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-8u45-linux-x64.tar.gz \
&& mkdir -p /build/software/jce \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
&& unzip -o /build/software/jce/jce_policy-8.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.8.0_45/jre/lib/security/local_policy.jar /build/software/java/jdk1.8.0_45/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.8.0_45/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/software/java/jdk1.8.0_45/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/software/java/jdk1.8.0_45/jre/lib/security/ \
&& rm  /build/software/jce/jce_policy-8.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicyJDK8

COPY jdk-7u51-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-7u51-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-7u51-linux-x64.tar.gz \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/7/UnlimitedJCEPolicyJDK7.zip \
&& unzip -o /build/software/jce/UnlimitedJCEPolicyJDK7.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.7.0_51/jre/lib/security/local_policy.jar /build/software/java/jdk1.7.0_51/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.7.0_51/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicy/local_policy.jar /build/software/java/jdk1.7.0_51/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicy/US_export_policy.jar /build/software/java/jdk1.7.0_51/jre/lib/security/ \
&& rm /build/software/jce/UnlimitedJCEPolicyJDK7.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicy

RUN apt-get update && apt-get install -y openjdk-7-jdk && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN \
add-apt-repository ppa:openjdk-r/ppa -y \
&& apt-get update && apt-get install -y openjdk-8-jdk \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/*

COPY jdk-6u33-linux-x64.bin /build/software/java

RUN \
chmod a+x /build/software/java/jdk-6u33-linux-x64.bin \
&& cd /build/software/java \
&& echo "\n" | sh /build/software/java/jdk-6u33-linux-x64.bin \
&& rm /build/software/java/jdk-6u33-linux-x64.bin

RUN \
wget -P /build/software/nodejs https://nodejs.org/dist/v8.8.1/node-v8.8.1-linux-x64.tar.xz \
&& tar -xvf /build/software/nodejs/node-v8.8.1-linux-x64.tar.xz --directory /build/software/nodejs \
&& rm /build/software/nodejs/node-v8.8.1-linux-x64.tar.xz

RUN wget -P /build/software/go https://dl.google.com/go/go1.10.linux-amd64.tar.gz \
    && tar -xzf /build/software/go/go1.10.linux-amd64.tar.gz --directory /build/software/go && mv /build/software/go/go /build/software/go/go-1.10 && rm /build/software/go/go1.10.linux-amd64.tar.gz

RUN wget -P /build/software/go https://dl.google.com/go/go1.12.5.linux-amd64.tar.gz \
    && tar -xzf /build/software/go/go1.12.5.linux-amd64.tar.gz --directory /build/software/go && mv /build/software/go/go /build/software/go/go-1.12.5 && rm /build/software/go/go1.12.5.linux-amd64.tar.gz

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

RUN \
apt-get update  && apt-get -y install python3-pip \
&& pip3 --version \
&& apt-get -y install build-essential python3-dev \
&& pip3 install mkdocs==1.0.4 && mkdocs --version \
&& pip3 install mkdocs-material==4.4.0

RUN \
apt-get update && apt-get -y install python-pip \
&& pip --version \
&& apt-get install -y libxml2-dev libxslt-dev \
&& pip install beautifulsoup4 \
&& apt-get install python-lxml

ENV PATH=$PATH:/build/software/nodejs/node-v8.8.1-linux-x64/bin

RUN \
echo "net.ipv4.ip_local_port_range=15000 61000" >> /etc/sysctl.conf \
&& echo "net.ipv4.tcp_fin_timeout=30" >> /etc/sysctl.conf \
&& echo "*	soft	nofile	65535" >> /etc/security/limits.conf \
&& echo "*	hard	nofile	65535" >> /etc/security/limits.conf \
&& echo "*	soft	nproc	65535" >> /etc/security/limits.conf \
&& echo "*	hard	nproc	65535" >> /etc/security/limits.conf

RUN \
mkdir -p /home/jenkins

RUN mkdir -p /build/gpg-keys/.gnupg 
ADD .gnupg /build/gpg-keys/.gnupg

ARG JENKINS_REMOTING_VERSION=3.5

# See https://github.com/jenkinsci/docker-slave/blob/2.62/Dockerfile#L32
RUN curl --create-dirs -sSLo /usr/share/jenkins/slave.jar https://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/$JENKINS_REMOTING_VERSION/remoting-$JENKINS_REMOTING_VERSION.jar \
  && chmod 755 /usr/share/jenkins \
  && chmod 644 /usr/share/jenkins/slave.jar \
  && chmod a+rwx /home/jenkins

EXPOSE 22

COPY entrypoint.sh /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

CMD ["/usr/sbin/sshd", "-D"]
