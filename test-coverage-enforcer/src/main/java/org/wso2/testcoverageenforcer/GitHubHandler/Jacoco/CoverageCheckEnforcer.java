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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.GitHubHandler.GitHubProject;
import org.wso2.testcoverageenforcer.Maven.FeatureAdder;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class CoverageCheckEnforcer {

    public static final Log log = LogFactory.getLog(Application.class);

    /**
     * Add jacoco coverage check to an existing repo and make a pull request
     */
    public static void createPullRequestWithCoverageCheck(
            String repositoryName,
            String gitHubUserName,
            String gitHubPassword,
            String gitHubEmail,
            String localWorkspacePath,
            boolean enablePR)
            throws
            IOException,
            GitAPIException,
            XmlPullParserException,
            TransformerException,
            SAXException,
            ParserConfigurationException {

        GitHubProject repo = new GitHubProject(
                repositoryName,
                gitHubUserName,
                gitHubPassword,
                gitHubEmail);
        try {
            log.info("Forking..");
            repo.gitFork();
            repo.setWorkspace(localWorkspacePath);
            log.info("Cloning..");
            repo.gitClone();
            log.info("Coverage Check addition..");
            FeatureAdder.intergrateJacocoCoverageCheck(
                    repo.getClonedPath(),
                    Constants.COVERAGE_PER_ELEMENT,
                    Constants.COVERAGE_THRESHOLD);
            log.info("Commiting..");
            repo.gitCommit(Constants.COMMIT_MESSAGE_COVERAGE_CHECK);
            log.info("Pushing..");
            repo.gitPush();
            if (enablePR) {
                log.info("Making a pull request..");
                repo.gitPullRequest();
            }
        } finally {
            log.info("Deleting forked repository on the GitHub..");
            repo.gitDeleteForked();
        }
    }
}
