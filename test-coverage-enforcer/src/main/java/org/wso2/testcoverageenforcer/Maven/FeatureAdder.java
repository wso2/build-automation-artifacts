/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.testcoverageenforcer.Maven;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.Maven.POM.ParentPom;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Contain methods to features to a multi-module maven project
 */
public class FeatureAdder {

    private static final Logger log = Logger.getLogger(Application.class);

    /**
     * Deep integration of Jacoco coverage check rule with an existing multi-module maven project
     *
     * @param projectPath        File path of the project containing parent pom file
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold  Line coverage threshold to break the build
     * @return Coverage check added at least in on module
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     * @throws XmlPullParserException       Error while parsing pom xml files
     */
    public static boolean integrateJacocoCoverageCheck(
            String projectPath,
            String coveragePerElement,
            String coverageThreshold) throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException {

        ParentPom parent = new ParentPom(projectPath);
        return applyJacocoCoverageCheck(parent, coveragePerElement, coverageThreshold);
    }

    private static boolean applyJacocoCoverageCheck(ParentPom parent,
                                                    String coveragePerElement,
                                                    String coverageThreshold)
            throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException {

        boolean checkRuleAddition = false;      // Coverage check rule added somewhere in the project
        if (parent.hasChildren()) {
            log.debug("Child modules are available. Analysing <PluginManagement> node");
            ArrayList<Object> results = parent.enforceCoverageCheckUnderPluginManagement(coveragePerElement, coverageThreshold);
            String surefireArgumentLine = (String) results.get(1);
            String jacocoReportPath = (String) results.get(2);
            checkRuleAddition = parent.inheritCoverageCheckInChildren(
                    parent.getChildren(),
                    coveragePerElement,
                    coverageThreshold,
                    surefireArgumentLine,
                    jacocoReportPath);
        } else if (parent.hasTests()) {
            log.debug("Tests are available in parent. Analysing <buildPlugin> node");
            parent.enforceCoverageCheckUnderBuildPlugins(coveragePerElement, coverageThreshold);
            checkRuleAddition = true;
        } else if (!parent.hasTests()) {
            log.debug("Tests are not available in parent. Skipping coverage addition ");
        }
        return checkRuleAddition;
    }

    /**
     * Deep integration of Jacoco coverage check rule with an existing multi-module maven project using existing code coverage
     * ratio as the threshold
     *
     * @param projectPath File path of the project containing parent pom file
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     * @throws XmlPullParserException       Error while parsing pom xml files
     * @throws InterruptedException         Waiting until the build to happen is failed
     */
    public static boolean integrateJacocoCoverageCheck(
            String projectPath)
            throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException,
            InterruptedException {

        ParentPom parent = new ParentPom(projectPath);
        // Apply jacoco coverage check with zero coverage threshold per bundle in the beginning
        String coveragePerElement = Constants.COVERAGE_PER_ELEMENT;
        String defaultCoverageThreshold = Constants.ZERO;
        boolean unitTestsAvailable = applyJacocoCoverageCheck(parent, coveragePerElement, defaultCoverageThreshold);
        if (!unitTestsAvailable) {
            return unitTestsAvailable;
        }
        // Build the project and get current code coverage value
        double minimumBundleCoverageRatio = parent.buildAndCalculateMinimumBundleCoverage();
        // Apply coverage check again with the newly calculated coverage ratio value
        applyJacocoCoverageCheck(
                parent,
                coveragePerElement,
                Double.toString(
                        ((short) (minimumBundleCoverageRatio * Constants.DECIMAL_CONSTANT_2)) / Constants.DECIMAL_CONSTANT_2));
        return unitTestsAvailable;
    }

    /**
     * This method will verify whether this project supports Jacoco coverage check rule
     *
     * @param projectPath Path to the project
     * @return True project supports Jacoco check rule. False otherwise.
     */
    public static boolean inspectJacocoSupport(String projectPath)
            throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {

        // Apply Jacoco check rule with zero threshold
        integrateJacocoCoverageCheck(projectPath, Constants.COVERAGE_PER_ELEMENT, Constants.ZERO);

        return searchCoverageCheckRule(buildProject(projectPath, true));
    }

    /**
     * Build the maven project
     *
     * @param getBuildOutput Get output stream of the build process
     * @return Status of the build. 0 if healthy. -1 otherwise
     * @throws IOException          IO errors occurring during the build
     * @throws InterruptedException Current thread interrupted by another thread while waiting
     */
    public static String buildProject(String projectPath, boolean getBuildOutput) throws IOException, InterruptedException {

        ProcessBuilder projectBuilder = getProjectBuilder(projectPath);
        Process process = projectBuilder.start();
        BufferedReader logBuffer = null;
        if (getBuildOutput) {
            logBuffer = (new BufferedReader(new InputStreamReader(process.getInputStream())));
        } else {
            projectBuilder.inheritIO();
        }
        int exitCode = process.waitFor();
        if (getBuildOutput) {
            StringBuilder logOutput = new StringBuilder();
            String line;
            while ((line = logBuffer.readLine()) != null) {
                logOutput.append(line);
                logOutput.append(System.getProperty(Constants.LINE_SEPERATOR));
            }
            return logOutput.toString();
        }
        return Integer.toString(exitCode);
    }

    /**
     * Clean project
     */
    public static void cleanProject(String projectPath) throws IOException, InterruptedException {

        ProcessBuilder projectCleaner = getProjectCleaner(projectPath);
        projectCleaner.start().waitFor();
    }

    /**
     * Inspect for Jacoco coverage check metric inspection available in a given build log as a String
     */
    private static boolean searchCoverageCheckRule(String buildLog) {

        return buildLog.contains(Constants.JACOCO_COVERAGE_CHECK_SUCCESS_MESSAGE);
    }

    /**
     * Get a ProcessBuilder object configured to build the project represented by this parent pom
     *
     * @return ProcessBuilder object configured to build this Maven project
     */
    private static ProcessBuilder getProjectBuilder(String projectPath) {

        ProcessBuilder builder = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add(Constants.MAVEN_MVN);
        commands.add(Constants.MAVEN_CLEAN);
        commands.add(Constants.MAVEN_INSTALL);
        builder.command(commands);
        builder.directory(new File(projectPath));
        return builder;
    }

    /**
     * Remove any file generated from a build process using Maven clean
     */
    private static ProcessBuilder getProjectCleaner(String projectPath) {

        ProcessBuilder builder = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add(Constants.MAVEN_MVN);
        commands.add(Constants.MAVEN_CLEAN);
        builder.command(commands);
        builder.directory(new File(projectPath));
        return builder;
    }

    public static void main(String[] args) throws Exception {

        //integrateJacocoCoverageCheck("/home/tharindu/Jenkins_Test/siddhi-execution-extrema", "BUNDLE", "0.0");
        System.out.println("\n\nDoes this project supports jacoco coverage check? = " + inspectJacocoSupport("/home/tharindu/Jenkins_Test/siddhi-map-json"));
    }
}
