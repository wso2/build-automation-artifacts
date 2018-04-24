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

package org.wso2.testcoverageenforcer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.PomFileWriteException;
import org.wso2.testcoverageenforcer.GitHubHandler.Jacoco.CoverageCheckEnforcer;
import org.wso2.testcoverageenforcer.ServerHandler.SQLServer;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Provide application interface to create pull requests for multiple github projects to add jacoco coverage check
 */
public class Application {

    private static final Logger log = Logger.getLogger(Application.class);

    public static void main(String[] args) {

        Options options = new Options();
        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        HelpFormatter help = new HelpFormatter();

        Option cliHelp = new Option("h", "help", false, "Get help with usage");
        Option repositoryName = new Option("r", "repository", true,
                "GitHub repository name to add coverage check(Format: 'username/repositoryName'). If urls are" +
                        " received from an external server, this option is ignored");
        Option workspacePath = new Option("w", "workspacePath", true,
                "Folder to temporally clone the repository during the procedure");
        Option thresholdValue = new Option("t", "threshold", true,
                "Line coverage threshold to break the build(float value between 0 and 1). If this parameter" +
                        " is not available then the tool will build the project to calculate line coverage and " +
                        "existing line coverage percentage will be used as the coverage threshold");
        Option element = new Option("e", "element", true,
                "Per which element this coverage check should be performed(BUNDLE, PACKAGE, CLASS, SOURCEFILE or METHOD)");
        Option pullRequest = new Option("p", "pullRequest", true,
                "Make a pull request once changes are done(boolean value: true or false)");
        Option propertiesFile = new Option("prop", "properties", true,
                "Properties file containing configurations for SQL server and GitHub account");
        workspacePath.setRequired(true);
        propertiesFile.setRequired(true);
        options.addOption(cliHelp);
        options.addOption(repositoryName);
        options.addOption(workspacePath);
        options.addOption(thresholdValue);
        options.addOption(element);
        options.addOption(pullRequest);
        options.addOption(propertiesFile);

        try {
            cmd = parser.parse(options, args);
            /*
            Automatic coverage threshold calculation status for when user did not input coverage threshold.
            Then current available coverage ratio will be set.
            */
            boolean automaticCoverageThresholdStatus = false;
            boolean pullRequestStatus = true;

            if (cmd.hasOption("h")) {
                help.printHelp("java -jar test-coverage-enforcer-1.0-SNAPSHOT.jar", options, true);
                System.exit(0);
            }
            /*
            If the threshold value and coverage per element value is not present, automatic calculation of threshold value will take place.
            In this case coverage per element would be set to BUNDLE.
             */
            if (!(cmd.hasOption("t")) && !(cmd.hasOption("e"))) {
                automaticCoverageThresholdStatus = true;
            }
            /*
            Pull request making after coverage threshold integration default value is set to true otherwise user
            set it to zero. In this case, only the forked repository will be get updated and no pull requests made
             */
            if (cmd.hasOption("p")) {
                pullRequestStatus = Boolean.parseBoolean(cmd.getOptionValue("p"));
            }
            /*
            If a repository url is given, apply coverage check for that repository. Otherwise request to an external mysql server
            to read repositories from a sql table and apply coverage check in each of them
             */
            if (!cmd.hasOption("r")) {
                log.info("---Read repositories from an external mysql server---");
                SQLServer externalServer = new SQLServer(cmd.getOptionValue("properties"));
                try {
                    String gitHubRepository;
                    while ((gitHubRepository = externalServer.getNextRepositoryURL()) != null) {
                        /*
                        If the coverage check integration failed due to Git or GitHub exception, log the message
                        and skip current repository
                         */
                        try {
                            log.info("Enforcing coverage on " + gitHubRepository);
                            CoverageCheckEnforcer checkEnforcer = new CoverageCheckEnforcer
                                    .CoverageCheckEnforcerBuilder(gitHubRepository, cmd.getOptionValue("properties"), cmd.getOptionValue("workspacePath"))
                                    .withPullRequest(pullRequestStatus)
                                    .withCoveragePerElement(automaticCoverageThresholdStatus ? null : cmd.getOptionValue("element"))
                                    .withCoverageThreshold(automaticCoverageThresholdStatus ? null : cmd.getOptionValue("threshold"))
                                    .withAutomaticCoverageThreshold(automaticCoverageThresholdStatus)
                                    .build();
                            checkEnforcer.createPullRequestWithCoverageCheck();

                        } catch (GitAPIException e) {
                            log.warn("Error while performing git operations. Skipping job", e);
                        } catch (IOException e) {
                            log.warn("Error occurred possibly due to a GitHub operation or local file IO. Skipping Job", e);
                        }
                    }
                } finally {
                    externalServer.close();
                }

            } else {
                log.info("---Enforcing coverage on " + cmd.getOptionValue("repository") + "---");
                CoverageCheckEnforcer checkEnforcer = new CoverageCheckEnforcer
                        .CoverageCheckEnforcerBuilder(cmd.getOptionValue("repository"), cmd.getOptionValue("properties"), cmd.getOptionValue("workspacePath"))
                        .withPullRequest(pullRequestStatus)
                        .withCoveragePerElement(automaticCoverageThresholdStatus ? null : cmd.getOptionValue("element"))
                        .withCoverageThreshold(automaticCoverageThresholdStatus ? null : cmd.getOptionValue("threshold"))
                        .withAutomaticCoverageThreshold(automaticCoverageThresholdStatus)
                        .build();
                checkEnforcer.createPullRequestWithCoverageCheck();
            }
        } catch (MissingOptionException e) {
            log.error(e);
            help.printHelp("java -jar test-coverage-enforcer-1.0-SNAPSHOT.jar", options, true);
        } catch (ParseException e) {
            log.error("Arguments parsing error: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Input or output operation error", e);
        } catch (PomFileReadException e) {
            log.error("Error while reading a pom file", e);
        } catch (PomFileWriteException e) {
            log.error("Error while writing a pom file", e);
        } catch (GitAPIException e) {
            log.error("Error occurred during git operation", e);
        } catch (InterruptedException e) {
            log.error("Project build interrupted", e);
        } catch (SQLException e) {
            log.error("Communication error with the external server", e);
        }
    }
}
