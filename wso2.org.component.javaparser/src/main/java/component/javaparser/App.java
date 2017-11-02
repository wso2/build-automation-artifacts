package component.javaparser;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class App {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("path name is missing");
        } else {
            String path = args[0];
            WSO2JavaParser wso2JavaParser = new WSO2JavaParser();
            if (path.contains("component.xml")) {

                List internalPaths = wso2JavaParser.getComponentPaths(path);


                int directoryPaths = internalPaths.size();

                for (int i = 0; i < directoryPaths; i++) {
                    List<String> fileNames = wso2JavaParser.getInternalFiles(internalPaths.get(i).toString());
                    for (int x = 0; x < fileNames.size(); x++) {
                        wso2JavaParser.parseJavaFile(fileNames.get(x));
                    }
                }
                List pomPaths = wso2JavaParser.getPomPaths(path);
                WSO2POMReader wso2POMReader = new WSO2POMReader();
                String parentPom = wso2POMReader.getParentPom(pomPaths);
                for (int i = 0; i < pomPaths.size(); i++) {
                    boolean isParentPom = false;
                    if (parentPom.equals(pomPaths.get(i))) {
                        isParentPom = true;
                    }
                    wso2POMReader.addDependency(pomPaths.get(i).toString(), isParentPom);
                }

            } else if (path.contains("target.xml")) {
                App app = new App();
                CompareXMLDiff compareXMLDiff = new CompareXMLDiff();
                List internalPaths = compareXMLDiff.getTargetXML(path);

                String serviceComponentFile = null;

                for (int i = 0; i < internalPaths.size(); i++) {
                    List<String> fileNames = wso2JavaParser.getInternalFiles(internalPaths.get(i).toString());

                    for (int x = 0; x < fileNames.size(); x++) {
                        if (fileNames.get(x).contains("serviceComponents.xml")) {
                            serviceComponentFile = fileNames.get(x);
                        }
                    }


                    for (int x = 0; x < fileNames.size(); x++) {
                        if (fileNames.size() == 2 && !fileNames.get(x).contains("serviceComponents.xml") && serviceComponentFile != null) {
                            compareXMLDiff.compareXML(fileNames.get(x), serviceComponentFile);
                        } else if (fileNames.size() > 2 && !fileNames.get(x).contains("serviceComponents.xml") && serviceComponentFile != null) {
                            compareXMLDiff.compareXML(fileNames.get(x), serviceComponentFile);
                        }
                    }

                }

                // Create new file and write the final result
                String reusltFile = app.createFilepath(path);
                app.createNewFile(reusltFile);
                List<String> fileDifferences = compareXMLDiff.getDifferences();
                boolean matched = app.isMatched(fileDifferences);

                String resultFile = app.createFilepath(path);
                if (!matched) {
                    for (String diff : fileDifferences) {
                        app.writeFile(diff, resultFile);
                    }
                } else if (matched) {
                    app.writeFile("success", resultFile);
                } else {
                    app.writeFile("Unknown", resultFile);
                }

            }
        }

    }

    private void writeFile(String result, String file) throws IOException {
        Path path = Paths.get(file);
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        content = content + result;
        Files.write(path, content.getBytes(charset));
    }

    private String createFilepath(String path) {
        String targetPath = path.replace("target.xml", "");
        String resultFile = targetPath + "result.txt";
        return resultFile;
    }

    private void createNewFile(String nwFile) {
        File file = new File(nwFile);
        boolean fvar = false;
        try {
            fvar = file.createNewFile();

            if (fvar) {
                System.out.println("Create the target xml file");
            } else {
                System.out.println("Result file already present at the specified location");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isMatched(List<String> differences) {
        boolean match = false;
        String matched = "matched";
        for (String diff : differences) {
            if (matched.equals(diff)) {
                match = true;
            } else {
                match = false;
                break;
            }
        }
        return match;
    }

}
