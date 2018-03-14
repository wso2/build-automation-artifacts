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

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.DocumentReader;
import org.wso2.testcoverageenforcer.FileHandler.DocumentWriter;
import org.wso2.testcoverageenforcer.FileHandler.POMReader;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * A Maven pom representation with essential methods to enforce Jacoco coverage rule
 * under build plugins
 */
abstract class MavenPom {

    /**
     * Path required to read the pom file data
     */
    String pomFilePath;

    /**
     * org.apache.maven.model.model representation of the pom file
     * to examine pom nodes
     */
    Model pomFile;

    /**
     * To initialize pom file path, pom file model and jacoco template file path
     *
     * @param pomFilePath Path for the attribute 'pomFilePath'
     * @throws IOException            Error reading the pom file
     * @throws XmlPullParserException Error parsing pom file to the maven Model
     */
    MavenPom(String pomFilePath) throws IOException, XmlPullParserException {

        this.pomFilePath = pomFilePath + File.separator + Constants.POM_NAME;
        this.pomFile = POMReader.getPOMModel(this.pomFilePath);
    }

    /**
     * To check whether this pom file contains child modules
     *
     * @return True if modules are present. False otherwise
     */
    public Boolean hasChildren() {

        return this.pomFile.getModules().size() > 0;
    }

    /**
     * Get a list of child modules to examine each
     *
     * @return A list of child modules if available. Empty list if not.
     * @throws IOException            Catch errors while reading child pom files
     * @throws XmlPullParserException Catch errors while parsing child pom files
     */
    public List<ChildMavenPom> getChildren() throws IOException, XmlPullParserException {

        List<ChildMavenPom> childMavenPomList = new ArrayList<>(0);
        for (String eachChildPomPath : this.pomFile.getModules()) {
            childMavenPomList.add(new ChildMavenPom(this.pomFilePath.replace(Constants.POM_NAME,
                    "") + eachChildPomPath));
        }
        return childMavenPomList;
    }

    /**
     * Check whether this pom directory should contain tests by searching for src/test folder
     *
     * @return True if src/test exists. Otherwise False.
     */
    public Boolean hasTests() {

        File testFolder = new File(this.pomFilePath.replace(Constants.POM_NAME,
                "") + Constants.TEST_FOLDER);
        return testFolder.exists();
    }

    /**
     * Read the template for 'jacoco coverage check under build plugin' and add missing nodes
     * in the pom file. If the jacoco coverage check is not present at all in the pom file, this will add
     * the whole template.
     */
    public void enforceCoverageCheckUnderBuildPlugins(String coveragePerElement, String coverageThreshold)
            throws TransformerException, ParserConfigurationException, IOException, SAXException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        Document jacocoInsertedPom = JacocoCoverage.insertJacocoCoverageCheck(
                pomFile,
                Constants.MAVEN_TAG_BUILD,
                coveragePerElement,
                coverageThreshold);
        DocumentWriter.writeDocument(jacocoInsertedPom, pomFilePath);
    }

    /**
     * Get pom file path
     */
    public String getPomPath() {
        return this.pomFilePath;
    }
}
