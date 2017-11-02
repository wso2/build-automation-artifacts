package component.javaparser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * This class read and process the pom files of repository
 */
public class WSO2POMReader {

    private void addSCRPluginVersion(Document doc, NodeList nodeList) {
        Element ele = null;
        String version = "1.16.0";
        for (int i = 0; i < nodeList.getLength(); i++) {
            ele = (Element) nodeList.item(i);
            if (hasSCRpluginVersion(nodeList)) {
                Node pluginVersion = ele.getElementsByTagName("maven.scr.plugin.version").item(0).getFirstChild();
                pluginVersion.setNodeValue(version);
            } else {
                Element pluginVersion = doc.createElement("maven.scr.plugin.version");
                pluginVersion.appendChild(doc.createTextNode(version));
                ele.appendChild(pluginVersion);
            }
        }
    }

    public void addDependency(String file, boolean parentPOM) {
        try {
            File inputFile = new File(file);
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList dependencies = doc.getElementsByTagName("dependencies");
            NodeList plugin = doc.getElementsByTagName("plugin");
            if (hasSCRPluginNode(plugin) && !parentPOM) {
                System.out.print("Component pom : " + file + "\t\t");
                addSCRDependencyToComponent(doc, dependencies);
                removeSCRpluginVersion(plugin, doc, parentPOM);
                updatePomFile(doc, file);
            } else if (parentPOM) {
                System.out.print("Parent pom : " + file + "\t\t");
                NodeList parentDependencies = doc.getElementsByTagName("dependencies");
                NodeList properties = doc.getElementsByTagName("properties");
                NodeList pluginManagement = doc.getElementsByTagName("pluginManagement");
                NodeList plugins = doc.getElementsByTagName("plugins");
                addSCRdependecyToParent(doc, parentDependencies);
                addSCRDependencyVesion(doc, properties);
                addSCRPlugin(doc, pluginManagement);
                removeSCRPlugin(plugins);
                addSCRPluginVersion(doc, properties);
                updatePomFile(doc, file);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (org.xml.sax.SAXException e) {
            e.printStackTrace();
        }
    }

    public void removeSCRpluginVersion(NodeList plugins, Document doc, boolean parentPOM) {
        Element plu = null;
        for (int i = 0; i < plugins.getLength(); i++) {
            plu = (Element) plugins.item(i);
            String name = plu.getElementsByTagName("artifactId").item(0).getTextContent().toString();
            Node version = plu.getElementsByTagName("version").item(0);
            if (name.equals("maven-scr-plugin")) {
                if (version != null) {
                    plu.removeChild(version);
                }
            }
        }
    }


    public void addSCRPlugin(Document doc, NodeList pluginManagement) {
        Element plugin = doc.createElement("plugin");
        Element groupId = doc.createElement("groupId");
        groupId.appendChild(doc.createTextNode("org.apache.felix"));
        Element artifactId = doc.createElement("artifactId");
        artifactId.appendChild(doc.createTextNode("maven-scr-plugin"));
        Element version = doc.createElement("version");
        version.appendChild(doc.createTextNode("${maven.scr.plugin.version}"));
        Element executions = doc.createElement("executions");
        Element execution = doc.createElement("execution");
        Element id = doc.createElement("id");
        id.appendChild(doc.createTextNode("generate-scr-scrdescriptor"));
        Element goals = doc.createElement("goals");
        Element goal = doc.createElement("goal");
        goal.appendChild(doc.createTextNode("scr"));
        Element scope = doc.createElement("scope");
        scope.appendChild(doc.createTextNode("provided"));
        plugin.appendChild(groupId);
        plugin.appendChild(artifactId);
        plugin.appendChild(version);
        plugin.appendChild(executions);
        executions.appendChild(execution);
        execution.appendChild(id);
        execution.appendChild(goals);
        goals.appendChild(goal);

        for (int i = 0; i < pluginManagement.getLength(); i++) {
            Node nNode = pluginManagement.item(i);
            Element eElement = (Element) nNode;
            eElement.getElementsByTagName("plugins").item(0).appendChild(plugin);
        }

    }

    private void removeSCRPlugin(NodeList plugins) {
        for (int i = 0; i < plugins.getLength(); i++) {
            Node nNode = plugins.item(i);
            Element eElement = (Element) nNode;
            String plugin = "maven-scr-plugin";
            String name = eElement.getTextContent();
            if (name.contains(plugin)) {
                nNode.removeChild(eElement.getElementsByTagName("plugin").item(0));
            }
        }
    }


    public String getParentPom(List<String> paths) {
        String shortest = paths.get(0);

        for (String str : paths) {
            if (str.length() < shortest.length()) {
                shortest = str;
            }
        }
        System.out.println("The shortest string: " + shortest);
        return shortest;
    }

    private boolean hasSCRpluginVersion(NodeList nodeList) {
        Element ele = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            ele = (Element) nodeList.item(i);
            Node pluginVersion = ele.getElementsByTagName("maven.scr.plugin.version").item(0);
            if (pluginVersion != null) {
                return true;
            }
        }
        return false;
    }

    private void addSCRDependencyVesion(Document doc, NodeList nlist) {
        Element ele = null;
        for (int i = 0; i < nlist.getLength(); i++) {
            ele = (Element) nlist.item(i);
            Element dependecyVersion = doc.createElement("apache.felix.scr.ds.annotations.version");
            dependecyVersion.appendChild(doc.createTextNode("1.2.4"));
            ele.appendChild(dependecyVersion);
        }
    }

    private void addSCRDependencyToComponent(Document doc, NodeList nlist) {
        Element ele = null;
        for (int i = 0; i < nlist.getLength(); i++) {
            ele = (Element) nlist.item(i);
            Element dependecy = doc.createElement("dependency");
            Element groupId = doc.createElement("groupId");
            groupId.appendChild(doc.createTextNode("org.apache.felix"));
            Element artifactId = doc.createElement("artifactId");
            artifactId.appendChild(doc.createTextNode("org.apache.felix.scr.ds-annotations"));
            Element scope = doc.createElement("scope");
            scope.appendChild(doc.createTextNode("provided"));
            ele.appendChild(dependecy);
            dependecy.appendChild(groupId);
            dependecy.appendChild(artifactId);
            dependecy.appendChild(scope);
        }
    }

    private boolean hasSCRPluginNode(NodeList plugins) {
        boolean isSCR = false;
        Element plu = null;
        for (int i = 0; i < plugins.getLength(); i++) {
            plu = (Element) plugins.item(i);
            String name = plu.getElementsByTagName("artifactId").item(0).getTextContent().toString();
            if (name.equals("maven-scr-plugin")) {
                isSCR = true;
            }
        }
        return isSCR;
    }

    public void addSCRdependecyToParent(Document doc, NodeList nlist) {
        Element ele = null;
        Element emp = null;
        for (int i = 0; i < nlist.getLength(); i++) {
            emp = (Element) nlist.item(i);
            Element dependecy = doc.createElement("dependency");
            Element groupId = doc.createElement("groupId");
            groupId.appendChild(doc.createTextNode("org.apache.felix"));
            Element artifactId = doc.createElement("artifactId");
            artifactId.appendChild(doc.createTextNode("org.apache.felix.scr.ds-annotations"));
            Element version = doc.createElement("version");
            version.appendChild(doc.createTextNode("${apache.felix.scr.ds.annotations.version}"));
            emp.appendChild(dependecy);
            dependecy.appendChild(groupId);
            dependecy.appendChild(artifactId);
            dependecy.appendChild(version);
        }
    }

    public void updatePomFile(Document doc, String file) {
        doc.setXmlStandalone(true);
        doc.getDocumentElement().normalize();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
            StreamResult result = new StreamResult(new File(file));
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            modifyProjectTag(file);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        System.out.println(" <<<<<updated successfully>>>>>");
    }

    private void modifyProjectTag(String file) {
        FileInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));

            System.out.println("Reading File line by line using BufferedReader");

            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line.contains("<project")) {
                    System.out.println(line);
                    String[] projectTag = line.split("-->");
                    writeFile(line, "  -->\n" + projectTag[1], file);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFile(String regex, String replacement, String file) throws IOException {
        Path path = Paths.get(file);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(regex, replacement);
        Files.write(path, content.getBytes(charset));
    }

}
