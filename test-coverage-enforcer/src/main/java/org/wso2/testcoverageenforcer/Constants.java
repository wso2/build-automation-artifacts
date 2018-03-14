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

import java.io.File;

public class Constants {

    public static final String POM_NAME = "pom.xml";
    public static final String UTF_8_CHARSET_NAME = "UTF-8";
    public static final String JACOCO_NODE_COVERAGE_CHECK_RULE = "jacoco-line-check-execution-node.xml";
    public static final String JACOCO_MAVEN_PLUGIN = "jacoco-maven-plugin";
    public static final String JACOCO_GOAL_AGENT_INVOKE = "prepare-agent";
    public static final String JACOCO_GOAL_COVERAGE_RULE_INVOKE = "check";
    public static final String JACOCO_GOAL_REPORT = "report";
    public static final String JACOCO_PLUGIN_ARTIFACT_ID = "jacoco-maven-plugin";
    public static final String JACOCO_TAG_COVERAGE_CHECK_VALUE = "minimum";
    public static final String JACOCO_TAG_COVERAGE_PER_ELEMENT = "element";
    public static final String JACOCO_TAG_REPORT_DEST = "destFile";
    public static final String MAVEN_TAG_PLUGIN = "plugin";
    public static final String MAVEN_TAG_EXECUTION = "execution";
    public static final String MAVEN_TAG_GOAL = "goal";
    public static final String MAVEN_TAG_BUILD = "build";
    public static final String MAVEN_TAG_PLUGIN_MANAGEMENT = "pluginManagement";
    public static final String MAVEN_TAG_ARTIFACT_ID = "artifactId";
    public static final String MAVEN_TAG_EXECUTIONS = "executions";
    public static final String MAVEN_TAG_PLUGINS = "plugins";

    private static final String RESOURCE_FOLDER = "src" + File.separator + "main" + File.separator + "resources";

    public static final String JACOCO_INHERITED_PLUGIN_TEMPLATE = RESOURCE_FOLDER + File.separator + "jacoco_inherited_plugin_template.xml";
    public static final String JACOCO_PLUGIN_TEMPLATE = RESOURCE_FOLDER + File.separator + "jacoco_plugin_template.xml";
    public static final String JACOCO_PREPARE_AGENT_TEMPLATE = RESOURCE_FOLDER + File.separator + "jacoco_prepare_agent_template.xml";
    public static final String JACOCO_REPORT_TEMPLATE = RESOURCE_FOLDER + File.separator + "jacoco_report_template.xml";
    public static final String JACOCO_CHECK_TEMPLATE = RESOURCE_FOLDER + File.separator + "jacoco_check_template.xml";

    public static final String TEST_FOLDER = "src" + File.separator + "test";
}
