package component.javaparser;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class CompareXMLDiff {

    private int numberOfReferenceTag;
    private List<String> fileDifferences = new ArrayList<>();

    public void compareXML(String file1, String file2) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        Document doc2 = null;
        try {
            db = dbf.newDocumentBuilder();
            doc = db.parse(new File(file1));
            doc2 = db.parse(new File(file2));

            //doc.

            Diff diff = new Diff(doc, doc2);
            setNumberofReferenceTag(doc);
            DetailedDiff detDiff = new DetailedDiff(diff);
            MatchTrackerImpl matchTracker = new MatchTrackerImpl();
            detDiff.overrideMatchTracker(matchTracker);
            detDiff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
            detDiff.getAllDifferences();

            boolean isMatchImplementationTags = matchTracker.isMatchedImplementationTags();
            boolean isMatchComponentTag = matchTracker.isMatchedComponentTags();

            if (getNumberofReferenceTag() == matchTracker.numberOfmatchingReferenceTags() && isMatchImplementationTags &&
                    isMatchComponentTag) {
                System.out.println(file1 + "\n" + file2 + "\nFiles are same");
                fileDifferences.add("matched");
            } else {
                String result = file1 + "\n" + file2 + "\nFiles are different\n";
                System.out.println(result);
                fileDifferences.add(result);
                //writeFile(result,targetFile);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
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

    public List<String> getTargetXML(String file) {
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
