/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package component.javaparser;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class will compare the previous component.xml file and latest component.xml file
 */
public class CompareXMLDiff {

    private int numberOfReferenceTag;
    private List<String> fileDifferences = new ArrayList<>();

    public void compareXML(String file1, String file2) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        Document doc2 = null;
        db = dbf.newDocumentBuilder();
        doc = db.parse(new File(file1));
        doc2 = db.parse(new File(file2));

        Diff diff = new Diff(doc, doc2);
        setNumberofReferenceTag(doc);
        DetailedDiff detDiff = new DetailedDiff(diff);
        MatchTrackerImpl matchTracker = new MatchTrackerImpl();
        detDiff.overrideMatchTracker(matchTracker);
        detDiff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        detDiff.getAllDifferences();

        boolean isMatchImplementationTags = matchTracker.isMatchedImplementationTags();
        boolean isMatchComponentTag = matchTracker.isMatchedComponentTags();

        if (getNumberofReferenceTag() == matchTracker.numberOfmatchingReferenceTags() && isMatchImplementationTags
                && isMatchComponentTag) {
            System.out.println(file1 + "\n" + file2 + "\nFiles are same");
            fileDifferences.add("matched");
        } else {
            String result = file1 + "\n" + file2 + "\nFiles are different\n";
            System.out.println(result);
            fileDifferences.add(result);
            //writeFile(result,targetFile);
        }
    }


    public List getDifferences() {
        return fileDifferences;
    }

    public int getNumberofReferenceTag() {
        return numberOfReferenceTag;
    }

    public void setNumberofReferenceTag(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("reference");
        numberOfReferenceTag = nodeList.getLength();

    }

    public List<String> getTargetXML(String file) throws IOException, SAXException, ParserConfigurationException {
        List<String> paths = new ArrayList<>();
        WSO2JavaParser wso2JavaParser = new WSO2JavaParser();
        Document doc = wso2JavaParser.readXMLDoc(file);
        NodeList nodeList = doc.getElementsByTagName("Path");
        for (int temp = 0; temp < nodeList.getLength(); temp++) {
            paths.add(nodeList.item(temp).getTextContent());
            //System.out.println(nodeList.item(temp).getTextContent());
        }

        return paths;
    }

}
