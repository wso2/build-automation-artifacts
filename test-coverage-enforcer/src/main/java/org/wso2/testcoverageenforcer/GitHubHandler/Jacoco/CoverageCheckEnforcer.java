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

package org.wso2.testcoverageenforcer.GitHubHandler.Jacoco;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.GitHubHandler.GitHubProject;
import org.wso2.testcoverageenforcer.Maven.FeatureAdder;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Contains methods to integrate jacoco coverage check with an existing GitHub project
 */
public class CoverageCheckEnforcer {

    /**
     * Log object to log each executing steps in a process
     */
    private static final Logger log = Logger.getLogger(CoverageCheckEnforcer.class);

    /**
     * Add jacoco coverage check to an existing repo and make a pull request
     *
     * @param repositoryName     Name of the GitHub repository to integrate coverage check in the form of 'userName/repositoryName'
     * @param localWorkspacePath Path to the folder to be used for temporary cloning of the repository
     * @param inputCoveragePerElement Per which element coverage checking to be performed(per BUNDLE, CLASS etc)
     * @param inputCoverageThreshold  Coverage threshold value to break the build
     * @param enablePR           Create a pull request after jacoco check rule integration in the forked repository
     * @return Successfully completed all the steps
     * @throws IOException                  Error in reading xml files or while initializing GitHub repository
     * @throws GitAPIException              Error while performing GitHub operations
     * @throws XmlPullParserException       Error while parsing xml files in the jacoco integration procedure
     * @throws TransformerException         Error while writing pom file
     * @throws SAXException                 Error while parsing pom file's input stream
     * @throws ParserConfigurationException Error while parsing the pom file
     */
    public static boolean createPullRequestWithCoverageCheck(String repositoryName, String propertiesFilePath,
                                                             String localWorkspacePath, String inputCoveragePerElement,
                                                             String inputCoverageThreshold, boolean enablePR,
                                                             boolean automaticCoverageThreshold)
            throws IOException, GitAPIException, XmlPullParserException, TransformerException, SAXException,
            ParserConfigurationException, InterruptedException {

        GitHubProject repo = new GitHubProject(repositoryName, propertiesFilePath);

        //If the repository is inactive for a time span of interest, ignore the procedure
        if (!(repo.getActiveStatusByCommit(Constants.GIT_TIME_PERIOD_OF_INTEREST))) {
            log.warn("Inactive repository for the past six months. Aborting procedure for this repository");
            return false;
        }

        log.info("--Removing any previously forked projects if exists..");
        repo.gitFork();
        repo.gitDeleteForked();

        log.info("--Forking..");
        repo.gitFork();
        repo.setWorkspace(localWorkspacePath);

        log.info("--Cloning..");
        repo.gitClone();

        log.info("--Inspecting support for Jacoco coverage check ");
        HashMap<String, Float> buildAnalysisWithCoverageCheck = FeatureAdder.inspectJacocoSupport(repo.getClonedPath());
        if (buildAnalysisWithCoverageCheck.get(Constants.UNIT_TESTS_AVAILABLE) == (Constants.STATUS_FALSE)) {
            log.warn("Skipping project due to unavailability of unit tests");
            return false;
        } else if (buildAnalysisWithCoverageCheck.get(Constants.BUILD_LOG_BUILD_SUCCESS) == (Constants.STATUS_FALSE)) {
            log.error("Build failed with coverage check. Possibly an already failing build or coverage check configuration error");
            return false;
        } else if (buildAnalysisWithCoverageCheck.get(Constants.BUILD_LOG_COVERAGE_CHECK_SUCCESS) == (Constants.STATUS_FALSE)) {
            log.warn("Coverage check integration failed in the build even though the build succeeded. Maybe this project does not have any unit tests");
            return false;
        }

        log.info("--Project is a healthy build and supports coverage check rule. Cleaning build files..");
        FeatureAdder.cleanProject(repo.getClonedPath());

        log.info("--Invoking coverage check rule..");
        String coveragePerElement = inputCoveragePerElement;
        String coverageThreshold = inputCoverageThreshold;
        if (automaticCoverageThreshold) {
            coveragePerElement = Constants.COVERAGE_PER_ELEMENT;
            float coverageThresholdValue = buildAnalysisWithCoverageCheck.get(Constants.BUILD_OUTPUT_MINIMUM_AVAILABLE_COVERAGE);
            coverageThreshold = String.format(java.util.Locale.US, "%.2f", (Math.floor(coverageThresholdValue * 100) / 100));
        }

        log.info("--Applying coverage rule with " + coverageThreshold + " threshold " + "per " + coveragePerElement + "..");
        FeatureAdder.integrateJacocoCoverageCheck(
                repo.getClonedPath(),
                coveragePerElement,
                coverageThreshold);

        log.info("--Committing..");
        repo.gitCommit(Constants.COMMIT_MESSAGE_COVERAGE_CHECK);

        log.info("--Pushing..");
        repo.gitPush();

        if (enablePR) {
            log.info("--Making a pull request..");
            repo.gitPullRequest();
        }
        return true;
    }
}
