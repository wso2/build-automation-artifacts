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
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileWriteException;
import org.wso2.testcoverageenforcer.GitHubHandler.GitHubProject;
import org.wso2.testcoverageenforcer.Maven.FeatureAdder;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
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
     * Name of the GitHub repository to integrate coverage check in the form of 'userName/repositoryName'
     */
    private String repositoryName;
    /**
     * Properties file with credentials to the GitHub account and mysql server
     */
    private String propertiesFilePath;
    /**
     * Path to the folder to be used for temporary cloning of the repository
     */
    private String localWorkspace;
    /**
     * Per which element coverage checking to be performed(per BUNDLE, CLASS etc)
     */
    private String coveragePerElement;
    /**
     * Coverage threshold value to break the build
     */
    private String coverageThreshold;
    /**
     * Create a pull request after jacoco check rule integration in the forked repository
     */
    private boolean pullRequest;
    /**
     * Build the project prior to applying coverage check to calculate suitable coverage threshold automatically
     */
    private boolean automaticCoverageThreshold;

    /**
     * Initialize required parameters with a builder object
     *
     * @param builder Builder class to initialize CoverageCheckEnforcer class
     */
    public CoverageCheckEnforcer(CoverageCheckEnforcerBuilder builder) {

        this.repositoryName = builder.repositoryName;
        this.propertiesFilePath = builder.propertiesFilePath;
        this.automaticCoverageThreshold = builder.automaticCoverageThreshold;
        this.coveragePerElement = builder.coveragePerElement;
        this.coverageThreshold = builder.coverageThreshold;
        this.localWorkspace = builder.localWorkspace;
        this.pullRequest = builder.pullRequest;
    }

    /**
     * Add jacoco coverage check to an existing repo and make a pull request
     *
     * @return Successfully completed all the steps
     * @throws IOException                  Error in reading xml files or while initializing GitHub repository
     * @throws GitAPIException              Error while performing GitHub operations
     * @throws InterruptedException Build process disturbed by another thread
     * @throws PomFileReadException Error while reading pom file in to a org.w3c.dom.Document object
     * @throws PomFileWriteException Error while writing org.w3c.dom.Document object to a pom file
     */
    public boolean createPullRequestWithCoverageCheck()
            throws GitAPIException, IOException, InterruptedException, PomFileReadException, PomFileWriteException {

        GitHubProject repo = new GitHubProject(repositoryName, propertiesFilePath);

        //If the repository is inactive for a time span of interest, ignore the procedure
        if (!(repo.getActiveStatusByCommit(Constants.Git.GIT_TIME_PERIOD_OF_INTEREST))) {
            log.warn("Inactive repository for the past six months. Aborting procedure for this repository");
            return false;
        }

        log.info("-Removing any previously forked projects if exists..");
        repo.gitFork();
        repo.gitDeleteForked();

        log.info("-Forking the project in to the GitHub account..");
        repo.gitFork();
        repo.setWorkspace(localWorkspace);

        log.info("-Cloning the forked project in to the local workspace..");
        repo.gitClone();

        log.info("-Inspecting support for Jacoco coverage check ");
        /*
        Build the project with zero coverage threshold applied. After the build, coverage check should be performed
        and the build should be successful in order to proceed for a pull request
         */
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

        log.info("-Project is a healthy build and supports coverage check rule. Cleaning build files..");
        FeatureAdder.cleanProject(repo.getClonedPath());

        log.info("-Invoking coverage check rule..");
        String coveragePerElement = this.coveragePerElement;
        String coverageThreshold = this.coverageThreshold;
        if (automaticCoverageThreshold) {
            coveragePerElement = Constants.COVERAGE_PER_ELEMENT;
            float coverageThresholdValue = buildAnalysisWithCoverageCheck.get(Constants.BUILD_OUTPUT_MINIMUM_AVAILABLE_COVERAGE);
            coverageThreshold = String.format(Locale.US, "%.2f", (Math.floor(coverageThresholdValue * 100) / 100));
        }

        log.info("-Applying coverage rule with " + coverageThreshold + " threshold " + "per " + coveragePerElement + "..");
        FeatureAdder.integrateJacocoCoverageCheck(
                repo.getClonedPath(),
                coveragePerElement,
                coverageThreshold);

        log.info("-Committing the coverage check rule addition..");
        repo.gitCommit(Constants.COMMIT_MESSAGE_COVERAGE_CHECK);

        log.info("-Pushing changes to the forked remote..");
        repo.gitPush();

        if (this.pullRequest) {
            log.info("-Making a pull request..");
            repo.gitPullRequest();
        }
        return true;
    }

    /**
     * Builder class
     */
    public static class CoverageCheckEnforcerBuilder {

        private String repositoryName;
        private String propertiesFilePath;
        private String localWorkspace;
        private String coveragePerElement;
        private String coverageThreshold;
        private boolean pullRequest;
        private boolean automaticCoverageThreshold;

        public CoverageCheckEnforcerBuilder(String repositoryName, String propertiesFilePath, String localWorkspace) {

            this.repositoryName = repositoryName;
            this.localWorkspace = localWorkspace;
            this.propertiesFilePath = propertiesFilePath;
        }

        public CoverageCheckEnforcerBuilder withPullRequest(boolean pullRequest) {

            this.pullRequest = pullRequest;
            return this;
        }

        public CoverageCheckEnforcerBuilder withAutomaticCoverageThreshold(boolean automaticCoverageThreshold) {

            this.automaticCoverageThreshold = automaticCoverageThreshold;
            return this;
        }

        public CoverageCheckEnforcerBuilder withCoveragePerElement(String coveragePerElement) {

            this.coveragePerElement = coveragePerElement;
            return this;
        }

        public CoverageCheckEnforcerBuilder withCoverageThreshold(String coverageThreshold) {

            this.coverageThreshold = coverageThreshold;
            return this;
        }

        public CoverageCheckEnforcer build() {

            return new CoverageCheckEnforcer(this);
        }
    }
}
