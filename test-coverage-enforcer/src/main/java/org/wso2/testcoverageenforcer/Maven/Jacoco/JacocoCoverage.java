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

    public static final Log log = LogFactory.getLog(JacocoCoverage.class);

    /**
     * Given Document model and a plugins node in that Document,
     * this will either add or modify existing jacoco plugin to have the functionality of coverage check
     *
     * @param pom           Jacoco will be added to this document
     * @param pluginsParent Jacoco plugin will be added as a child node appended to this node
     */
    public static Document insertJacocoCoverageCheck(Document pom,
                                                     String pluginsParent,
                                                     String coveragePerElement,
                                                     String coverageThreshold)
            throws ParserConfigurationException, IOException, SAXException {

        Node plugins = createPluginsNode(pom, pluginsParent);
        // Check for jacoco plugin existence
        Element jacocoPlugin = null;
        NodeList pluginList = plugins.getChildNodes();
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0).getTextContent().equals(Constants.JACOCO_MAVEN_PLUGIN)) {
                jacocoPlugin = plugin;
                break;
            }
        }
        if (jacocoPlugin == null) {
            // Get the root node of the xml file
            Node jacocoPluginTemplate = pom.importNode(TemplateReader.extractTemplate(Constants.JACOCO_PLUGIN_TEMPLATE), true);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0).setTextContent(coveragePerElement);
            ((Element) jacocoPluginTemplate).getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0).setTextContent(coverageThreshold);
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
                switch (execution.getElementsByTagName(Constants.MAVEN_TAG_GOAL).item(0).getTextContent()) { // there may be multiple goals. handle that.
                    case Constants.JACOCO_GOAL_AGENT_INVOKE:
                        nodeAnalysisReport.put(Constants.JACOCO_GOAL_AGENT_INVOKE, true);
                        deleteJacocoReportPathNodes(execution);
                        break;
                    case Constants.JACOCO_GOAL_REPORT:
                        nodeAnalysisReport.put(Constants.JACOCO_GOAL_REPORT, true);

                        break;
                    case Constants.JACOCO_GOAL_COVERAGE_RULE_INVOKE:
                        execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0).setTextContent(coveragePerElement);
                        execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0).setTextContent(coverageThreshold);
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
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0).setTextContent(coveragePerElement);
                jacocoCheckExecutionElementTemplate.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0).setTextContent(coverageThreshold);
                executions.appendChild(jacocoCheckExecutionNodeTemplate);
                log.info("Check added in " + pluginsParent + " | " + pom.getDocumentURI());
            }
        }
        return pom;
    }

    /**
     * Given Document model and a plugins node in that Document, this will add jacoco plugin inheritance to the model
     */
    public static Document inheritCoverageCheckFromParent(Document pom,
                                                          String coveragePerElement,
                                                          String coverageThreshold)
            throws ParserConfigurationException, IOException, SAXException {
        //log.info("inherit for " + pom.getDocumentURI());
        Node plugins = createPluginsNode(pom, Constants.MAVEN_TAG_BUILD);
        // Check for jacoco plugin existence
        Element inheritedJacocoPlugin = null;
        NodeList pluginList = plugins.getChildNodes();
        for (int i = 0; i < pluginList.getLength(); i++) {
            // Skip line breakers, white spaces etc
            if (!pluginList.item(i).getNodeName().equals(Constants.MAVEN_TAG_PLUGIN)) continue;

            Element plugin = (Element) pluginList.item(i);
            // Every plugin has an 'artifactId'
            if (plugin.getElementsByTagName(Constants.MAVEN_TAG_ARTIFACT_ID).item(0).getTextContent().equals(Constants.JACOCO_MAVEN_PLUGIN)) {
                inheritedJacocoPlugin = plugin;
                break;
            }
        }
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
                            execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_PER_ELEMENT).item(0).setTextContent(coveragePerElement);
                            execution.getElementsByTagName(Constants.JACOCO_TAG_COVERAGE_CHECK_VALUE).item(0).setTextContent(coverageThreshold);
                            log.info("Coverage Thresholds changed");
                            break;
                        case Constants.JACOCO_GOAL_AGENT_INVOKE:
                            deleteJacocoReportPathNodes(execution);
                            break;
                    }
                }
            }
        }
        return pom;
    }

    /**
     * Create <plugins> node under a given parent node name if not already exists
     * (either <build><plugins> or <build><pluginManagement><plugins>)
     * Parent node would be created under the root node of the pom if not exists.
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
        if (parentNodeName == Constants.MAVEN_TAG_PLUGIN_MANAGEMENT) {
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
     * delete destFile node
     */
    private static void deleteJacocoReportPathNodes(Element execution) {

        NodeList destFileNodeList = execution.getElementsByTagName(Constants.JACOCO_DESTFILE);
        for (int i = 0; i < destFileNodeList.getLength(); i++) {
            destFileNodeList.item(i).getParentNode().removeChild(destFileNodeList.item(i));
        }
    }
}
