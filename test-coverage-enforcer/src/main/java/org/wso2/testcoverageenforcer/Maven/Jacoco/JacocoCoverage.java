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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.PomFileReadException;
import org.wso2.testcoverageenforcer.FileHandler.TemplateReader;

import java.util.HashMap;
import java.util.Map;

/**
 * contains methods to process pom/xml files
 */
public class JacocoCoverage {

    private static final Logger log = Logger.getLogger(Application.class);

    /**
     * Given Document model and the 'plugins' node in that Document(either under <build> or <build><pluginManagement>),
     * this will either add or modify existing jacoco plugin to have the functionality of coverage check
     *
     * @param pom                Jacoco will be added to this document
     * @param pluginsParent      Jacoco plugin will be added as a child node appended to this node
     * @param coveragePerElement Per which element jacoco coverage check should be performed
     * @param coverageThreshold  Line coverage threshold to break the build
     * @return A HashMap of objects in the order of,
     * Jacoco inserted pom file as an org.w3c.Document object
     * Maven surefire argument line String in the processed document,
     * Jacoco report path String in the processed document
     * @throws PomFileReadException Error while reading the pom
     */
    public static HashMap<String, Object> insertJacocoCoverageCheck(Document pom,
                                                                    String pluginsParent,
                                                                    String coveragePerElement,
                                                                    String coverageThreshold)
            throws PomFileReadException {


        /*To return multiple objects*/
        HashMap<String, Object> output = new HashMap<>();

        /*<plugins> node might not be present in some cases. Uses existing or create otherwise*/
        Node plugins = createPluginsNode(pom, pluginsParent);
        /*
        Custom defined surefire argument name and jacoco report path could be present in the child. If not defaults
        are used. Hence defaults are initialized in the beginning and values are updated if any custom definitions
        are found later in the document
         */
        String surefireArgLine = Constants.Default.DEFAULT_JACOCO_SUREFIRE_ARGLINE;
        String jacocoReportFilePath = Constants.Default.DEFAULT_JACOCO_REPORT_PATH;
        Element jacocoPlugin = null;
        /*Initialized with root element for Maven Surefire plugin. Because it needs to be added if it is not available*/
        Element mavenSurefirePlugin = (Element) plugins;
        boolean surefirePluginAvailable = false;
        NodeList pluginList = plugins.getChildNodes();
        /*Check for jacoco and Maven surefire plugin existence*/
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            if (plugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_ARTIFACT_ID).item(0)
                    .getTextContent()
                    .equals(Constants.Jacoco.JACOCO_MAVEN_PLUGIN)) {
                jacocoPlugin = plugin;
            } else if (plugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_ARTIFACT_ID).item(0)
                    .getTextContent()
                    .equals(Constants.Surefire.SUREFIRE_MAVEN_PLUGIN)) {
                mavenSurefirePlugin = plugin;
                surefirePluginAvailable = true;
            }
        }
        if (jacocoPlugin == null) {
            // Get the root node of the xml file
            Node jacocoPluginTemplate =
                    pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_PLUGIN_TEMPLATE), true);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.Jacoco.JACOCO_TAG_COVERAGE_PER_ELEMENT)
                    .item(0).setTextContent(coveragePerElement);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.Jacoco.JACOCO_TAG_COVERAGE_CHECK_VALUE)
                    .item(0).setTextContent(coverageThreshold);
            plugins.appendChild(jacocoPluginTemplate);
            if (log.isDebugEnabled()) {
                log.debug("Full Jacoco plugin template added in under " + pluginsParent + " for " + pom.getDocumentURI());
            }
        } else { // If jacoco plugin exists, analyze each execution for the presence of executions with goals of
            // 'prepare-agent', 'report', 'check'
            Node executions = jacocoPlugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_EXECUTIONS).item(0);
            if (executions == null) {
                executions = pom.createElement(Constants.Maven.MAVEN_TAG_EXECUTIONS);
                jacocoPlugin.appendChild(executions);
            }
            NodeList executionsList = executions.getChildNodes();
            Map<String, Boolean> nodeAnalysisReport = new HashMap<>();
            nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE, false);
            nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_REPORT, false);
            nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE, false);
            for (int i = 0; i < executionsList.getLength(); i++) {
                // Skip line breakers, white spaces etc
                if (!executionsList.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_EXECUTION)) continue;

                Element execution = (Element) executionsList.item(i);
                switch (execution.getElementsByTagName(Constants.Maven.MAVEN_TAG_GOAL).item(0)
                        .getTextContent()) {
                    case Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE:
                        nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE, true);
                        jacocoReportFilePath = getJacocoReportPath(execution);
                        surefireArgLine = getJacocoSurefireArgLine(execution);
                        break;
                    case Constants.Jacoco.JACOCO_GOAL_REPORT:
                        nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_REPORT, true);
                        break;
                    case Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE:
                        /*
                        Update threshold and coverage per element if the execution is already exists
                         */
                        createNode(pom, execution, Constants.Jacoco.JACOCO_POM_PATH_ELEMENT)
                                .setTextContent(coveragePerElement);
                        createNode(pom, execution, Constants.Jacoco.JACOCO_POM_PATH_MINIMUM)
                                .setTextContent(coverageThreshold);
                        createNode(pom, execution, Constants.Jacoco.JACOCO_POM_PATH_DATA_FILE)
                                .setTextContent(jacocoReportFilePath);
                        if (log.isDebugEnabled()) {
                            log.debug("Coverage element, coverage threshold and data file path parameters are updated" +
                                    " for " + pom.getDocumentURI());
                        }
                        nodeAnalysisReport.put(Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE, true);
                        break;
                }
            }
            // Append each template Node for missing jacoco nodes
            if (!(nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE))) {
                Node jacocoPrepareAgentExecutionNodeTemplate =
                        pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_PREPARE_AGENT_TEMPLATE)
                                , true);
                executions.appendChild(jacocoPrepareAgentExecutionNodeTemplate);
                if (log.isDebugEnabled()) {
                    log.debug("Jacoco prepare-agent execution template added in under " + pluginsParent + " for "
                            + pom.getDocumentURI());
                }
            }
            if (!(nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_REPORT))) {
                Node jacocoReportExecutionNodeTemplate =
                        pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_REPORT_TEMPLATE),
                                true);
                Element jacocoReportExecutionNodeElement = (Element) jacocoReportExecutionNodeTemplate;
                jacocoReportExecutionNodeElement.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_REPORT_READ)
                        .item(0).setTextContent(jacocoReportFilePath);
                executions.appendChild(jacocoReportExecutionNodeTemplate);
                if (log.isDebugEnabled()) {
                    log.debug("Jacoco report execution template added in under " + pluginsParent + " for "
                            + pom.getDocumentURI());
                }
            }
            if (!(nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE))) {
                Node jacocoCheckExecutionNodeTemplate =
                        pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_CHECK_TEMPLATE),
                                true);
                Element jacocoCheckExecutionElementTemplate = (Element) jacocoCheckExecutionNodeTemplate;
                jacocoCheckExecutionElementTemplate.getElementsByTagName(
                        Constants.Jacoco.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0)
                        .setTextContent(coveragePerElement);
                jacocoCheckExecutionElementTemplate.getElementsByTagName(
                        Constants.Jacoco.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0)
                        .setTextContent(coverageThreshold);
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_REPORT_READ)
                        .item(0).setTextContent(jacocoReportFilePath);
                executions.appendChild(jacocoCheckExecutionNodeTemplate);
                if (log.isDebugEnabled()) {
                    log.debug("Jacoco check execution template added in under " + pluginsParent + " for "
                            + pom.getDocumentURI());
                }
            }
            if (log.isDebugEnabled() &&
                    nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE) &&
                    nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_REPORT) &&
                    nodeAnalysisReport.get(Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE)) {
                log.debug("Jacoco Plugin already available");
            }
        }

        setSurefireArgumentLineForJacoco(pom, surefirePluginAvailable, mavenSurefirePlugin, surefireArgLine);
        output.put(Constants.Jacoco.JACOCO_INSERTED_POM, pom);
        output.put(Constants.Surefire.SUREFIRE_ARGLINE_IN_THE_POM, surefireArgLine);
        output.put(Constants.Jacoco.JACOCO_REPORT_PATH_IN_THE_POM, jacocoReportFilePath);
        return output;
    }

    /**
     * Given Document model and a plugins node in that Document, this will add jacoco plugin inheritance to the model
     *
     * @param pom                          Pom file as an org.w3c.Document object to inherit jacoco
     * @param coveragePerElement           Per which element jacoco coverage check should be performed
     * @param coverageThreshold            Line coverage threshold to break the build
     * @param surefireArgumentLineInParent surefire argument name in the parent pom
     * @param jacocoReportPathInParent     jacoco report file path used in the parent pom
     * @return Jacoco inherited pom file as an org.w3c.Document object
     * @throws PomFileReadException Error while reading the pom
     */
    public static Document inheritCoverageCheckFromParent(Document pom,
                                                          String coveragePerElement,
                                                          String coverageThreshold,
                                                          String surefireArgumentLineInParent,
                                                          String jacocoReportPathInParent)
            throws PomFileReadException {

        Node plugins = createPluginsNode(pom, Constants.Maven.MAVEN_TAG_BUILD);
        // Check for jacoco plugin existence
        Element inheritedJacocoPlugin = null;
        Node inheritedSurefirePlugin = plugins;
        boolean surefirePluginAvailable = false;
        NodeList pluginList = plugins.getChildNodes();
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            // Every plugin has an 'artifactId'
            if (plugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_ARTIFACT_ID).item(0).getTextContent()
                    .equals(Constants.Jacoco.JACOCO_MAVEN_PLUGIN)) {
                inheritedJacocoPlugin = plugin;
            } else if (plugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_ARTIFACT_ID).item(0)
                    .getTextContent().equals(Constants.Surefire.SUREFIRE_MAVEN_PLUGIN)) {
                surefirePluginAvailable = true;
                inheritedSurefirePlugin = plugin;
            }
        }

        boolean jacocoPrepareAgentPresent = false;
        boolean localJacocoPrepareAgentAvailable = false;
        String jacocoReportFilePath = jacocoReportPathInParent;
        String jacocoSurefireArgLine = surefireArgumentLineInParent;
        Element jacocoCheckElement = null;
        if (inheritedJacocoPlugin == null) {
            // Get the root node of the xml file
            Node inheritedJacocoPluginTemplate =
                    pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_INHERITED_PLUGIN_TEMPLATE),
                            true);
            plugins.appendChild(inheritedJacocoPluginTemplate);
            if (log.isDebugEnabled()) {
                log.debug("Jacoco plugin configuration inherited from parent in " + pom.getDocumentURI());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Jacoco plugin is already inherited in " + pom.getDocumentURI());
            }
            /*
            If jacoco plugin and executions exists, update coverage threshold and coverage per element.
            If inheritance is already present, ignore enforcing inheritance of coverage check
             */
            Node executions = inheritedJacocoPlugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_EXECUTIONS)
                    .item(0);
            NodeList executionsList;
            if (executions != null) {
                executionsList = executions.getChildNodes();
                for (int i = 0; i < executionsList.getLength(); i++) {
                    // Skip line breakers, white spaces etc
                    if (!executionsList.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_EXECUTION)) continue;

                    Element execution = (Element) executionsList.item(i);
                    switch (execution.getElementsByTagName(Constants.Maven.MAVEN_TAG_GOAL).item(0)
                            .getTextContent()) {
                        case Constants.Jacoco.JACOCO_GOAL_COVERAGE_RULE_INVOKE:
                            jacocoCheckElement = execution;
                            /*
                            If coverage check rule is present, update locally defined threshold and coverage per element
                             if available
                             */
                            NodeList elementList =
                                    execution.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_COVERAGE_PER_ELEMENT);
                            NodeList minimumList =
                                    execution.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_COVERAGE_CHECK_VALUE);
                            if (elementList.getLength() != 0) {
                                elementList.item(0).setTextContent(coveragePerElement);
                                if (log.isDebugEnabled()) {
                                    log.debug("Coverage per element changed");
                                }
                            }
                            if (minimumList.getLength() != 0) {
                                minimumList.item(0).setTextContent(coverageThreshold);
                                if (log.isDebugEnabled()) {
                                    log.debug("Coverage Threshold changed");
                                }
                            }

                            break;
                        case Constants.Jacoco.JACOCO_GOAL_AGENT_INVOKE:
                            NodeList dataFiles = execution.getElementsByTagName(Constants.Jacoco.JACOCO_DESTFILE);
                            jacocoPrepareAgentPresent = true;
                            if (dataFiles.getLength() > 0) {
                                jacocoReportFilePath = dataFiles.item(0).getTextContent();
                            } else {
                                jacocoReportFilePath = jacocoReportPathInParent;
                            }
                            NodeList surefireArguments =
                                    execution.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_SUREFIRE_ARGLINE_NAME);
                            localJacocoPrepareAgentAvailable = true;
                            if (surefireArguments.getLength() > 0) {
                                jacocoSurefireArgLine = surefireArguments.item(0).getTextContent();
                            } else {
                                jacocoSurefireArgLine = surefireArgumentLineInParent;
                            }
                            break;
                    }
                }
                /*
                 Jacoco Check rule is not present but locally changed report generation path is detected. This will make
                 child module to inherit Jacoco check rule from the parent with an incorrect path for the coverage
                 report.
                 Therefore change inheriting report path configuration locally.
                  */
                if ((jacocoCheckElement == null) && (jacocoPrepareAgentPresent)) {
                    Node jacocoCheckInheritance =
                            pom.importNode(TemplateReader.extractTemplate(
                                    Constants.Jacoco.JACOCO_CHECK_INHERIT_TEMPLATE), true);
                    Element jacocoCheckInheritanceTemplate = (Element) jacocoCheckInheritance;
                    jacocoCheckInheritanceTemplate.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_REPORT_READ)
                            .item(0).setTextContent(jacocoReportFilePath);
                    executions.appendChild(jacocoCheckInheritance);
                    if (log.isDebugEnabled()) {
                        log.debug("Report path configured for inheriting check rule");
                    }
                } else if (jacocoCheckElement != null) {
                    /*
                    If jacoco check rule is present, add report file path information for inheriting jacoco
                    prepare-agent execution
                     */
                    createNode(pom, jacocoCheckElement, Constants.Jacoco.JACOCO_POM_PATH_DATA_FILE)
                            .setTextContent(jacocoReportFilePath);
                    if (log.isDebugEnabled()) {
                        log.debug("Report path configured for existing Check rule");
                    }
                }
            }
        }

        inheritSurefireArgumentLineForJacoco(
                pom, surefirePluginAvailable, inheritedSurefirePlugin, localJacocoPrepareAgentAvailable,
                jacocoSurefireArgLine);

        return pom;
    }

    /**
     * Create <plugins> node under a given parent node name if not already exists
     * (either <build><plugins> or <build><pluginManagement><plugins>)
     * Parent node would be created under the root node of the pom if not exists.
     *
     * @param xml            Pom file as an org.w3c.Document object
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
            if (rootChildren.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_BUILD)) {
                parentExists = true;
                parent = rootChildren.item(i);
                break;
            }
        }
        if (!parentExists) {
            Node parentNode = xml.createElement(Constants.Maven.MAVEN_TAG_BUILD);
            root.appendChild(parentNode);
            parent = parentNode;
        }

        // create pluginManagement under build node if required
        if (parentNodeName.equals(Constants.Maven.MAVEN_TAG_PLUGIN_MANAGEMENT)) {
            NodeList parentChildren = parent.getChildNodes();
            boolean pluginManagementExists = false;
            for (int i = 0; i < parentChildren.getLength(); i++) {
                if (parentChildren.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_PLUGIN_MANAGEMENT)) {
                    pluginManagementExists = true;
                    parent = parentChildren.item(i);
                    break;
                }
            }
            if (!pluginManagementExists) {
                Node pluginManagement = xml.createElement(Constants.Maven.MAVEN_TAG_PLUGIN_MANAGEMENT);
                parent.appendChild(pluginManagement);
                parent = pluginManagement;
            }
        }

        // Create plugins node
        NodeList parentChildren = parent.getChildNodes();
        boolean pluginsExists = false;
        Node plugins = null;
        for (int i = 0; i < parentChildren.getLength(); i++) {
            if (parentChildren.item(i).getNodeName().equals(Constants.Maven.MAVEN_TAG_PLUGINS)) {
                pluginsExists = true;
                plugins = parentChildren.item(i);
                break;
            }
        }
        if (!pluginsExists) {
            plugins = xml.createElement(Constants.Maven.MAVEN_TAG_PLUGINS);
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

        // Search for Jacoco report file writing path under execution <configurations> tag
        NodeList destFileNodeList = execution.getElementsByTagName(Constants.Jacoco.JACOCO_DESTFILE);
        if (destFileNodeList.getLength() > 0) {
            return destFileNodeList.item(0).getTextContent();
        } else {
            /*
            If Jacoco report destination file definition is unavailable in the execution go to the jacoco plugin node
             and search under whole node for the <destFile> tag
             */
            Element jacocoPluginElement = (Element) execution.getParentNode().getParentNode();
            destFileNodeList = jacocoPluginElement.getElementsByTagName(Constants.Jacoco.JACOCO_DESTFILE);
            if (destFileNodeList.getLength() > 0) {
                return destFileNodeList.item(0).getTextContent();
            } else {
                return Constants.Default.DEFAULT_JACOCO_REPORT_PATH;
            }
        }
    }

    /**
     * Search and return the jacoco argument line for surefire plugin if exists. Return the default if not.
     *
     * @param execution Under which execution element this surefire argument line should be searched for
     * @return surefire argument line
     */
    private static String getJacocoSurefireArgLine(Element execution) {

        NodeList destFileNodeList = execution.getElementsByTagName(Constants.Jacoco.JACOCO_TAG_SUREFIRE_ARGLINE_NAME);
        if (destFileNodeList.getLength() > 0) {
            return destFileNodeList.item(0).getTextContent();
        } else {
            return Constants.Default.DEFAULT_JACOCO_SUREFIRE_ARGLINE;
        }
    }

    /**
     * Configure maven surefire plugin to include jacoco agent argument line
     *
     * @param pom                     Pom file model to set surefire argument line
     * @param surefirePluginAvailable Surefire plugin definition is present in the pom
     * @param surefirePlugin          Maven Surefire plugin element if exists. Otherwise this is equal to the maven
     *                                <plugins> element
     * @param jacocoArgumentLine      Name of the argument line set by jacoco plugin for Maven Surefire plugin
     * @throws PomFileReadException Error while reading the pom
     */
    private static void setSurefireArgumentLineForJacoco(Document pom,
                                                         boolean surefirePluginAvailable,
                                                         Element surefirePlugin,
                                                         String jacocoArgumentLine)
            throws PomFileReadException {

        // If plugin definition is not present, surefirePlugin is equal to <plugins> node in the pom file. Hence
        // append extracted surefire template to it
        if (!surefirePluginAvailable) {
            Node surefirePluginTemplate =
                    pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_SUREFIRE_PLUGIN_TEMPLATE),
                            true);

            /*Set Jacoco argument line in the appending Maven Surefire plugin template and append */
            Element surefirePluginTemplateElement = (Element) surefirePluginTemplate;
            surefirePluginTemplateElement.getElementsByTagName(Constants.Surefire.SUREFIRE_TAG_ARGLINE).item(0)
                    .setTextContent(getArgument(jacocoArgumentLine));
            surefirePlugin.appendChild(surefirePluginTemplate);
            if (log.isDebugEnabled()) {
                log.debug("Complete Maven Surefire template was added for " + pom.getDocumentURI());
            }
        } else {

            /*Multiple <configuration> nodes could be present. Scan each one of them and add jacoco argument line if
            not configured*/
            NodeList configurationNodes = surefirePlugin.getElementsByTagName(Constants.Maven.MAVEN_TAG_CONFIGURATION);

            for (int i = 0; i < configurationNodes.getLength(); i++) {
                /*
                If required jacoco argument line (either default or locally defined) is present in the current Maven
                Surefire configuration,
                skip the procedure
                 */
                if (searchForPreConfiguredJacocoArgumentLine((Element) configurationNodes.item(i),
                        jacocoArgumentLine) && log.isDebugEnabled()) {
                    log.debug("Jacoco agent argument line is already configured for Maven Surefire");
                    return;
                }
            }

            /*If the <configuration> nodes are present(or a single one)  pick any one and append
            <argLine>requiredJacocoArgumentLine</argLine>*/
            Node configurationNode;
            if (configurationNodes.getLength() == 0) {
                configurationNode = pom.createElement(Constants.Maven.MAVEN_TAG_CONFIGURATION);
            } else {
                configurationNode = configurationNodes.item(0);
            }
            Node jacocoArgumentLineForSurefire =
                    pom.importNode(TemplateReader.extractTemplate(Constants.Jacoco.JACOCO_SUREFIRE_ARGLINE_TEMPLATE),
                            true);
            Element jacocoArgumentLineForSurefireElement = (Element) jacocoArgumentLineForSurefire;
            jacocoArgumentLineForSurefireElement.setTextContent(getArgument(jacocoArgumentLine));
            configurationNode.appendChild(jacocoArgumentLineForSurefire);
            if (log.isDebugEnabled()) {
                log.debug("Jacoco argument line for Maven Surefire is configured");
            }
        }
    }

    /**
     * Inherit surefire plugin in to the child pom from the parent. If jacoco coverage is defined locally, inheritance
     * will adjust surefire for the local jacoco coverage definition
     *
     * @param pom                              Pom file model to inherit surefire argument line
     * @param surefirePluginAvailable          surefire plugin definition is present in the pom file
     * @param surefirePlugin                   Maven Surefire plugin element if exists. Otherwise this should equal
     *                                         to maven plugins
     *                                         element
     * @param localJacocoPrepareAgentAvailable Jacoco prepare-agent execution definition is available
     * @param jacocoArgumentLine               Argument to be added
     * @throws PomFileReadException Error while reading the pom
     */
    private static void inheritSurefireArgumentLineForJacoco(Document pom,
                                                             boolean surefirePluginAvailable,
                                                             Node surefirePlugin,
                                                             boolean localJacocoPrepareAgentAvailable,
                                                             String jacocoArgumentLine)
            throws PomFileReadException {

        if (!surefirePluginAvailable) { //maven surefire plugin is not defined in the child
            if (!localJacocoPrepareAgentAvailable) { //Jacoco prepare-agent execution is not present in the child
                Node simplyInheritSurefire =
                        pom.importNode(TemplateReader.extractTemplate(
                                Constants.Surefire.SUREFIRE_SIMPLE_INHERIT_TEMPLATE), true);
                surefirePlugin.appendChild(simplyInheritSurefire);
                if (log.isDebugEnabled()) {
                    log.debug("Maven Surefire plugin inherited from the parent without configuration changes");
                }
            } else { // Replace '<argLine>' in surefire inheritance the local value
                Node InheritAndAugmentSurefire =
                        pom.importNode(TemplateReader.extractTemplate(
                                Constants.Surefire.SUREFIRE_INHERIT_WITH_ARGLINE_TEMPLATE), true);
                Element InheritAndAugmentSurefireElement = (Element) InheritAndAugmentSurefire;
                InheritAndAugmentSurefireElement
                        .getElementsByTagName(Constants.Surefire.SUREFIRE_TAG_ARGLINE)
                        .item(0)
                        .setTextContent(getArgument(jacocoArgumentLine));
                surefirePlugin.appendChild(InheritAndAugmentSurefireElement);
                if (log.isDebugEnabled()) {
                    log.debug("Maven Surefire plugin inherited from the parent configured for locally defined Jacoco " +
                            "agent");
                }
            }
        } else { //Maven surefire plugin definition is available
            if (localJacocoPrepareAgentAvailable) { //jacoco prepare-agent execution is available
                Element surefirePluginElement = (Element) surefirePlugin;
                NodeList argLines = surefirePluginElement.getElementsByTagName(Constants.Surefire.SUREFIRE_TAG_ARGLINE);
                /*
                If <argLine> tag or tags present, search if the jacoco argument line is included in any of them and skip
                this procedure if available. If <argLine> tag is not present at all add required template under
                <configuration>
                */
                for (int i = 0; i < argLines.getLength(); i++) {
                    if (argLines.item(i).getTextContent().contains(getArgument(jacocoArgumentLine))) {
                        return;
                    }
                }
                NodeList configurations =
                        surefirePluginElement.getElementsByTagName(Constants.Maven.MAVEN_TAG_CONFIGURATION);
                Node configuration;

                /*Get <configuration> element. Create if not exists*/
                if (configurations.getLength() == 0) {
                    configuration = pom.createElement(Constants.Maven.MAVEN_TAG_CONFIGURATION);
                } else {
                    configuration = configurations.item(0);
                }
                Node argLine =
                        pom.importNode(TemplateReader.extractTemplate(
                                Constants.Jacoco.JACOCO_SUREFIRE_ARGLINE_TEMPLATE), true);

                /*Modifying jacoco argument line in the Surefire template*/
                argLine.setTextContent(argLine.getTextContent().replace(getArgument(
                        Constants.Default.DEFAULT_JACOCO_SUREFIRE_PROPERTY_NAME), getArgument(jacocoArgumentLine)));
                configuration.appendChild(argLine);
                if (log.isDebugEnabled()) {
                    log.debug("Modified Maven Surefire argument line for locally available Jacoco agent");
                }
            } else {
                /*
                If surefire is defined but prepare agent is not present in the child, surefire argument line should be
                the argument line defined in parent. But this locally defined surefire definition might contain a
                different
                argument line overriding what we define in the parent. Hence it should be configured accordingly if
                <argLine>
                available locally. First check this step has been already done and add otherwise.
                 */
                NodeList argLines =
                        ((Element) surefirePlugin).getElementsByTagName(Constants.Surefire.SUREFIRE_TAG_ARGLINE);
                for (int i = 0; i < argLines.getLength(); i++) {

                    /*If argument line already exists in the Surefire definition, skip inheriting*/
                    if (log.isDebugEnabled() &&
                            argLines.item(i).getTextContent().contains(getArgument(jacocoArgumentLine))) {
                        log.debug("Locally defined Surefire plugin is already configured for the inheriting Jacoco " +
                                "plugin");
                        return;
                    }
                }
                Node newArgLine = pom.createElement(Constants.Surefire.SUREFIRE_TAG_ARGLINE);
                newArgLine.setTextContent(getArgument(jacocoArgumentLine));
                createNode(pom, surefirePlugin,
                        Constants.Surefire.SUREFIRE_POM_PATH_CONFIGURATION).appendChild(newArgLine);
                if (log.isDebugEnabled()) {
                    log.debug("Local Surefire definition configured for inheriting jacoco prepare-agent");
                }
            }
        }
    }

    /**
     * Check whether this pom file has already configured maven surefire plugin argument line for jacoco
     *
     * @param configuration      Configuration node to search jacoco argument line
     * @param jacocoArgumentLine Argument which should be present in the surefire definition if the pom is already
     *                           configured
     * @return Whether the pom file is pre-configured or not
     */
    private static boolean searchForPreConfiguredJacocoArgumentLine(Element configuration, String jacocoArgumentLine) {

        NodeList arguments = configuration.getElementsByTagName(Constants.Surefire.SUREFIRE_TAG_ARGLINE);
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

    /**
     * Go through a given node tree starting from a given root. If any element does not exists, create one.
     * If multiple nodes with the same name found while traversing, first one would be chosen
     *
     * @param root        root node of the elementTree parameter
     * @param elementTree Array of node names in traversing order from top to bottom
     */
    private static Node createNode(Document xml, Node root, String[] elementTree) {

        Node parent = root;
        for (String elementName : elementTree) {
            Element parentElement = (Element) parent;
            if (parentElement.getElementsByTagName(elementName).getLength() > 0) {
                parent = parentElement.getElementsByTagName(elementName).item(0);
            } else {
                Node createdNode = xml.createElement(elementName);
                parent.appendChild(createdNode);
                parent = createdNode;
            }
        }
        return parent;
    }
}
