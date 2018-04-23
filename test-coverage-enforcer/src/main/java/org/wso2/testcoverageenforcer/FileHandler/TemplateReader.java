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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Contains methods to read template xml files to return it's node tree
 */
public class TemplateReader {

    /**
     * Given a xml file path, read xml as org.w3c.Document model and return it's root element as a Node
     *
     * @param xmlPath xml file path
     * @return Root element of the xml file
     * @throws PomFileReadException Error occurred while reading a pom file in to a org.w3c.dom.Document object
     */
    public static Node extractTemplate(String xmlPath)
            throws PomFileReadException {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();

            Path tempFile = Files.createTempFile("template_temp", ".xml");
            try (InputStream stream = TemplateReader.class.getClassLoader().getResourceAsStream(xmlPath)) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try (FileInputStream xmlFileStream = new FileInputStream(tempFile.toFile())) {
                Document xmlFile = db.parse(xmlFileStream);
                return xmlFile.getDocumentElement();
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new PomFileReadException(e.getMessage());
        }
    }
}
