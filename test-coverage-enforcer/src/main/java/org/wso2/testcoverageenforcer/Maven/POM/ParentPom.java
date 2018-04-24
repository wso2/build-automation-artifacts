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
import org.w3c.dom.Document;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.DocumentReader;
import org.wso2.testcoverageenforcer.FileHandler.DocumentWriter;
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileWriteException;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
     * @throws PomFileReadException Error while reading the pom
     */
    public ParentPom(String pomFilePath) throws PomFileReadException {

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
     * @throws PomFileReadException  Error while reading the pom
     * @throws PomFileWriteException Error while writing the pom file
     */
    public HashMap<String, Object> enforceCoverageCheckUnderPluginManagement(String coveragePerElement,
                                                                             String coverageThreshold)
            throws PomFileReadException, PomFileWriteException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        HashMap<String, Object> processedData = JacocoCoverage.insertJacocoCoverageCheck(
                pomFile,
                Constants.Maven.MAVEN_TAG_PLUGIN_MANAGEMENT,
                coveragePerElement,
                coverageThreshold);
        Document jacocoInsertedPom = (Document) processedData.get(Constants.Jacoco.JACOCO_INSERTED_POM);
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
     * @throws PomFileReadException  Error while reading the pom
     * @throws PomFileWriteException Error while writing the pom file
     */
    public boolean inheritCoverageCheckInChildren(List<ChildPom> children, String coveragePerElement, String coverageThreshold,
                                                  String surefireArgumentLine, String jacocoReportPath)
            throws PomFileReadException, PomFileWriteException {

        boolean checkRuleAddition = false; // Jacoco coverage check rule added at least in any child
        for (ChildPom child : children) {
            if (child.hasChildren()) {
                boolean ruleAdded = inheritCoverageCheckInChildren(child.getChildren(), coveragePerElement, coverageThreshold, surefireArgumentLine, jacocoReportPath);
                checkRuleAddition = checkRuleAddition || ruleAdded;
            } else if (child.hasTests()) {
                child.inheritCoverageCheckFromParent(coveragePerElement, coverageThreshold, surefireArgumentLine, jacocoReportPath);
                checkRuleAddition = true;
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
     * @throws IOException          Error in opening files
     * @throws PomFileReadException Error while reading the pom
     */
    public double getMinimumBundleCoverage(List<ChildPom> children)
            throws PomFileReadException, IOException {

        double minimumCoverage = 1;
        for (ChildPom child : children) {
            if (child.hasChildren()) {
                getMinimumBundleCoverage(child.getChildren());
            } else if (child.hasTests()) {
                double bundleCoverage = child.getBundleCoverage();
                log.info("Line coverage per bundle is " + Double.toString(bundleCoverage) + " for " + child.getPomFilePath());
                //add log for calculated coverages
                if (bundleCoverage < minimumCoverage) {
                    minimumCoverage = bundleCoverage;
                }
            }
        }
        // Print minimum value
        return minimumCoverage;
    }
}
