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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.wso2.testcoverageenforcer.GitHubHandler.Jacoco.CoverageCheckEnforcer;
import org.wso2.testcoverageenforcer.ServerHandler.SQLServer;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
        /*
        Automatic coverage threshold calculation status for when user did not input coverage threshold.
        Then current available coverage ratio will be set.
         */
        boolean automaticCoverageThreshold = false;
        options.addOption("h", "help", false,
                "Get help with usage");
        options.addOption("r", "repository", true,
                "GitHub repository name to add coverage check(Format: 'username/repositoryname'). If urls are" +
                        " received from an external server, this option is ignored");
        options.addOption("w", "workspacePath", true,
                "Folder to temporally clone the repository during the procedure");
        options.addOption("t", "threshold", true,
                "Line coverage threshold to break the build(float value between 0 and 1). If this parameter" +
                        " is not available then the tool will build the project to calculate line coverage and " +
                        "existing line coverage percentage will be used as the coverage threshold");
        options.addOption("e", "element", true,
                "Per which element this coverage check should be performed(BUNDLE, PACKAGE, CLASS, SOURCEFILE or METHOD)");
        options.addOption("p", "pullRequest", true,
                "Make a pull request once changes are done(boolean value: true or false)");
        options.addOption("s", "enableServer", true,
                "Get repository urls from the SQL server. Provide your credentials in the properties file");
        options.addOption("prop", "properties", true,
                "Properties file containing configurations for SQL server and GitHub account");
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help.printHelp("java -jar test-coverage-enforcer-1.0-SNAPSHOT.jar", options, true);
                System.exit(0);
            }
            /*
            If repository urls are received using an external server, ignore 'repository' option
             */
            if (!(cmd.hasOption("r")) && !Boolean.parseBoolean(cmd.getOptionValue("enableServer"))) {
                throw new MissingOptionException("Missing repository name");
            }
            /*
            If the threshold value is not present, automatic calculation of threshold value will take place. In this case
            coverage per element would be set to BUNDLE and user input is ignored.
             */
            if (!(cmd.hasOption("t"))) {
                automaticCoverageThreshold = true;
            } else if (!(cmd.hasOption("e"))) {
                throw new MissingOptionException("Missing element name");
            }
            if (!(cmd.hasOption("w"))) {
                throw new MissingOptionException("Missing workspace path");
            }
            if (!(cmd.hasOption("p"))) {
                throw new MissingOptionException("Missing pull request information");
            }
            if (!(cmd.hasOption("s"))) {
                throw new MissingOptionException("Missing server enabling information");
            }
            if (!(cmd.hasOption("prop"))) {
                throw new MissingOptionException("Missing properties file");
            }
            /*
            If an external server is available, read urls and integrate coverage check in each of them.
            Otherwise use the url provided as an argument and integrate coverage check in it.
             */
            if (Boolean.parseBoolean(cmd.getOptionValue("enableServer"))) {
                SQLServer externalServer = new SQLServer(cmd.getOptionValue("properties"));
                try {
                    String gitHubRepository;
                    while (true) {
                        gitHubRepository = externalServer.getNextRepositoryURL();
                        if (gitHubRepository == null) break;
                        log.info("Enforcing coverage on " + gitHubRepository);
                        CoverageCheckEnforcer.createPullRequestWithCoverageCheck(
                                gitHubRepository,
                                cmd.getOptionValue("properties"),
                                cmd.getOptionValue("workspacePath"),
                                automaticCoverageThreshold ? null : cmd.getOptionValue("element"),
                                automaticCoverageThreshold ? null : cmd.getOptionValue("threshold"),
                                Boolean.parseBoolean(cmd.getOptionValue("pullRequest")),
                                automaticCoverageThreshold);
                    }
                } finally {
                    externalServer.close();
                }

            } else {
                log.info("Enforcing coverage on " + cmd.getOptionValue("repository"));
                CoverageCheckEnforcer.createPullRequestWithCoverageCheck(
                        cmd.getOptionValue("repository"),
                        cmd.getOptionValue("properties"),
                        cmd.getOptionValue("workspacePath"),
                        automaticCoverageThreshold ? null : cmd.getOptionValue("element"),
                        automaticCoverageThreshold ? null : cmd.getOptionValue("threshold"),
                        Boolean.parseBoolean(cmd.getOptionValue("pullRequest")),
                        automaticCoverageThreshold);
            }

        } catch (ParseException e) {
            log.error("Arguments parsing error: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("File reading error", e);
        } catch (XmlPullParserException e) {
            log.error("Error occurred while parsing the pom file", e);
        } catch (ParserConfigurationException e) {
            log.error("Error occurred due to a serious configuration error in 'javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()'", e);
        } catch (SAXException e) {
            log.error("Error occurred while parsing source xml file and the pom file", e);
        } catch (TransformerException e) {
            log.error("Error occurred while writing back to the pom file", e);
        } catch (GitAPIException e) {
            log.error("Error occurred during git operation", e);
        } catch (InterruptedException e) {
            log.error("Project build interrupted", e);
        } catch (SQLException e) {
            log.error("Communication error with the external server", e);
        }
    }
}
