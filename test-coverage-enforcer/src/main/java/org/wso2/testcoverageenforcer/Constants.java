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
    public static final String JACOCO_MAVEN_PLUGIN = "jacoco-maven-plugin";
    public static final String SUREFIRE_MAVEN_PLUGIN = "maven-surefire-plugin";
    public static final String JACOCO_GOAL_AGENT_INVOKE = "prepare-agent";
    public static final String JACOCO_GOAL_COVERAGE_RULE_INVOKE = "check";
    public static final String JACOCO_GOAL_REPORT = "report";
    public static final String JACOCO_TAG_COVERAGE_CHECK_VALUE = "minimum";
    public static final String JACOCO_TAG_COVERAGE_PER_ELEMENT = "element";
    public static final String[] JACOCO_POM_PATH_MINIMUM = {"configuration", "rules", "rule", "limits", "limit", "minimum"};
    public static final String[] JACOCO_POM_PATH_ELEMENT = {"configuration", "rules", "rule", "element"};
    public static final String[] JACOCO_POM_PATH_DATA_FILE = {"configuration", "dataFile"};
    public static final String[] SUREFIRE_POM_PATH_CONFIGURATION = {"configuration"};
    public static final String MAVEN_TAG_PLUGIN = "plugin";
    public static final String MAVEN_TAG_EXECUTION = "execution";
    public static final String MAVEN_TAG_GOAL = "goal";
    public static final String MAVEN_TAG_BUILD = "build";
    public static final String MAVEN_TAG_PLUGIN_MANAGEMENT = "pluginManagement";
    public static final String MAVEN_TAG_ARTIFACT_ID = "artifactId";
    public static final String MAVEN_TAG_EXECUTIONS = "executions";
    public static final String MAVEN_TAG_PLUGINS = "plugins";
    public static final String MAVEN_TAG_CONFIGURATION = "configuration";
    public static final String MAVEN_MVN = "mvn";
    public static final String MAVEN_CLEAN = "clean";
    public static final String MAVEN_INSTALL = "install";
    public static final int MAVEN_HEALTHY_BUILD = 0;
    public static final int MAVEN_BAD_BUILD = -1;
    public static final String SUREFIRE_TAG_ARGLINE = "argLine";
    public static final String JACOCO_TAG_REPORT_READ = "dataFile";
    public static final String JACOCO_TAG_SUREFIRE_ARGLINE_NAME = "propertyName";
    public static final String JACOCO_DESTFILE = "destFile";
    public static final String JACOCO_COVERAGE_CHECK_SUCCESS_MESSAGE = "All coverage checks have been met";
    public static final String GIT_PR_TITLE = "Integration of Jacoco coverage check";
    public static final String GIT_PR_MASTER = "master";
    public static final short GIT_TIME_PERIOD_OF_INTEREST = 6;
    public static final short GITHUB_COMMITS_OF_INTEREST_COUNT = 8;
    public static final short GITHUB_RECENT_COMMITS_THRESHOLD = 3;
    public static final String GIT_JENKINS_BOT = "wso2-jenkins-bot";
    public static final String COMMIT_MESSAGE_COVERAGE_CHECK = "Other: Jacoco Coverage Check Integration";
    public static final String TEST_FOLDER = "src" + File.separator + "test";
    public static final String COVERAGE_PER_ELEMENT = "BUNDLE";
    public static final String GIT_PR_BODY = "pullRequestMessage.txt";
    public static final String GITHUB_URL = "https://github.com/";
    public static final String COLON = ":";
    public static final String SQL_URL = "sql.url";
    public static final String SQL_USERNAME = "sql.username";
    public static final String SQL_PASSWORD = "sql.password";
    public static final String SQL_TABLE = "sql.table";
    public static final String SQL_REPO_COLLUMN = "sql.github.column.id";
    public static final String GIT_USERNAME = "github.username";
    public static final String GIT_PASSWORD = "github.password";
    public static final String GIT_EMAIL = "github.email";
    public static final String DEFAULT_JACOCO_REPORT_PATH = "${project.build.directory}/jacoco.exec";
    public static final String DEFAULT_JACOCO_SUREFIRE_ARGLINE = "argLine";
    public static final String DEFAULT_JACOCO_SUREFIRE_PROPERTY_NAME = "argLine";
    public static final String BUILD_TARGET_FOLDER = File.separator + "target";
    public static final String EMPTY_STRING = "";
    public static final String BUILD_CLASSES_FOLDER = "classes";
    public static final String BUILD_EXECUTION_FILE = "exec";
    public static final String ZERO = "0";
    public static final double DECIMAL_CONSTANT_2 = 100.0;
    public static final String JACOCO_INHERITED_PLUGIN_TEMPLATE = "jacoco_inherited_plugin_template.xml";
    public static final String JACOCO_PLUGIN_TEMPLATE = "jacoco_plugin_template.xml";
    public static final String JACOCO_PREPARE_AGENT_TEMPLATE = "jacoco_prepare_agent_template.xml";
    public static final String JACOCO_REPORT_TEMPLATE = "jacoco_report_template.xml";
    public static final String JACOCO_CHECK_TEMPLATE = "jacoco_check_template.xml";
    public static final String JACOCO_SUREFIRE_PLUGIN_TEMPLATE = "surefire_plugin_template.xml";
    public static final String JACOCO_SUREFIRE_ARGLINE_TEMPLATE = "jacoco_surefire_argument_line_template.xml";
    public static final String JACOCO_CHECK_INHERIT_TEMPLATE = "jacoco_check_inheritance_template.xml";
    public static final String SUREFIRE_SIMPLE_INHERIT_TEMPLATE = "surefire_simple_inherit.xml";
    public static final String SUREFIRE_INHERIT_WITH_ARGLINE_TEMPLATE = "surefire_inherit_with_arg_line.xml";
    public static final String XML_STYLE_SHEET = "xml_indent.xsl";
    public static final String CHILD_NAME_TESTS_INTEGRATION = "tests-integration";
    public static final String LINE_SEPERATOR = "line.seperator";
}
