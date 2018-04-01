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
import org.wso2.testcoverageenforcer.Maven.POM.ParentMavenPom;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
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
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     * @throws XmlPullParserException       Error while parsing pom xml files
     */
    public static void integrateJacocoCoverageCheck(
            String projectPath,
            String coveragePerElement,
            String coverageThreshold) throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException {

        ParentMavenPom parent = new ParentMavenPom(projectPath);
        applyJacocoCoverageCheck(parent, coveragePerElement, coverageThreshold);
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
    public static void integrateJacocoCoverageCheck(
            String projectPath)
            throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException,
            InterruptedException {

        ParentMavenPom parent = new ParentMavenPom(projectPath);
        //Apply jacoco coverage check with zero coverage threshold per bundle in the beginning
        String coveragePerElement = Constants.COVERAGE_PER_ELEMENT;
        String coverageThreshold = Constants.ZERO;
        applyJacocoCoverageCheck(parent, coveragePerElement, coverageThreshold);
        //Build the project and get current code coverage value
        double minimumBundleCoverageRatio = parent.buildAndCalculateMinimumBundleCoverage();
        //Apply coverage check again with the newly calculated coverage ratio value
        applyJacocoCoverageCheck(
                parent,
                coveragePerElement,
                Double.toString(
                        ((short) (minimumBundleCoverageRatio * Constants.DECIMAL_CONSTANT_2)) / Constants.DECIMAL_CONSTANT_2));
    }

    private static void applyJacocoCoverageCheck(ParentMavenPom parent,
                                                 String coveragePerElement,
                                                 String coverageThreshold)
            throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException {

        if (parent.hasChildren()) {
            log.debug("Child modules are available. Analysing <PluginManagement> node");
            ArrayList<Object> results = parent.enforceCoverageCheckUnderPluginManagement(coveragePerElement, coverageThreshold);
            String surefireArgumentLine = (String) results.get(1);
            String jacocoReportPath = (String) results.get(2);
            parent.inheritCoverageCheckInChildren(parent.getChildren(), coveragePerElement, coverageThreshold, surefireArgumentLine, jacocoReportPath);
        } else if (parent.hasTests()) {
            log.debug("Tests are available in parent. Analysing <buildPlugin> node");
            parent.enforceCoverageCheckUnderBuildPlugins(coveragePerElement, coverageThreshold);
        } else if (!parent.hasTests()) {
            log.debug("Tests are not available in parent. Skipping coverage addition ");
        }
    }

    public static void main(String[] args) throws Exception{
        integrateJacocoCoverageCheck("/home/tharindu/Jenkins_Test/Wso2_repos/analytics-apim", "BUNDLE", "0.0");
    }
}
