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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.wso2.testcoverageenforcer.Maven.ParentMavenPom;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 *
 */
public class Application {

    private static final Log log = LogFactory.getLog(Application.class);

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("h", "help", false,
                "Get help with usage");
        options.addOption("p", "path", true,
                "Path to the project's directory containing the parent pom");
        options.addOption("t", "threshold", true,
                "Line coverage threshold to break the build(float value between 0 and 1)");
        options.addOption("e", "element", true,
                "Per which element this coverage check should be performed(BUNDLE, PACKAGE, CLASS, SOURCEFILE or METHOD)");

        CommandLineParser parser = new BasicParser();
        CommandLine line;
        String[] arguments = new String[3];
        HelpFormatter help = new HelpFormatter();

        try {
            line = parser.parse(options, args);

            if (line.hasOption("h")) {
                help.printHelp(
                        "java -jar prepare-line-check-0.1-SNAPSHOT.jar",
                        options,
                        true);
                return;
            }
            if (line.hasOption("p")) {
                arguments[0] = line.getOptionValue("p");
            } else {
                log.error("Missing path argument");
                return;
            }
            if (line.hasOption("t")) {
                arguments[1] = line.getOptionValue("t");
            } else {
                log.error("Missing threshold argument");
                return;
            }
            if (line.hasOption("e")) {
                arguments[2] = line.getOptionValue("e");
            } else {
                log.error("Missing element argument");
                return;
            }
            //---------------------------------------------------------------------

            ParentMavenPom parent = new ParentMavenPom(arguments[0]);
            if (parent.hasChildren()) {
                parent.enforceCoverageCheckUnderPluginManagement(arguments[2], arguments[1]);
                parent.inheritCoverageCheckInChildren(parent.getChildren(), arguments[2], arguments[1]);
            } else if (parent.hasTests()) {
                parent.enforceCoverageCheckUnderBuildPlugins(arguments[2], arguments[1]);
            } else if (!parent.hasTests()) {
                log.info("Tests not available in the parent. Rule addition skipped");
            }

            //----------------------------------------------------------------------
        } catch (ParseException e) {
            log.error("Arguments parsing error:");
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
