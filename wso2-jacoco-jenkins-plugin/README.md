WSO2 Jenkins Jacoco Plugin
==========================

| Powered By | Talks With | 
|:-------------------------:|:-------------------------:
| ![](jacoco_logo.png) | ![](jenkins_logo.png) | 

Allows you to find and visualize the information of code coverage for a project within Jenkins

Note: Version 2.0.0 and higher requires using JaCoCo 0.7.5 or newer, if your projects still use JaCoCo 0.7.4, 
the plugin will not display any code-coverage numbers any more! In this case please use version 1.0.19 until you can update jacoco in your codebase.

This plugin was written extending the [jacoco-plugin](https://github.com/jenkinsci/jacoco-plugin)

How to build and test
=====================

* Build the plugin:

`mvn package`

* Test locally (invokes a local Jenkins instance with the plugin installed):

`mvn hpi:run`

See https://jenkinsci.github.io/maven-hpi-plugin/ for details.


