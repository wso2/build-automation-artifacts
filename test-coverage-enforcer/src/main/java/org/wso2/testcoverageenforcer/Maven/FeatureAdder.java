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

package org.wso2.testcoverageenforcer.Maven;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.wso2.testcoverageenforcer.Maven.POM.ParentMavenPom;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Contain methods to features to a multi-module maven project
 */
public class FeatureAdder {

    /**
     * Deep integration of Jacoco coverage check rule with an existing multi-module maven project
     */
    public static void intergrateJacocoCoverageCheck (
            String projectPath,
            String coveragePerElement,
            String coverageThreshold) throws IOException, XmlPullParserException, ParserConfigurationException, SAXException, TransformerException{

        ParentMavenPom parent = new ParentMavenPom(projectPath);
        if (parent.hasChildren()) {
            parent.enforceCoverageCheckUnderPluginManagement(coveragePerElement, coverageThreshold);
            parent.inheritCoverageCheckInChildren(parent.getChildren(), coveragePerElement, coverageThreshold);
        } else if (parent.hasTests()) {
            parent.enforceCoverageCheckUnderBuildPlugins( coveragePerElement, coverageThreshold);
        }
    }
}
