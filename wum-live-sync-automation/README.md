#Introduction
This is Jenkins CI automation project for WSO2 support update testing process. 
Here, Jenkins CI is explained with how to run tests against WSO2 products.
#Usage
<pre>1. How to setup the project:
	a. Pre-requisites:
		1. Install Jenkins plugins: Amazon EC2 Container Service, database-mysql, Groovy, Parameterized Scheduler, Pipeline, Workspace Cleanup Plugin
		2. Install Docker on your local
		3. Install Groovy manually on your Jenkins server node, add GROOVY_HOME environment variable to root level then give Groovy home path to Groovy Jenkins plugin
		4. Set Jenkins Global properties (environment variables):
			a. UAT_DB_HOST: Updates info DB host
			b. UAT_DB_PORT: Updates info DB port
			c. UAT_DB_USER: Updates info DB username
			d. UAT_DB_PASS: Updates info DB password
			e. UAT_JENKINS_USER: User to download created WUM update files from Jenkins server
			f. UAT_JENKINS_PASS: Password to download created WUM update files from Jenkins server
	b. Setup steps:
		1. Run Dockerfile and create docker image
		2. Upload the docker image to AWS ECR and create auto-sclaing group in AWS to apply to ECS cluster (use ECS optimized images for docker conatainers)
			a. You must have all the JDKs and other installs which are done on docker image as well as on the Jenkins server node itself in the same paths.
		3. Create uat_batch_job and get job build stage run uatBatchJob/Jenkinsfile from this location as Excute System Groovy Script
		4. Enable Jenkins configuration for "uat_batch_job" with "Delete workspace before build starts", "This project is parameterised" and "Build periodically with parameters"
			a. Give this project is parameterized, Name: BUILD_TIME, Default value: 00:00, Description: "Build periodically with parameters" cron build field name. Valid values 00:00 and 14:00.
		5. Build periodically with parameters configuration of "uat_batch_job":
			# 12AM daily midnight build
			H(0-0) 0 * * * % BUILD_TIME=00:00
			# 2PM daily SL work hours build
			H(0-0) 14 * * * % BUILD_TIME=14:00
		6. Create uat_batch_job_parallel_trigger as pipeline job and get job's pipeline script from uatBatchJobParallelTrigger/Jenkinsfile from this location
		7. Enable Jenkins configuration for "uat_batch_job_parallel_trigger" with "Build after other projects are built"
			a. Give Build after other projects are built, Projects to watch: uat_batch_job, Enable Trigger only if build is stable as well.
	c. Post-reqisites:
		N/A

2. How to run this project:
	a. Pre-requisites:
		N/A
	b. Setup steps:
		1. This will start the "uat_batch_job" at 00:00 and 14:00 SL time every day and will pick up the batch of updates submitted between previous time period.
		2. All the relevant WSO2 product - product version will be run in an AWS ECS slave server node separately and parallel.
	c. Post-reqisites:
		1. Each AWS ECS slave node with WSO2 product name - version will be printed on the "uat_batch_job_parallel_trigger" console. Please have a look on whether they have passed correctly on each child job status. And check with necessary artifacts are uploaded to Nexus UAT.
</pre>
