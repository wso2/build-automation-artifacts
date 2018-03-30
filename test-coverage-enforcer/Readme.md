
# wso2-test-coverage-enforcer #
This repository produces a java command line application to integrate jacoco coverage check rule with 
either a single Maven project or a list of projects read from a sql table.
## Workflow ##
This tool take a GitHub url to a wso2 product repository and perform
following tasks. Also this tool supports reading a set of urls from a sql table and perform followings for each of them.
* Check whether the repository is active for the past six months and ignore inactive repositories
* Forking in to a GitHub account
* Cloning in to the local computer
* Integrate jacoco check rule
* Commit changes and push to the forked remote
* Create a pull request 
## Getting Started ##
Build the Maven project and use the jar file. Refer to the enforcer.properties file in the project to configure settings for the sql server and the GitHub account used for forking.
## Usage ##
Instructions for using the tool can be found under help. Use -h tag.
### Important Notes ###
* This tool will preserve any original jacoco line coverage configurations. However in order to 
ensure coverage check rule's execution, existing values in Maven Surefire argument line would be replaced with
jacoco agent argument line.