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

package org.wso2.testcoverageenforcer.Maven.POM;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.DocumentReader;
import org.wso2.testcoverageenforcer.FileHandler.DocumentWriter;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;
import org.wso2.testcoverageenforcer.Maven.Jacoco.CoverageReportReader;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * A Maven pom representation with added capabilities to enforce jacoco coverage check
 * under plugin management
 */
public class ParentMavenPom extends MavenPom {

    public static final Log log = LogFactory.getLog(Application.class);
    /**
     * Class constructor
     * @param pomFilePath File path to the pom file
     * @throws IOException            Catch errors while reading child pom files
     * @throws XmlPullParserException Catch errors while parsing child pom files
     */
    public ParentMavenPom(String pomFilePath) throws IOException, XmlPullParserException {

        super(pomFilePath);
    }

    /**
     * Read the template for 'jacoco coverage check under plugin management' and add missing nodes
     * in the pom file. If the jacoco coverage check is not present at all in the pom file, this will add
     * the whole template.
     *
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold Line coverage threshold to break the build
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     * @throws TransformerException Error while writing pom file back
     */
    public void enforceCoverageCheckUnderPluginManagement(String coveragePerElement,
                                                          String coverageThreshold)
            throws TransformerException, ParserConfigurationException, IOException, SAXException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        Document jacocoInsertedPom = JacocoCoverage.insertJacocoCoverageCheck(
                pomFile,
                Constants.MAVEN_TAG_PLUGIN_MANAGEMENT,
                coveragePerElement,
                coverageThreshold);
        DocumentWriter.writeDocument(jacocoInsertedPom, pomFilePath);
    }

    /**
     * Deep traverse through children and inherit coverage check from the parent
     *
     * @param children A list of child maven objects
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold Line coverage threshold to break the build
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     * @throws TransformerException Error while writing pom file back
     * @throws XmlPullParserException Error while parsing pom xml files
     */
    public void inheritCoverageCheckInChildren(List<ChildMavenPom> children, String coveragePerElement, String coverageThreshold)
            throws SAXException, IOException, XmlPullParserException, ParserConfigurationException, TransformerException {

        for (ChildMavenPom child : children) {
            if (child.hasChildren()) {
                inheritCoverageCheckInChildren(child.getChildren(), coveragePerElement, coverageThreshold);
            } else if (child.hasTests()) {
                child.inheritCoverageCheckFromParent(coveragePerElement, coverageThreshold);
            } else if (!child.hasTests()) {
                // add logging
            }

        }
    }

    /**
     * Build the maven project
     *
     * @throws IOException IO errors occurring during the build
     * @throws InterruptedException Current thread interrupted by another thread while waiting
     */
    public int buildProject() throws IOException, InterruptedException{

        ProcessBuilder builder = new ProcessBuilder();
        List<String> commands = new ArrayList<String>();
        commands.add(Constants.MAVEN_MVN);
        commands.add(Constants.MAVEN_CLEAN);
        commands.add(Constants.MAVEN_INSTALL);
        builder.command(commands);
        builder.directory(new File(this.pomFilePath.replace(File.separator + Constants.POM_NAME, Constants.EMPTY_STRING)));
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode == Constants.MAVEN_HEALTHY_BUILD) {
            return Constants.MAVEN_HEALTHY_BUILD;
        }
        else {
            return Constants.MAVEN_BAD_BUILD;
        }
    }

    /**
     * Get minimum line coverage value in the project
     *
     * @return Minimum coverage ratio value among bundles
     * @throws IOException Error in opening files
     * @throws XmlPullParserException Error while parsing pom files
     */
     public double getMinimumBundleCoverage(List<ChildMavenPom> children)
             throws IOException, XmlPullParserException {

         double minimumCoverage = 1;
         for (ChildMavenPom child : children) {
             if (child.hasChildren()) {
                 getMinimumBundleCoverage(child.getChildren());
             } else if (child.hasTests()) {
                 double bundleCoverage = child.getBundleCoverage();
                 if (bundleCoverage < minimumCoverage) {
                     minimumCoverage = bundleCoverage;
                 }
             }
         }
         return minimumCoverage;
     }
    /**
     * Build the project with coverage check generation and calculate minimum bundle coverage
     *
     * @return Calculated minimum coverage value
     * @throws IOException Error in opening files
     * @throws XmlPullParserException Error while parsing pom files
     * @throws InterruptedException Current thread interrupted by another thread while waiting
     */
    public double buildAndCalculateMinimumBundleCoverage()
    throws IOException, XmlPullParserException, InterruptedException{
        this.buildProject();
        return this.getMinimumBundleCoverage(this.getChildren());
    }
}
