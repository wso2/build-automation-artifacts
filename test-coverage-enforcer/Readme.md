
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
Instructions for using the tool can be found under help.
```bash
java -jar test-coverage-enforcer-1.0-SNAPSHOT.jar -r "user_name/repo_name" -w "path/to/your/clone/directory" -e "BUNDLE" -p "true" -prop "./path/to/your/enforcer.properties"
```
```bash
-e,--element                Per which element this coverage check should be performed(BUNDLE, PACKAGE, CLASS,
                            SOURCEFILE or METHOD). Default is `BUNDLE`
 -h,--help                  Get help with usage
 -p,--pullRequest           Make a pull request once changes are done(boolean value: `true` or `false`). 
                            Default is `true`
 -prop,--properties         Properties file containing configurations for SQL server and GitHub account. 
                            This is mandatory.                          
 -r,--repository            GitHub repository name to add coverage check(Format: 'user_name/repository_name'). 
                            If this parameter is not present, tool will use sql configurations to use data
                            from the sql server 
 -t,--threshold             Line coverage threshold to break the build(a float value between 0 and 1). If this  
                            parameter is not available then the tool will build the project to calculate
                            existing line coverage and a slightly lower value to the existing coverage
                            will be used to prevent the build from braking
 -w,--workspacePath         Folder to temporally clone the repository/repositories during the procedure. 
                            This is mandatory
```
### Important Notes ###
* This tool will preserve any existing original jacoco line coverage configurations. However in order to 
ensure coverage check rule's execution, existing values in Maven Surefire argument line would be replaced with
jacoco agent argument line.
* Current version only supports adding jacoco coverage check rule for modules having a unit test folder. Hence performing tests in a separate folder will not be subjected for this rule addition like OSGI test. Any module name contains substrings 'OSGI' or 'integration' will be skipped
 