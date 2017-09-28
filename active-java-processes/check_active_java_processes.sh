#!/bin/bash
number_of_java_processes=0

#get number of runing java processes except sun.tools.jps.Jps -Dapplication.home=/usr/lib/jvm/....
number_of_java_processes=$(jps -vl | grep -v 'sun.tools.jps.Jps' | grep -v 'hudson.remoting.jnlp.Main' | grep -v grep | wc -l)
java_processes=$(jps -vl | grep -v 'sun.tools.jps.Jps' | grep -v 'hudson.remoting.jnlp.Main' | grep -v grep)

#if there are java processes except sun.tools.jps.Jps -Dapplication.home=/usr/lib/jvm/.... then exit with 100
if [ $number_of_java_processes -gt 0 ]
then
 echo There are some java processes that are not properly shutdown....
 #print runing java processes
 echo Get all runung java processes....
 echo $java_processes 

 #get ports listening by java processes with pid 
 echo Get all ports listening by java processes....
 netstat -nlp | grep 'java' 

 #if there are runing carbon servers then kill them. 
 ps -ef | grep 'carbon.bootstrap' | grep 'carbon.home' | grep -v grep | awk '{print $2}' | xargs kill -9
 echo 'The build job is failed by the Jenkins since there are some java processes which are not properly shut down after the build is finished. Therefore, please consider this if build is fail even maven build is completed successfully.'

 # exit with 100 state
 echo 'exit 100'
 exit 100
else
 exit 0
fi
exit 0 
