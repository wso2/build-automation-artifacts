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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.wso2.testcoverageenforcer.GitHubHandler.Jacoco.CoverageCheckEnforcer;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Provide application interface to create pull requests for multiple github projects to add jacoco coverage check
 */
public class Application {

    public static final Log log = LogFactory.getLog(Application.class);

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(
                "h",
                "help",
                false,
                "Get help with usage");
        options.addOption(
                "r",
                "repository",
                true,
                "GitHub repository name to add coverage check(Format: 'username/repositoryname')");
        options.addOption(
                "w",
                "workspacePath",
                true,
                "Folder to temporally clone the repository during the procedure");
        options.addOption(
                "t",
                "threshold",
                true,
                "Line coverage threshold to break the build(float value between 0 and 1)");
        options.addOption(
                "e",
                "element",
                true,
                "Per which element this coverage check should be performed(BUNDLE, PACKAGE, CLASS, SOURCEFILE or METHOD)");
        options.addOption(
                "p",
                "pullRequest",
                true,
                "Make a pull request once changes are done(boolean value: true or false)");
        CommandLineParser parser = new BasicParser();
        CommandLine line;
        String[] arguments = new String[5];
        HelpFormatter help = new HelpFormatter();

        try {
            line = parser.parse(options, args);

            if (line.hasOption("h")) {
                help.printHelp("java -jar test-coverage-enforcer-1.0-SNAPSHOT.jar", options, true);
                return;
            }
            if (line.hasOption("r")) {
                arguments[0] = line.getOptionValue("r");
            } else {
                throw new MissingOptionException("Missing repository name");
            }
            if (line.hasOption("t")) {
                arguments[1] = line.getOptionValue("t");
            } else {
                throw new MissingOptionException("Missing threshold value");
            }
            if (line.hasOption("e")) {
                arguments[2] = line.getOptionValue("e");
            } else {
                throw new MissingOptionException("Missing element name");
            }
            if (line.hasOption("w")) {
                arguments[3] = line.getOptionValue("w");
            } else {
                throw new MissingOptionException("Missing workspace path");
            }
            if (line.hasOption("p")) {
                arguments[4] = line.getOptionValue("p");
            }
            else {
                throw new MissingOptionException("Missing pull request information");
            }
            log.info("Enforcing coverage on " + arguments[0]);
            CoverageCheckEnforcer.createPullRequestWithCoverageCheck(
                    arguments[0],
                    Constants.GIT_USERNAME,
                    Constants.GIT_PASSWORD,
                    Constants.GIT_EMAIL,
                    arguments[3],
                    Boolean.parseBoolean(arguments[4]));
        } catch (ParseException e) {
            log.error("Arguments parsing error: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Cannot read the parent POM", e);
        } catch (XmlPullParserException e) {
            log.error("Error occurred while parsing the pom file", e);
        } catch (ParserConfigurationException e) {
            log.error("Error occurred due to a serious configuration error in 'javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()'", e);
        } catch (SAXException e) {
            log.error("Error occurred while parsing source xml file and the pom file", e);
        } catch (TransformerException e) {
            log.error("Error occurred while writing back to the pom file", e);
        }
    }
}
