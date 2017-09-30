#!/bin/bash

#get runing java processes except sun.tools.jps.Jps -Dapplication.home=/usr/lib/jvm/.. and slave jar (hudson.remoting.jnlp.Main
JAVA_PROCESSES=$(jps -vl | grep -v 'sun.tools.jps.Jps' | grep -v 'hudson.remoting.jnlp.Main' | grep -v grep)

#if there are java processes except sun.tools.jps.Jps -Dapplication.home=/usr/lib/jvm/.. and slave jar then exit with 100
if [ -z "$JAVA_PROCESSES" ]
then
 echo 'Running port and java processes validation has become successful.'
 exit 0
else
 echo 'There are some java processes that are not properly shutdown.'
 #print running java processes
 echo 'Get all running java processes...'
 printf '%s\n' "$JAVA_PROCESSES"

 #get ports listening by java processes with pid 
 echo 'Get all ports listened by java processes...'
 netstat -nlp | grep 'java' 

 #if there are runing carbon servers then kill them. 
 ps -ef | grep 'carbon.bootstrap' | grep 'carbon.home' | grep -v grep | awk '{print $2}' | xargs kill -9
 echo 'The build job is failed by the Jenkins since there are some java processes which are not properly shut down after the build is finished. Therefore, please consider this if build is fail even maven build is completed successfully.'

 #exit with 100 state
 echo 'exit 100'
 exit 100
fi
exit 0 

