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

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.DocumentReader;
import org.wso2.testcoverageenforcer.FileHandler.DocumentWriter;
import org.wso2.testcoverageenforcer.Maven.Jacoco.CoverageReportReader;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * A Maven pom representation with added capabilities to inherit jacoco coverage check
 * from a parent pom file
 */
public class ChildPom extends MavenPom {

    /**
     * Class constructor
     *
     * @param pomFilePath File path to the pom file
     * @throws IOException            Error reading the pom file
     * @throws XmlPullParserException Error while parsing pom xml
     */
    public ChildPom(String pomFilePath) throws IOException, XmlPullParserException {

        super(pomFilePath);
    }

    /**
     * Read the template for 'inherit jacoco coverage check from parent' and add missing nodes
     * in the pom file. If the inherit jacoco coverage check is not present at all in the pom file, this will add
     * the whole template.
     *
     * @param coveragePerElement   Per which element jacoco coverage check should be performed
     * @param coverageThreshold    Line coverage threshold to break the build
     * @param surefireArgumentLine surefire argument name in the parent pom
     * @param jacocoReportPath     jacoco report file path used in the parent pom
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     * @throws TransformerException         Error while writing pom file back
     */
    public void inheritCoverageCheckFromParent(String coveragePerElement, String coverageThreshold, String surefireArgumentLine, String jacocoReportPath)
            throws ParserConfigurationException, IOException, SAXException, TransformerException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        Document jacocoInheritedPom = JacocoCoverage.inheritCoverageCheckFromParent(
                pomFile,
                coveragePerElement,
                coverageThreshold,
                surefireArgumentLine,
                jacocoReportPath);
        DocumentWriter.writeDocument(jacocoInheritedPom, pomFilePath);
    }

    /**
     * Calculate line coverage ratio for the bundle
     *
     * @return Combined line coverage value for all packages
     * @throws IOException Jacoco execution file not found
     */
    public double getBundleCoverage() throws IOException {

        String buildFolderPath = this.pomFilePath.replace(File.separator + Constants.POM_NAME, Constants.EMPTY_STRING)
                + Constants.BUILD_TARGET_FOLDER;
        File buildFolder = new File(buildFolderPath);
        CoverageReportReader coverageReader = new CoverageReportReader(buildFolder);
        return coverageReader.getCoverageThresholdForBundle();
    }
}
