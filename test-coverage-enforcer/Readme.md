
# wso2-test-coverage-enforcer #
This repository produces a java command line application to integrate jacoco coverage check rule with 
a Maven project.
## Workflow ##
This tool take a GitHub url to a wso2 product repository and perform
following tasks.
* Forking in to a GitHub account([GitHub account](https://github.com/test-coverage-enforce-bot))
* Cloning in to the local computer
* Integrate jacoco check rule
* Make a commit and push to the forked remote
* Create a pull request 
## Getting Started ##
Build the Maven project and use the jar file.
## Usage ##
Instructions for using the tool can be found under help. Use -h tag.
### Important Notes ###
* This tool will preserve any original jacoco line coverage configurations. However in order to 
ensure coverage check rule's execution, Maven Surefire argument line would be replaced with
jacoco agent argument line.