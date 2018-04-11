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
import org.wso2.testcoverageenforcer.Maven.FeatureAdder;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;
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
public class ParentPom extends MavenPom {

    public static final Log log = LogFactory.getLog(Application.class);

    /**
     * Class constructor
     *
     * @param pomFilePath File path to the pom file
     * @throws IOException            Catch errors while reading child pom files
     * @throws XmlPullParserException Catch errors while parsing child pom files
     */
    public ParentPom(String pomFilePath) throws IOException, XmlPullParserException {

        super(pomFilePath);
    }

    /**
     * Read the template for 'jacoco coverage check under plugin management' and add missing nodes
     * in the pom file. If the jacoco coverage check is not present at all in the pom file, this will add
     * the whole template.
     *
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold  Line coverage threshold to break the build
     * @return An ArrayList of objects in the order of,
     * Jacoco inserted pom file as an org.w3c.Document object
     * Maven surefire argument line String in the processed document,
     * Jacoco report path String in the processed document
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     */
    public ArrayList<Object> enforceCoverageCheckUnderPluginManagement(String coveragePerElement,
                                                                       String coverageThreshold)
            throws TransformerException, ParserConfigurationException, IOException, SAXException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        ArrayList<Object> processedData = JacocoCoverage.insertJacocoCoverageCheck(
                pomFile,
                Constants.MAVEN_TAG_PLUGIN_MANAGEMENT,
                coveragePerElement,
                coverageThreshold);
        Document jacocoInsertedPom = (Document) processedData.get(0);
        DocumentWriter.writeDocument(jacocoInsertedPom, pomFilePath);
        return processedData;
    }

    /**
     * Deep traverse through children and inherit coverage check from the parent
     *
     * @param children             A list of child maven objects
     * @param coveragePerElement   Per which element jacoco coverage check should be performed
     * @param coverageThreshold    Line coverage threshold to break the build
     * @param surefireArgumentLine surefire argument name in the parent pom
     * @param jacocoReportPath     jacoco report file path used in the parent pom
     * @return Check rule added at least in any child
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     * @throws XmlPullParserException       Error while parsing pom xml files
     */
    public boolean inheritCoverageCheckInChildren(List<ChildPom> children, String coveragePerElement, String coverageThreshold,
                                                  String surefireArgumentLine, String jacocoReportPath)
            throws SAXException, IOException, XmlPullParserException, ParserConfigurationException, TransformerException {

        boolean checkRuleAddition = false; // Jacoco coverage check rule added at least in any child
        for (ChildPom child : children) {
            if (child.hasChildren()) {
                checkRuleAddition = checkRuleAddition || inheritCoverageCheckInChildren(child.getChildren(), coveragePerElement, coverageThreshold, surefireArgumentLine, jacocoReportPath);
            } else if (child.hasTests()) {
                child.inheritCoverageCheckFromParent(coveragePerElement, coverageThreshold, surefireArgumentLine, jacocoReportPath);
                checkRuleAddition = checkRuleAddition || true;
            } else if (!child.hasTests()) {
                log.debug("Ignoring child module due to missing tests in " + child.getPomFilePath());
            }

        }
        return checkRuleAddition;
    }

    /**
     * Get minimum line coverage value in the project
     *
     * @return Minimum coverage ratio value among bundles
     * @throws IOException            Error in opening files
     * @throws XmlPullParserException Error while parsing pom files
     */
    public double getMinimumBundleCoverage(List<ChildPom> children)
            throws IOException, XmlPullParserException {

        double minimumCoverage = 1;
        for (ChildPom child : children) {
            if (child.hasChildren()) {
                getMinimumBundleCoverage(child.getChildren());
            } else if (child.hasTests()) {
                double bundleCoverage = child.getBundleCoverage();
                //add log for calculated coverages
                if (bundleCoverage < minimumCoverage) {
                    minimumCoverage = bundleCoverage;
                }
            }
        }
        //print minimun value
        return minimumCoverage;
    }

    /**
     * Build the project with coverage check generation and calculate minimum bundle coverage
     *
     * @return Calculated minimum coverage value
     * @throws IOException            Error in opening files
     * @throws XmlPullParserException Error while parsing pom files
     * @throws InterruptedException   Current thread interrupted by another thread while waiting
     */
    public double buildAndCalculateMinimumBundleCoverage()
            throws IOException, XmlPullParserException, InterruptedException {

        FeatureAdder.buildProject(this.pomFilePath.replace(File.separator + Constants.POM_NAME, ""), false);
        return this.getMinimumBundleCoverage(this.getChildren());
    }
}
