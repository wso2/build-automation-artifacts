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

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.w3c.dom.Document;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.DocumentReader;
import org.wso2.testcoverageenforcer.FileHandler.DocumentWriter;
import org.wso2.testcoverageenforcer.FileHandler.POMReader;
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileWriteException;
import org.wso2.testcoverageenforcer.Maven.Jacoco.JacocoCoverage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Maven pom representation with essential methods to enforce Jacoco coverage rule
 * under build plugins
 */
abstract class MavenPom {

    /**
     * Logging
     */
    private static final Logger log = Logger.getLogger(MavenPom.class);

    /**
     * Path required to read the pom file data
     */
    final String pomFilePath;

    /**
     * org.apache.maven.model.model representation of the pom file
     * to examine pom nodes
     */
    private Model pomFile;

    /**
     * To initialize pom file path, pom file model and jacoco template file path
     *
     * @param pomFilePath Path for the attribute 'pomFilePath'
     * @throws PomFileReadException Error while reading the pom
     */
    MavenPom(String pomFilePath) throws PomFileReadException {

        this.pomFilePath = pomFilePath + File.separator + Constants.POM_NAME;
        this.pomFile = POMReader.getPOMModel(this.pomFilePath);
    }

    /**
     * To check whether this pom file contains child modules
     *
     * @return True if modules are present. False otherwise
     */
    public Boolean hasChildren() {

        if (!(this.pomFile.getModules().size() > 0)) { //Check with profiles for children
            for (Profile eachProfile : this.pomFile.getProfiles()) {
                if (eachProfile.getModules().size() > 0) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Get a list of child modules to examine each
     *
     * @return A list of child modules if available. Empty list if not.
     * @throws PomFileReadException Error while reading the pom
     */
    public List<ChildPom> getChildren() throws PomFileReadException {

        List<ChildPom> childPomList = new ArrayList<>(0);
        /*
        Sometimes modules are available under <profiles> tag. Not under <modules> tag.
        In that case, look under <profiles> tag for modules and add coverage check for
        all available modules
         */
        List<String> modules = this.pomFile.getModules();
        if (modules.size() == 0) {
            List<Profile> profilesList = this.pomFile.getProfiles();

            //Both <modules> and <profiles> are not present, pom does not have children
            if (profilesList.size() == 0) return null;
            List<List<String>> modulesList = new ArrayList<>();
            for (Profile eachProfile : profilesList) {
                List<String> modulesInProfile = eachProfile.getModules();
                if (modulesInProfile.size() > 0) {
                    modulesList.add(modulesInProfile);
                }
            }

            //No modules are present in available profiles
            if (modulesList.size() == 0) return null;
            modules = modulesList.stream().flatMap(x -> x.stream()).collect(Collectors.toList());
        }
        for (String eachChildPomPath : modules) {
            if (eachChildPomPath.contains(Constants.Child.CHILD_NAME_TESTS_INTEGRATION)) {
                log.info("Skipping " + this.pomFilePath.replace(Constants.POM_NAME, "") + eachChildPomPath + ". Integration test module");
                continue;    //Neglect the module 'tests-integration'
            } else if (eachChildPomPath.contains(Constants.Child.CHILD_NAME_OSGI)) {
                log.info("Skipping " + this.pomFilePath.replace(Constants.POM_NAME, "") + eachChildPomPath + ". OSGI test module");
                continue;
            }
            childPomList.add(new ChildPom(this.pomFilePath.replace(Constants.POM_NAME,
                    "") + eachChildPomPath));
        }
        return childPomList;
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
     *
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold  Line coverage threshold to break the build
     * @return An Array List of objects in the order of,
     * Jacoco inserted pom file as an org.w3c.Document object
     * Maven surefire argument line String in the processed document,
     * Jacoco report path String in the processed document
     * @throws PomFileReadException  Error while reading the pom
     * @throws PomFileWriteException Error while writing the pom file
     */
    public HashMap<String, Object> enforceCoverageCheckUnderBuildPlugins(String coveragePerElement, String coverageThreshold)
            throws PomFileReadException, PomFileWriteException {

        Document pomFile = DocumentReader.readDocument(pomFilePath);
        pomFile.setDocumentURI(pomFilePath);
        HashMap<String, Object> processedData = JacocoCoverage.insertJacocoCoverageCheck(
                pomFile,
                Constants.Maven.MAVEN_TAG_BUILD,
                coveragePerElement,
                coverageThreshold);
        Document jacocoInsertedPom = (Document) processedData.get(Constants.Jacoco.JACOCO_INSERTED_POM);
        DocumentWriter.writeDocument(jacocoInsertedPom, pomFilePath);
        return processedData;
    }

    /**
     * Get pom file path relevant to an instance
     *
     * @return File path of the relevant pom file
     */
    public String getPomFilePath() {

        return this.pomFilePath;
    }
}
