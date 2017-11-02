package component.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class WSO2JavaParser {

    FileInputStream in = null;
    private SCRComponent scrComponent;

    /**
     * This method use process the java file and change the content of file
     * if java file has java doc comment this method return the true value
     *
     * @param path
     * @return
     * @throws IOException
     */
    public boolean parseJavaFile(String path) throws IOException {
        boolean hasScrDoc = false;
        this.in = new FileInputStream(path);
        ParserConfiguration ps = new ParserConfiguration();
        ps.setDoNotAssignCommentsPrecedingEmptyLines(false);
        JavaParser.setStaticConfiguration(ps);

        CompilationUnit cu = JavaParser.parse(in);
        addAnnotationsImports(cu);
        JavaDocCommentVisitor javaDocCommentVisitor = new JavaDocCommentVisitor();
        cu.accept(javaDocCommentVisitor, null);
        // Read the java doc comment by javaDocCommentVisitor instance
        String javaDocComment = javaDocCommentVisitor.getDocContent();

        // Split the doc comment
        if (javaDocComment != null) {
            String[] docComments = splitJavaDoc(javaDocComment);
            // Here splited docComment has two parts. class comment and @scr annotation part
            if (docComments != null && docComments.length == 2) {
                splitSCRComment(docComments[1]);
                addAnnotations(scrComponent, docComments[0], path);
                hasScrDoc = true;
            } else {
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<Can not identified comment>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                System.out.print(javaDocComment);
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            }
        }
        return hasScrDoc;
    }

    private String[] splitJavaDoc(String comment) {
        String[] str = comment.split("\n");
        String[] comments = null;
        for (int i = 0; i < str.length; i++) {
            if (checkScrComponentAnnotation(str[i])) {
                comments = comment.split("@scr.component");
            }
        }
        if (comments != null && comments.length == 2) {
            comments[1] = "\n * @scr.component" + comments[1];
        }
        return comments;
    }

    private void splitSCRComment(String javaDocComment) {
        String[] scrElementList = javaDocComment.toString().split("@");
        List<String> list = new ArrayList();
        for (String x : scrElementList) {
            if (x.contains("scr")) {
                x = x.replaceAll("/", "");
                x = x.replaceAll("\\*", "");
                list.add(x);
            }
        }
        for (String x : list) {
            if (x.contains("component")) {
                getScrAnnotation(x);
            } else if (x.contains("reference")) {
                scrComponent.addReferences(getReference(x));
            } else {
                System.out.print("error");
            }
        }
    }

    private boolean checkScrComponentAnnotation(String comment) {
        String patternString = "\\s.\\s(@scr.component)+";
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(comment);
        return matcher.lookingAt();
    }

    // This method identified the @scr.component annotation elements
    private void getScrAnnotation(String x) {

        String immediate = null;
        String name = null;
        Pattern p = Pattern.compile("immediate=\"([^\"]*)\"");
        Matcher m = p.matcher(x);
        while (m.find()) {
            immediate = m.group(1);
        }
        Pattern pname = Pattern.compile("name=\"([^\"]*)\"");
        Matcher mname = pname.matcher(x);
        while (mname.find()) {
            name = mname.group(1);
        }
        //System.out.println("name: " + name + " immediate :" + immediate + "\n");
        this.scrComponent = new SCRComponent(name, immediate);
    }

    // This method identified the @scr.reference annotation elements
    private SCRReference getReference(String reference) {

        SCRReference scrReference = new SCRReference();
        Pattern p = Pattern.compile("name=\"([^\"]*)\"");
        Matcher m = p.matcher(reference);
        while (m.find()) {
            scrReference.setReferenceName(m.group(1));
            //System.out.println("Reference Name: " + scrReference.getReferenceName());
        }
        Pattern q = Pattern.compile("interface=\"([^\"]*)\"");
        Matcher x = q.matcher(reference);
        while (x.find()) {
            scrReference.setService(x.group(1));
            // System.out.println("Service: " + scrReference.getService());
        }
        Pattern c = Pattern.compile("cardinality=\"([^\"]*)\"");
        Matcher d = c.matcher(reference);
        while (d.find()) {
            scrReference.setCardinality(d.group(1));
            //System.out.println("Cardinality: " + scrReference.getCardinality());
        }
        Pattern cs = Pattern.compile("policy=\"([^\"]*)\"");
        Matcher ds = cs.matcher(reference);
        while (ds.find()) {
            scrReference.setPolicy(ds.group(1));
            // System.out.println("Policy: " + scrReference.getPolicy());
        }
        Pattern csd = Pattern.compile("unbind=\"([^\"]*)\"");
        Matcher dsd = csd.matcher(reference);
        while (dsd.find()) {
            scrReference.setUnbind(dsd.group(1));
            //System.out.println("unbind: " + scrReference.getUnbind());
        }
        Pattern csdx = Pattern.compile("bind=\"([^\"]*)\"");
        String dsq = reference.replace("unbind", "***");
        Matcher dsdx = csdx.matcher(dsq);
        while (dsdx.find()) {
            scrReference.setBind(dsdx.group(1));
            // System.out.println("bind: " + scrReference.getBind());
        }
        System.out.println("\n");
        return scrReference;
    }

    // This method get the java files from internal directory
    public List<String> getInternalFiles(String directoryPath) {
        File folder = new File(directoryPath);
        File[] listOfFiles = folder.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String file = directoryPath + "/" + listOfFiles[i].getName();
                    fileNames.add(file);
                }
            }
        }
        return fileNames;
    }

    public List<String> getComponentPaths(String file) {
        List<String> paths = new ArrayList<>();
        Document doc = readXMLDoc(file);
        NodeList nodeList = doc.getElementsByTagName("Path");
        for (int temp = 0; temp < nodeList.getLength(); temp++) {
            paths.add(nodeList.item(temp).getTextContent());
            //System.out.println(nodeList.item(temp).getTextContent());
        }

        return paths;
    }

    public List<String> getPomPaths(String file) {
        List<String> pomPaths = new ArrayList<>();
        Document doc = readXMLDoc(file);
        NodeList nodeList = doc.getElementsByTagName("POMPath");
        for (int temp = 0; temp < nodeList.getLength(); temp++) {
            pomPaths.add(nodeList.item(temp).getTextContent());
        }
        return pomPaths;
    }

    public Document readXMLDoc(String file) {
        File inputFile = new File(file);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = dBuilder.parse(inputFile);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void addAnnotations(SCRComponent scrComponent, String comment, String path) throws IOException {
        if (scrComponent != null) {
            MethodVisitor methodVisitor = new MethodVisitor();
            methodVisitor.setScrComponent(scrComponent);
            ClassVisitor classVisitor = new ClassVisitor();
            classVisitor.setScrComponent(scrComponent);
            if (comment != null && comment.length() > 5) {
                classVisitor.setClassComment(comment);
            }
            File fis = new File(path);

            CompilationUnit cu = JavaParser.parse(fis, StandardCharsets.UTF_8);
            cu.accept(methodVisitor, null);
            cu.accept(classVisitor, null);
            addAnnotationsImports(cu);
            System.out.println(cu.toString());
            cu.getStorage().get().save();
            Files.write(fis.toPath(), Arrays.asList(cu.toString()), StandardCharsets.UTF_8);

        }
    }

    // Here add the imports of osgi annotations
    private void addAnnotationsImports(CompilationUnit cu) {
        cu.addImport("org.osgi.service.component.annotations.Activate");
        cu.addImport("org.osgi.service.component.annotations.Component");
        cu.addImport("org.osgi.service.component.annotations.Deactivate");
        cu.addImport("org.osgi.service.component.annotations.Reference");
        cu.addImport("org.osgi.service.component.annotations.ReferenceCardinality");
        cu.addImport("org.osgi.service.component.annotations.ReferencePolicy");
    }

}
