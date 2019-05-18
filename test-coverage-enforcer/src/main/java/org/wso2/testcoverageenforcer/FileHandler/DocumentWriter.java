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
import org.wso2.testcoverageenforcer.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Contains methods to write org.w3c.dom.Document objects to xml files
 */
public class DocumentWriter {

    /**
     * Write a given org.w3c.dom.Document object as a pom xml file
     *
     * @param xml           org.w3c.dom.Document object
     * @param targetXmlPath File path to write pom file
     * @throws PomFileWriteException Error occurred while writing org.w3c.dom.Document object to a pom file
     */
    public static void writeDocument(Document xml, String targetXmlPath) throws PomFileWriteException {

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Path tempFile = Files.createTempFile("xml_indent_temp", ".xsl");
            try (InputStream stream = TemplateReader.class.getClassLoader().getResourceAsStream(
                    Constants.XML_STYLE_SHEET)) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(tempFile.toString()));
            if (xml.getXmlEncoding() != null) transformer.setOutputProperty(OutputKeys.ENCODING, xml.getXmlEncoding());
            if (xml.getXmlVersion() != null) transformer.setOutputProperty(OutputKeys.VERSION, xml.getXmlVersion());
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(new File(targetXmlPath));
            transformer.transform(source, result);
        } catch (TransformerException | IOException e) {
            throw new PomFileWriteException(e.getMessage(), e);
        }
    }
}
