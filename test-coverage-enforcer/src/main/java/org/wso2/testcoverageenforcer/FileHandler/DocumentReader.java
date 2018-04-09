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

package org.wso2.testcoverageenforcer.FileHandler;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Contains a method to parse pom XML file in to an org.w3c.dom.Document object
 */
public class DocumentReader {

    /**
     * Read a pom XML file and parsing it to an org.w3c.dom.Document object
     *
     * @param xmlPath File path to the pom
     * @return org.w3c.dom.Document object for the pom file
     * @throws ParserConfigurationException Error while parsing the pom file
     * @throws IOException                  Error reading the pom file
     * @throws SAXException                 Error while parsing the pom's file input stream
     */
    public static Document readDocument(String xmlPath) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        try (FileInputStream documentStream = new FileInputStream(new File(xmlPath))) {
            return db.parse(documentStream);
        }
    }
}
