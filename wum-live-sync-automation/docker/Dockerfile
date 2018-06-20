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

FROM 878565184191.dkr.ecr.ap-southeast-1.amazonaws.com/wso2-jenkins-ecs-slave:latest

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.3.9-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.3.9-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.3.3-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.3.3-bin.tar.gz

RUN \
wget -P /build/software/maven https://archive.apache.org/dist/maven/maven-3/3.2.1/binaries/apache-maven-3.2.1-bin.tar.gz \
&& tar -xvzf /build/software/maven/apache-maven-3.2.1-bin.tar.gz --directory /build/software/maven \
&& rm /build/software/maven/apache-maven-3.2.1-bin.tar.gz

RUN mkdir -p /build/software/ant

COPY apache-ant-1.9.6-bin.tar.gz /build/software/ant/

RUN tar -xzf /build/software/ant/apache-ant-1.9.6-bin.tar.gz --directory /build/software/ant && rm /build/software/ant/apache-ant-1.9.6-bin.tar.gz

RUN mkdir -p /build/software/java

COPY jdk-8u151-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-8u151-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-8u151-linux-x64.tar.gz \
&& mkdir -p /build/software/jce \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
&& unzip -o /build/software/jce/jce_policy-8.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.8.0_151/jre/lib/security/policy/unlimited/local_policy.jar /build/software/java/jdk1.8.0_151/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.8.0_151/jre/lib/security/policy/unlimited/US_export_policy.jar /build/software/java/jdk1.8.0_151/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/software/java/jdk1.8.0_151/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/software/java/jdk1.8.0_151/jre/lib/security/ \
&& rm  /build/software/jce/jce_policy-8.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicyJDK8

COPY jdk-8u131-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-8u131-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-8u131-linux-x64.tar.gz \
&& mkdir -p /build/software/jce \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
&& unzip -o /build/software/jce/jce_policy-8.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.8.0_131/jre/lib/security/local_policy.jar /build/software/java/jdk1.8.0_131/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.8.0_131/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.8.0_131/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/local_policy.jar /build/software/java/jdk1.8.0_131/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicyJDK8/US_export_policy.jar /build/software/java/jdk1.8.0_131/jre/lib/security/ \
&& rm  /build/software/jce/jce_policy-8.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicyJDK8

COPY jdk-7u79-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-7u79-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-7u79-linux-x64.tar.gz \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/7/UnlimitedJCEPolicyJDK7.zip \
&& unzip -o /build/software/jce/UnlimitedJCEPolicyJDK7.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.7.0_79/jre/lib/security/local_policy.jar /build/software/java/jdk1.7.0_79/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.7.0_79/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.7.0_79/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicy/local_policy.jar /build/software/java/jdk1.7.0_79/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicy/US_export_policy.jar /build/software/java/jdk1.7.0_79/jre/lib/security/ \
&& rm /build/software/jce/UnlimitedJCEPolicyJDK7.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicy

COPY jdk-7u65-linux-x64.tar.gz /build/software/java

RUN \
tar -xvzf /build/software/java/jdk-7u65-linux-x64.tar.gz --directory /build/software/java \
&& rm /build/software/java/jdk-7u65-linux-x64.tar.gz \
&& wget -P /build/software/jce --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jce/7/UnlimitedJCEPolicyJDK7.zip \
&& unzip -o /build/software/jce/UnlimitedJCEPolicyJDK7.zip -d /build/software/jce \
&& mv /build/software/java/jdk1.7.0_65/jre/lib/security/local_policy.jar /build/software/java/jdk1.7.0_65/jre/lib/security/local_policy-original.jar \
&& mv /build/software/java/jdk1.7.0_65/jre/lib/security/US_export_policy.jar /build/software/java/jdk1.7.0_65/jre/lib/security/US_export_policy-original.jar \
&& cp /build/software/jce/UnlimitedJCEPolicy/local_policy.jar /build/software/java/jdk1.7.0_65/jre/lib/security/ \
&& cp /build/software/jce/UnlimitedJCEPolicy/US_export_policy.jar /build/software/java/jdk1.7.0_65/jre/lib/security/ \
&& rm /build/software/jce/UnlimitedJCEPolicyJDK7.zip \
&& rm -r /build/software/jce/UnlimitedJCEPolicy

COPY jdk-6u33-linux-x64.bin /build/software/java

RUN \
chmod a+x /build/software/java/jdk-6u33-linux-x64.bin \
&& cd /build/software/java \
&& echo "\n" | sh /build/software/java/jdk-6u33-linux-x64.bin \
&& rm /build/software/java/jdk-6u33-linux-x64.bin

RUN \
apt-get update && apt-get install -y zip
