package component.javaparser;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.MatchTracker;
import org.custommonkey.xmlunit.NodeDetail;
import org.w3c.dom.Node;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MatchTrackerImpl implements MatchTracker {

    private int referenceTags = 0;
    private boolean matchedImplementationTags = false;
    private boolean matchedComponentTags = false;

    private static String printNode(Node node) {
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            StringWriter sw = new StringWriter();
            try {
                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                t.transform(new DOMSource(node), new StreamResult(sw));
            } catch (TransformerException te) {
                System.out.println("nodeToString Transformer Exception");
            }
            return sw.toString();

        }
        return null;
    }

    public void matchFound(Difference difference) {
        NodeDetail controlNode = null;
        NodeDetail testNode = null;
        if (difference != null) {
            controlNode = difference.getControlNodeDetail();
            testNode = difference.getTestNodeDetail();

            if (controlNode.getValue().equals("scr:component")) {
                matchingSCRComponentTag(controlNode, testNode);
            }
            matchingSCRReferenceTag(controlNode, testNode);
        } else {
            System.out.println(controlNode.getNode());
        }
    }

    private boolean readTag(NodeDetail controlNode) {

        if (controlNode.getValue().equals("reference")) {
            return true;
        } else if (controlNode.getValue().equals("implementation")) {
            return true;
        }
        return false;
    }

    private void matchingSCRComponentTag(NodeDetail controlNode, NodeDetail testNode) {
        if (controlNode.getValue().equals("scr:component")) {
            Node nodeList = controlNode.getNode().getNextSibling();
            while (nodeList != null) {
                nodeList = nodeList.getNextSibling();
            }
            String name1 = controlNode.getNode().getAttributes().getNamedItem("name").toString();
            String immediate1 = controlNode.getNode().getAttributes().getNamedItem("immediate").toString();
            String name2 = testNode.getNode().getAttributes().getNamedItem("name").toString();
            String immediate2 = testNode.getNode().getAttributes().getNamedItem("immediate").toString();
            if (name1.equals(name2) && immediate1.equals(immediate2)) {
                matchedComponentTags = true;
                System.out.println("\nBefore Change Component name: " + name2 + "Changed immediate: " + immediate2);
                System.out.println("After Change  name: " + name1 + "Changed immediate: " + immediate1);
                System.out.println("Component name and immediate are same");
            } else {
                String n = "\n";
                String unmatch = "scr:component\n" + name1 + n + immediate1 + n + immediate1 + n + immediate2;
                System.out.println("Component name and immediate  are not same\n" + unmatch);
            }
        }
    }

    private boolean matchingSCRReferenceTag(NodeDetail controlNode, NodeDetail testNode) {
        boolean matched = false;
        if (readTag(controlNode)) {
            String controlNodeValue = printNode(controlNode.getNode());
            String testNodeValue = printNode(testNode.getNode());
            System.out.println("\n---------------------------------------------------------------------------------------\n");
            if (controlNodeValue != null) {
                System.out.println("####################");
                System.out.println("Control Node: " + controlNodeValue);
            }
            if (testNodeValue != null) {
                System.out.println("Test Node: " + testNodeValue);
                System.out.println("####################");
            }

            if (controlNodeValue.equals(testNodeValue)) {
                System.out.println("Both values are matched");
                matched = true;
            }
            if (controlNodeValue.contains("reference")) {
                referenceTags++;
            }
            if (controlNodeValue.contains("implementation")) {
                matchedImplementationTags = true;
            }
            System.out.println("\n---------------------------------------------------------------------------------------\n");
        }
        return matched;
    }

    public int numberOfmatchingReferenceTags() {
        return referenceTags;
    }

    public boolean isMatchedImplementationTags() {
        return matchedImplementationTags;
    }

    public boolean isMatchedComponentTags() {
        return matchedComponentTags;
    }


}
