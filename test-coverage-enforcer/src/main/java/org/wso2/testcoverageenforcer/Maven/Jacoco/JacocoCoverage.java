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

package org.wso2.testcoverageenforcer.Maven.Jacoco;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.TemplateReader;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

/**
 * contains methods to process pom/xml files
 */
public class JacocoCoverage {

    public static final Log log = LogFactory.getLog(Application.class);

    /**
     * Given Document model and a plugins node in that Document,
     * this will either add or modify existing jacoco plugin to have the functionality of coverage check
     *
     * @param pom           Jacoco will be added to this document
     * @param pluginsParent Jacoco plugin will be added as a child node appended to this node
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold Line coverage threshold to break the build
     * @return Jacoco inserted pom file as an org.w3c.Document object
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     */
    public static Document insertJacocoCoverageCheck(Document pom,
                                                     String pluginsParent,
                                                     String coveragePerElement,
                                                     String coverageThreshold)
            throws ParserConfigurationException, IOException, SAXException {

        Node plugins = createPluginsNode(pom, pluginsParent);
        String surefireArgLine = Constants.DEFAULT_JACOCO_SUREFIRE_ARGLINE;
        String jacocoReportFilePath = Constants.DEFAULT_JACOCO_REPORT_PATH;
        // Check for jacoco plugin existence
        Element jacocoPlugin = null;
        Element mavenSurefirePlugin = (Element) plugins;
        boolean surefirePluginAvailable = false;
        NodeList pluginList = plugins.getChildNodes();
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0)
                    .getTextContent()
                    .equals(Constants.JACOCO_MAVEN_PLUGIN)) {
                jacocoPlugin = plugin;
            } else if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0)
                    .getTextContent()
                    .equals(Constants.SUREFIRE_MAVEN_PLUGIN)) {
                mavenSurefirePlugin = plugin;
                surefirePluginAvailable = true;
            }
        }
        if (jacocoPlugin == null) {
            // Get the root node of the xml file
            Node jacocoPluginTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_PLUGIN_TEMPLATE), true);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0)
                    .setTextContent(coveragePerElement);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0)
                    .setTextContent(coverageThreshold);
            plugins.appendChild(jacocoPluginTemplate);
            log.info("jacoco plugin added in under " + pluginsParent + " | " + pom.getDocumentURI());

        } else { // If jacoco plugin exists, analyze each execution for the presence of executions with goals of 'prepare-agent', 'report', 'check'
            Node executions = jacocoPlugin.getElementsByTagName(Constants.MAVEN_TAG_EXECUTIONS).item(0);
            if (executions == null) {
                executions = pom.createElement(Constants.MAVEN_TAG_EXECUTIONS);
                jacocoPlugin.appendChild(executions);
            }
            NodeList executionsList = executions.getChildNodes();
            Map nodeAnalysisReport = new HashMap();
            nodeAnalysisReport.put(Constants.JACOCO_GOAL_AGENT_INVOKE, false);
            nodeAnalysisReport.put(Constants.JACOCO_GOAL_REPORT, false);
            nodeAnalysisReport.put(Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE, false);
            for (int i = 0; i < executionsList.getLength(); i++) {
                // Skip line breakers, white spaces etc
                if (!executionsList.item(i).getNodeName().equals(Constants.MAVEN_TAG_EXECUTION)) continue;

                Element execution = (Element) executionsList.item(i);
                switch (execution.getElementsByTagName(Constants.MAVEN_TAG_GOAL).item(0).getTextContent()) {
                    case Constants.JACOCO_GOAL_AGENT_INVOKE:
                        nodeAnalysisReport.put(Constants.JACOCO_GOAL_AGENT_INVOKE, true);
                        jacocoReportFilePath = getJacocoReportPath(execution);
                        surefireArgLine = getJacocoSurefireArgLine(execution);
                        break;
                    case Constants.JACOCO_GOAL_REPORT:
                        nodeAnalysisReport.put(Constants.JACOCO_GOAL_REPORT, true);

                        break;
                    case Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE:
                        execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0)
                                .setTextContent(coveragePerElement);
                        execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0)
                                .setTextContent(coverageThreshold);
                        nodeAnalysisReport.put(Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE, true);

                        break;
                }
            }
            // Append each template Node for missing jacoco nodes
            if (!((boolean) nodeAnalysisReport.get(Constants.JACOCO_GOAL_AGENT_INVOKE))) {
                Node jacocoPrepareAgentExecutionNodeTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_PREPARE_AGENT_TEMPLATE), true);
                executions.appendChild(jacocoPrepareAgentExecutionNodeTemplate);
                log.info("Prepare-agent added in " + pluginsParent + " | " + pom.getDocumentURI());
            }
            if (!((boolean) nodeAnalysisReport.get(Constants.JACOCO_GOAL_REPORT))) {
                Node jacocoReportExecutionNodeTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_REPORT_TEMPLATE), true);
                executions.appendChild(jacocoReportExecutionNodeTemplate);
                log.info("Report added in " + pluginsParent + " | " + pom.getDocumentURI());
            }
            if (!((boolean) nodeAnalysisReport.get(Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE))) {
                Node jacocoCheckExecutionNodeTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_CHECK_TEMPLATE), true);
                Element jacocoCheckExecutionElementTemplate = (Element) jacocoCheckExecutionNodeTemplate;
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0)
                        .setTextContent(coveragePerElement);
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0)
                        .setTextContent(coverageThreshold);
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.JACOCO_TAG_REPORT_READ).item(0)
                        .setTextContent(jacocoReportFilePath);
                executions.appendChild(jacocoCheckExecutionNodeTemplate);
                log.info("Check added in " + pluginsParent + " | " + pom.getDocumentURI());
            }
        }

        setSurefireArgumentLineForJacoco(pom, surefirePluginAvailable, mavenSurefirePlugin, surefireArgLine);
        return pom;
    }

    /**
     * Given Document model and a plugins node in that Document, this will add jacoco plugin inheritance to the model
     *
     * @param pom Pom file as an org.w3c.Document object to inherit jacoco
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold Line coverage threshold to break the build
     * @return Jacoco inherited pom file as an org.w3c.Document object
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     */
    public static Document inheritCoverageCheckFromParent(Document pom,
                                                          String coveragePerElement,
                                                          String coverageThreshold)
            throws ParserConfigurationException, IOException, SAXException {
        //log.info("inherit for " + pom.getDocumentURI());
        Node plugins = createPluginsNode(pom, Constants.MAVEN_TAG_BUILD);
        // Check for jacoco plugin existence
        Element inheritedJacocoPlugin = null;
        Node inheritedSurefirePlugin = plugins;
        boolean surefirePluginAvailable = false;
        NodeList pluginList = plugins.getChildNodes();
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            // Every plugin has an 'artifactId'
            if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0).getTextContent().equals(Constants.JACOCO_MAVEN_PLUGIN)) {
                inheritedJacocoPlugin = plugin;
            } else if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0).getTextContent().equals(Constants.SUREFIRE_MAVEN_PLUGIN)) {
                surefirePluginAvailable = true;
                inheritedSurefirePlugin = plugin;
            }
        }

        boolean jacocoReportFilePathPresent = false;
        boolean jacocoSurefireArgLinePresent = false;
        String jacocoReportFilePath = null;
        String jacocoSurefireArgLine = null;
        Element jacocoCheckElement = null;
        if (inheritedJacocoPlugin == null) {
            // Get the root node of the xml file
            Node inheritedJacocoPluginTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_INHERITED_PLUGIN_TEMPLATE), true);
            plugins.appendChild(inheritedJacocoPluginTemplate);
            log.info("Inherited in " + "Build" + " | " + pom.getDocumentURI());
        } else {
            // If jacoco plugin and executions exists, update coverage threshold and coverage per element
            // If inheritance is already present, ignore enforcing inheritance of coverage check
            Node executions = inheritedJacocoPlugin.getElementsByTagName(Constants.MAVEN_TAG_EXECUTIONS).item(0);
            NodeList executionsList;
            if (executions != null) {
                executionsList = executions.getChildNodes();
                for (int i = 0; i < executionsList.getLength(); i++) {
                    // Skip line breakers, white spaces etc
                    if (!executionsList.item(i).getNodeName().equals(Constants.MAVEN_TAG_EXECUTION)) continue;

                    Element execution = (Element) executionsList.item(i);
                    switch (execution.getElementsByTagName(Constants.MAVEN_TAG_GOAL).item(0).getTextContent()) {
                        case Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE:
                            jacocoCheckElement = execution;
                            execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0).setTextContent(coveragePerElement);
                            execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0).setTextContent(coverageThreshold);
                            log.info("Coverage Thresholds changed");
                            break;
                        case Constants.JACOCO_GOAL_AGENT_INVOKE:
                            NodeList dataFiles = execution.getElementsByTagName(Constants.JACOCO_DESTFILE);
                            jacocoReportFilePathPresent = true;
                            if (dataFiles.getLength() > 0) {
                                jacocoReportFilePath = dataFiles.item(0).getTextContent();
                            } else {
                                jacocoReportFilePath = Constants.DEFAULT_JACOCO_REPORT_PATH;
                            }
                            NodeList surefireArguments = execution.getElementsByTagName(Constants.JACOCO_TAG_SUREFIRE_ARGLINE_NAME);
                            jacocoSurefireArgLinePresent = true;
                            if (surefireArguments.getLength() > 0) {
                                jacocoSurefireArgLine = surefireArguments.item(0).getTextContent();
                            } else {
                                jacocoSurefireArgLine = Constants.DEFAULT_JACOCO_SUREFIRE_ARGLINE;
                            }
                            break;
                    }
                }
                // Jacoco Check rule is not present but locally changed report generation path is detected. Then
                // change jacoco report path in the inherited check rule from the parent
                if ((jacocoCheckElement == null) && (jacocoReportFilePathPresent)) {
                    Node jacocoCheckInheritance = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_CHECK_INHERIT_TEMPLATE), true);
                    Element jacocoCheckInheritanceTemplate = (Element) jacocoCheckInheritance;
                    jacocoCheckInheritanceTemplate.getElementsByTagName(Constants.JACOCO_TAG_REPORT_READ).item(0).setTextContent(jacocoReportFilePath);
                    executions.appendChild(jacocoCheckInheritance);
                }
            }
        }

        inheritSurefireArgumentLineForJacoco(
                pom,
                surefirePluginAvailable,
                inheritedSurefirePlugin,
                jacocoSurefireArgLinePresent,
                jacocoSurefireArgLine);

        return pom;
    }

    /**
     * Create <plugins> node under a given parent node name if not already exists
     * (either <build><plugins> or <build><pluginManagement><plugins>)
     * Parent node would be created under the root node of the pom if not exists.
     * @param xml Pom file as an org.w3c.Document object
     * @param parentNodeName Under which parent this plugins node should be created
     * @return 'plugins' node in a pom file
     */
    private static Node createPluginsNode(Document xml, String parentNodeName) {
        // Create build node
        Node root = xml.getDocumentElement();
        NodeList rootChildren = root.getChildNodes();
        boolean parentExists = false;
        Node parent = null;
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNodeName().equals(Constants.MAVEN_TAG_BUILD)) {
                parentExists = true;
                parent = rootChildren.item(i);
                break;
            }
        }
        if (!parentExists) {
            Node parentNode = xml.createElement(Constants.MAVEN_TAG_BUILD);
            root.appendChild(parentNode);
            parent = parentNode;
        }

        // create pluginManagement under build node if required
        if (parentNodeName.equals(Constants.MAVEN_TAG_PLUGIN_MANAGEMENT)) {
            NodeList parentChildren = parent.getChildNodes();
            boolean pluginManagementExists = false;
            for (int i = 0; i < parentChildren.getLength(); i++) {
                if (parentChildren.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGIN_MANAGEMENT)) {
                    pluginManagementExists = true;
                    parent = parentChildren.item(i);
                    break;
                }
            }
            if (!pluginManagementExists) {
                Node pluginManagement = xml.createElement(Constants.MAVEN_TAG_PLUGIN_MANAGEMENT);
                parent.appendChild(pluginManagement);
                parent = pluginManagement;
            }
        }

        // Create plugins node
        NodeList parentChildren = parent.getChildNodes();
        boolean pluginsExists = false;
        Node plugins = null;
        for (int i = 0; i < parentChildren.getLength(); i++) {
            if (parentChildren.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGINS)) {
                pluginsExists = true;
                plugins = parentChildren.item(i);
                break;
            }
        }
        if (!pluginsExists) {
            plugins = xml.createElement(Constants.MAVEN_TAG_PLUGINS);
            parent.appendChild(plugins);
        }
        return plugins;
    }

    /**
     * find for any custom jacoco report path and return it
     *
     * @param execution Under which execution element this jacoco path should be searched for
     * @return Jacoco report file path
     */
    private static String getJacocoReportPath(Element execution) {

        NodeList destFileNodeList = execution.getElementsByTagName(Constants.JACOCO_DESTFILE);
        if (destFileNodeList.getLength() > 0) {
            return destFileNodeList.item(0).getTextContent();
        } else {
            return Constants.DEFAULT_JACOCO_REPORT_PATH;
        }
    }

    /**
     * Search and return the jacoco argument line for surefire plugin if exists. Return the default if not.
     *
     * @param execution Under which execution element this surefire argument line should be searched for
     * @return surefire argument line
     */
    private static String getJacocoSurefireArgLine(Element execution) {

        NodeList destFileNodeList = execution.getElementsByTagName(Constants.JACOCO_TAG_SUREFIRE_ARGLINE_NAME);
        if (destFileNodeList.getLength() > 0) {
            return destFileNodeList.item(0).getTextContent();
        } else {
            return Constants.DEFAULT_JACOCO_SUREFIRE_ARGLINE;
        }
    }

    /**
     * Configure maven surefire plugin to include jacoco agent argument line
     *
     * @param pom Pom file model to set surefire argument line
     * @param surefirePluginAvailable Surefire plugin definition is present in the pom
     * @param surefirePlugin     Maven Surefire plugin element if exists. Otherwise this should equal to maven plugins
     *                           element
     * @param jacocoArgumentLine Name of the argument line set by jacoco plugin for Maven Surefire plugin
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     */
    private static void setSurefireArgumentLineForJacoco(Document pom,
                                                         boolean surefirePluginAvailable,
                                                         Element surefirePlugin,
                                                         String jacocoArgumentLine)
            throws ParserConfigurationException, IOException, SAXException {

        // If plugin is not present, surefirePlugin is equal to 'plugins' node in the pom file. Hence append extracted surefire template to it
        if (!surefirePluginAvailable) {
            Node surefirePluginTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_SUREFIRE_PLUGIN_TEMPLATE), true);
            surefirePlugin.appendChild(surefirePluginTemplate);
            log.info("Appending Maven Surefire plugin");
        } else {
            // Multiple configuration nodes could be present. Scan each one of them and add jacoco argument line if not configured
            NodeList configurationNodes = surefirePlugin.getElementsByTagName(Constants.MAVEN_TAG_CONFIGURATION);

            for (int i = 0; i < configurationNodes.getLength(); i++) {
                // If jacoco argument line is already set, skip the procedure
                if (searchForPreConfiguredJacocoArgumentLine((Element) configurationNodes.item(i), jacocoArgumentLine)) {
                    return;
                }
            }
            // Add new argument line
            Node configurationNode;
            if (configurationNodes.getLength() == 0) {
                configurationNode = pom.createElement(Constants.MAVEN_TAG_CONFIGURATION);
            } else {
                configurationNode = configurationNodes.item(0);
            }
            Node jacocoArgumentLineForSurefire = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_SUREFIRE_ARGLINE_TEMPLATE), true);
            configurationNode.appendChild(jacocoArgumentLineForSurefire);
            log.info("Modified argument line in the existing Maven Surefire plugin");
        }
    }

    /**
     * Inherit surefire plugin in to the child pom from the parent. If jacoco coverage is defined locally, inheritance
     * will adjust surefire for the local jacoco coverage definition
     *
     * @param pom Pom file model to inherit surefire argument line
     * @param surefirePluginAvailable surefire plugin definition is present in the pom file
     * @param surefirePlugin Maven Surefire plugin element if exists. Otherwise this should equal to maven plugins
     *                       element
     * @param jacocoSurefireArgLinePresent Jacoco prepare-agent execution definition is available
     * @param jacocoArgumentLine Argument to be added
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException Error reading the pom file
     * @throws SAXException Error while parsing the pom's file input stream
     */
    private static void inheritSurefireArgumentLineForJacoco(Document pom,
                                                             boolean surefirePluginAvailable,
                                                             Node surefirePlugin,
                                                             boolean jacocoSurefireArgLinePresent,
                                                             String jacocoArgumentLine)
            throws ParserConfigurationException, IOException, SAXException {

        if (!surefirePluginAvailable) { //maven surefire plugin is not defined in the child
            if (!jacocoSurefireArgLinePresent) { //Jacoco prepare-agent execution in not present
                Node simplyInheritSurefire = pom.importNode(TemplateReader.extractTemplate(Constants.SUREFIRE_SIMPLE_INHERIT_TEMPLATE), true);
                surefirePlugin.appendChild(simplyInheritSurefire);
            } else { // Replace 'argLine' in surefire inheritance with local value
                Node InheritAndAugmentSurefire = pom.importNode(TemplateReader.extractTemplate(Constants.SUREFIRE_INHERIT_WITH_ARGLINE_TEMPLATE), true);
                Element InheritAndAugmentSurefireElement = (Element) InheritAndAugmentSurefire;
                InheritAndAugmentSurefireElement
                        .getElementsByTagName(Constants.SUREFIRE_TAG_ARGLINE)
                        .item(0)
                        .setTextContent(getArgument(jacocoArgumentLine));
                surefirePlugin.appendChild(InheritAndAugmentSurefireElement);
            }
            log.info("Inherited Maven Surefire plugin from the parent ");
        } else { //Maven surefire plugin definition is available
            if (jacocoSurefireArgLinePresent) { //jacoco prepare-agent execution is available
                Element surefirePluginElement = (Element) surefirePlugin;
                NodeList argLines = surefirePluginElement.getElementsByTagName(Constants.SUREFIRE_TAG_ARGLINE);
                for (int i = 0; i < argLines.getLength(); i++) {
                    if (argLines.item(i).getTextContent().contains(getArgument(jacocoArgumentLine))) {
                        return;
                    }
                }
                NodeList configurations = surefirePluginElement.getElementsByTagName(Constants.MAVEN_TAG_CONFIGURATION);
                Node configuration;
                if (configurations.getLength() == 0) {
                    configuration = pom.createElement(Constants.MAVEN_TAG_CONFIGURATION);
                } else {
                    configuration = configurations.item(0);
                }
                Node argLine = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_SUREFIRE_ARGLINE_TEMPLATE), true);
                argLine.setTextContent(argLine.getTextContent().replace(getArgument(Constants.DEFAULT_JACOCO_SUREFIRE_PROPERTY_NAME), getArgument(jacocoArgumentLine)));
                configuration.appendChild(argLine);
                log.info("Modified Maven Surefire argument line in the child");
            }
        }
    }

    /**
     * Check whether this pom file has already configured maven surefire plugin argument line for jacoco
     *
     * @param configuration Configuration node to search jacoco argument line
     * @param jacocoArgumentLine Argument which should be present in the surefire definition if the pom is already configured
     * @return Whether the pom file is pre-configured or not
     */
    private static boolean searchForPreConfiguredJacocoArgumentLine(Element configuration, String jacocoArgumentLine) {

        NodeList arguments = configuration.getElementsByTagName(Constants.SUREFIRE_TAG_ARGLINE);
        for (int i = 0; i < arguments.getLength(); i++) {
            if (arguments.item(i).getTextContent().contains(getArgument(jacocoArgumentLine))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a property name, return formatted syntax which gives the property value
     *
     * @param property Name of the maven property
     * @return Dollar and parenthesis signs added maven property
     */
    private static String getArgument(String property) {

        return "${" + property + "}";
    }
}
