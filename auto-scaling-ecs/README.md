
Amazon Boto3 SDK based python script that can scale-down a given Amazon ECS cluster. 

This checks the reserved CPU and whether there are any idle ECS containr instances. 
If there are idle instances, and if its close to the end of the billing cycle (1hr), 
and there are no pending Jenkins jobs waiting in the queue, then this will shutdown the idle ECS container instance.

This should be run as a cron job every 15mins. This can be run as a Jenkins job if re'quired.
