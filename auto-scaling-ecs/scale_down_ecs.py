'''
/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
'''


#---------------Auto Scaling Down Instances When There Are No Running Containers---------------------
import boto3
import datetime 
import math
import time

#Method to get the unix time
def unix_time(time_stamp):
    return time.mktime(time_stamp.timetuple())

#Method to scale down the instances 
def scale_down(containerInstance, container_instances_count):
    print "\nContainer Instance ID: " + containerInstance['ec2InstanceId']
    #Check if the instance state has running task or pending tasks and number of instances in the cluster is greater than 1
    if (containerInstance['runningTasksCount'] == 0 and containerInstance['pendingTasksCount']== 0 and container_instances_count > 1):
        #registration time of the container instance
        time_reg = containerInstance['registeredAt']
	#current time of the instance
        current_time = datetime.datetime.now()
        #next billing hour of the instance
        hours_difference = (unix_time(current_time) - unix_time(time_reg))/(60*60)
        next_billing_hour = time_reg + datetime.timedelta(hours=math.ceil(hours_difference))
	print ("Next billing hour begins: %s" % next_billing_hour )
        #check if the current time close to 45 minutes of the current billing hour
        threshold_time = next_billing_hour - datetime.timedelta(minutes=15)
        print ("Threshold time to kill: %s" % threshold_time )
        print ("Current time: %s" % current_time )
        if unix_time(threshold_time) < unix_time(current_time):
            #Terminate the instance and number of available container instances would be decreased by 1
            print "Terminating instance " + containerInstance['ec2InstanceId'] 
            asgClient.terminate_instance_in_auto_scaling_group(InstanceId=containerInstance['ec2InstanceId'], ShouldDecrementDesiredCapacity=True)
	    container_instances_count -= 1
	    print ("Size of the cluster after termination %s\n" %container_instances_count)
    #if there are running/pending containers inside the instances
    else :
        print ("Running Containers {} \nPending tasks {} \nCluster size {} \n ".format(containerInstance['runningTasksCount'],containerInstance['pendingTasksCount'],container_instances_count))
    

#choose the aws user which to access the resources
session = boto3.Session(profile_name='account2')

#ecs client and auto scaling group resource generation
ecsClient = session.client(service_name='ecs')
asgClient = session.client(service_name='autoscaling')

#list container instances of the cluster
clusterListResp = ecsClient.list_container_instances(cluster='Jenkins-ECS')

#details of each container
containerDetails = ecsClient.describe_container_instances(cluster='Jenkins-ECS', containerInstances=clusterListResp['containerInstanceArns'])

#Get the instances count in the cluster
container_instances_count = len(containerDetails['containerInstances'])

#loop through every instances to check if it should be terminated
for containerInstance in containerDetails['containerInstances']:
    scale_down(containerInstance, container_instances_count)





