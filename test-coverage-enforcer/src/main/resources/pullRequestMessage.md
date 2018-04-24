## Purpose
Generate unit test coverage information and add a rule to stop the build if the coverage threshold have not met

## Goals
Integrate Jacoco Maven plugin with Maven Surefire plugin to generate unit test coverage information as well as unit test coverage checking

## Approach
1) Invoke jacoco maven plugin with coverage check in the parent pom under `<build> <pluginManagement>`
2) Inherit jacoco maven plugin in child poms
3) Invoke jacoco maven plugin with coverage check in a non-multi-module maven project pom file under `<build>`
4) Reformat pom files for proper indentation and formatting
* If any part of existing jacoco implementation is present during this procedure, nothing will be added or modified.
* However jacoco argument line for Maven Surefire plugin will replace existing Maven Surefire plugin arguments.

## Documentation
For more information please visit [here.](https://github.com/wso2/build-automation-artifacts/tree/master/test-coverage-enforcer)