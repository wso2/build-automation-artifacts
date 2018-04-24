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
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileWriteException;
import org.wso2.testcoverageenforcer.Maven.POM.ParentPom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @throws PomFileReadException  Error while reading child pom
     * @throws PomFileWriteException Error while writing child pom file
     */
    public static boolean integrateJacocoCoverageCheck(
            String projectPath,
            String coveragePerElement,
            String coverageThreshold) throws PomFileReadException, PomFileWriteException {

        ParentPom parent = new ParentPom(projectPath);
        return applyJacocoCoverageCheck(parent, coveragePerElement, coverageThreshold);
    }

    private static boolean applyJacocoCoverageCheck(ParentPom parent,
                                                    String coveragePerElement,
                                                    String coverageThreshold)
            throws PomFileReadException, PomFileWriteException {

        boolean checkRuleAddition = false;      // Coverage check rule added somewhere in the project
        if (parent.hasChildren()) {
            log.debug("Child modules are available. Analysing <PluginManagement> node");
            HashMap<String, Object> results = parent.enforceCoverageCheckUnderPluginManagement(coveragePerElement, coverageThreshold);
            String surefireArgumentLine = (String) results.get(Constants.Surefire.SUREFIRE_ARGLINE_IN_THE_POM);
            String jacocoReportPath = (String) results.get(Constants.Jacoco.JACOCO_REPORT_PATH_IN_THE_POM);
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
     * This method will verify whether this project supports Jacoco coverage check rule
     *
     * @param projectPath Path to the project
     * @return True project supports Jacoco check rule. False otherwise.
     */
    public static HashMap<String, Float> inspectJacocoSupport(String projectPath)
            throws InterruptedException, PomFileReadException, PomFileWriteException, IOException {

        // Apply Jacoco check rule with zero threshold
        boolean unitTestsAvailable = integrateJacocoCoverageCheck(projectPath, Constants.COVERAGE_PER_ELEMENT, Constants.ZERO);
        HashMap<String, Float> output = new HashMap<>();
        if (!unitTestsAvailable) {
            output.put(Constants.UNIT_TESTS_AVAILABLE, Constants.Status.STATUS_FALSE);
            return output;
        }
        output = analyzeBuildForCoverageCheckRule(projectPath);
        output.put(Constants.UNIT_TESTS_AVAILABLE, Constants.Status.STATUS_TRUE);
        return output;
    }

    /**
     * Build the maven project
     *
     * @param getBuildOutput Get output of the build process
     * @throws IOException          IO errors occurring during the build
     * @throws InterruptedException Current thread interrupted by another thread while waiting
     */
    public static void buildProject(String projectPath, boolean getBuildOutput) throws IOException, InterruptedException {

        ProcessBuilder projectBuilder = getProjectBuilder(projectPath);
        projectBuilder.inheritIO();
        Process process = projectBuilder.start();
        process.waitFor();
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
    private static HashMap<String, Float> analyzeBuildForCoverageCheckRule(String projectPath)
            throws InterruptedException, IOException, PomFileReadException {

        HashMap<String, Float> analysisLog = new HashMap<>();
        analysisLog.put(Constants.Build.BUILD_LOG_COVERAGE_CHECK_SUCCESS, Constants.Status.STATUS_FALSE);
        analysisLog.put(Constants.Build.BUILD_LOG_BUILD_SUCCESS, Constants.Status.STATUS_FALSE);
        analysisLog.put(Constants.Build.BUILD_OUTPUT_MINIMUM_AVAILABLE_COVERAGE, null);

        ProcessBuilder projectBuilder = getProjectBuilder(projectPath);

        File buildLog = new File(Constants.Build.BUILD_LOGS_FILE_PATH);
        if (!buildLog.exists()) {
            buildLog.createNewFile();
        } else {
            buildLog.delete();
            buildLog.createNewFile();
        }

        projectBuilder.redirectErrorStream(true);
        projectBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(buildLog));

        Process buildProcess = projectBuilder.start();
        buildProcess.waitFor();

        analyzeBuildBuffer(buildLog, analysisLog);

        ParentPom parent = new ParentPom(projectPath);
        float minimumBundleCoverage = (float) parent.getMinimumBundleCoverage(parent.getChildren());
        analysisLog.put(Constants.Build.BUILD_OUTPUT_MINIMUM_AVAILABLE_COVERAGE, new Float(minimumBundleCoverage));
        log.info("Minimum bundle coverage is " + Float.toString(minimumBundleCoverage) + " for " + projectPath);

        return analysisLog;
    }

    /**
     * Analyse data available in a BufferedReader and set analysis results
     */
    private static void analyzeBuildBuffer(File buildLog, Map<String, Float> analysisLog)
            throws IOException {

        BufferedReader logBuffer = new BufferedReader(new FileReader(buildLog));
        String buildLine;
        while (((buildLine = logBuffer.readLine()) != null)) {
            if (buildLine != null && buildLine.contains(Constants.Build.BUILD_LOG_JACOCO_COVERAGE_CHECK_SUCCESS_MESSAGE)) {
                analysisLog.put(Constants.Build.BUILD_LOG_COVERAGE_CHECK_SUCCESS, Constants.Status.STATUS_TRUE);
            }
            if (buildLine != null && buildLine.contains(Constants.Build.BUILD_LOG_BUILD_SUCCESS_MESSAGE)) {
                analysisLog.put(Constants.Build.BUILD_LOG_BUILD_SUCCESS, Constants.Status.STATUS_TRUE);
            }
            log.debug(buildLine);
        }
    }

    /**
     * Get a ProcessBuilder object configured to build the project represented by this parent pom
     *
     * @return ProcessBuilder object configured to build this Maven project
     */
    private static ProcessBuilder getProjectBuilder(String projectPath) {

        ProcessBuilder builder = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add(Constants.Maven.MAVEN_MVN);
        commands.add(Constants.Maven.MAVEN_CLEAN);
        commands.add(Constants.Maven.MAVEN_INSTALL);
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
        commands.add(Constants.Maven.MAVEN_MVN);

        commands.add(Constants.Maven.MAVEN_CLEAN);
        builder.command(commands);
        builder.directory(new File(projectPath));
        return builder;
    }
}
