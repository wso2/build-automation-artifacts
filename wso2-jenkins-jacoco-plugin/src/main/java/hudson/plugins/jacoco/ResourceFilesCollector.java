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

package hudson.plugins.jacoco;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Collect Jacoco resources files and create a zip file.
 */
public class ResourceFilesCollector {

    private boolean isCreateZipFiles;
    private TaskListener taskListener;
    private JacocoBuildAction action;
    private Run<?, ?> run;

    /**
     * ResourceFilesCollector class constructor
     */
    ResourceFilesCollector(Run<?, ?> run, TaskListener taskListener, JacocoBuildAction action, boolean isCreateZipFiles) {
        this.isCreateZipFiles = isCreateZipFiles;
        this.taskListener = taskListener;
        this.action = action;
        this.run = run;
    }

    private static final String CLASS_DIRECTORY = "classes";
    private static final String SOURCE_DIRECTORY = "sources";
    private static final String JACOCO_RESOURCES = "jacocoResources";
    private static final String RESOURCE_ZIP_FILE_NAME = "resources.zip";
    private static final String JACOCO_EXEC_FILE = "jacoco.exec";
    private static final int BYTE_BUFFER_LENGTH = 1024;

    /**
     * This method collect jacoco exec,source and class files.
     */
    public void collectResourceFiles() {

        final PrintStream logger = taskListener.getLogger();

        if (isCreateZipFiles) {
            logger.println("[JaCoCo plugin] Collecting .java , .class and jacoco.exec files");
            final List<File> jacocoExecFiles = action.getJacocoReport().getExecFiles();
            int fileSize = jacocoExecFiles.size();
            if (fileSize == 0) {
                logger.println("[JaCoCo plugin] No jacoco.exec file recorded");
            } else {
                String jobInfoDirectory = run.getRootDir().getAbsolutePath();
                logger.println("[JaCoCo plugin] Creating folder to store files");
                String jacocoResourcesFolder = jobInfoDirectory + File.separator + JACOCO_RESOURCES;
                Path path = Paths.get(jacocoResourcesFolder);
                try {
                    Files.createDirectories(path);
                    logger.println("[JaCoCo plugin] File created successfully at " + jobInfoDirectory + ".");
                    FileUtils.copyDirectory(new File(jobInfoDirectory + File.separator + "jacoco" + File.
                            separator + CLASS_DIRECTORY), new File
                            (jacocoResourcesFolder + File.separator + "classes"));
                    FileUtils.copyDirectory(new File(jobInfoDirectory + File.separator + "jacoco" + File.
                            separator + SOURCE_DIRECTORY), new File
                            (jacocoResourcesFolder + File.separator + "sources"));
                    if (fileSize == 1) {
                        FileUtils.copyFile(jacocoExecFiles.get(0), new File(jacocoResourcesFolder +
                                File.separator + JACOCO_EXEC_FILE));
                    } else {
                        logger.println("[JaCoCo plugin] Found " + fileSize + " jacoco.exec files.Merging...");
                        ExecFileLoader loader = new ExecFileLoader();
                        for (File exec : jacocoExecFiles) {
                            loader.load(exec);
                        }
                        logger.println("[JaCoCo plugin] Successfully merged jacoco.exec files.");
                        loader.save(new File(jacocoResourcesFolder + File.separator + JACOCO_EXEC_FILE),
                                false);
                    }
                    logger.println("[JaCoCo plugin] Compressing Jacoco resource folder...");
                    compressAndCopy(jacocoResourcesFolder, jobInfoDirectory + File.separator +
                            RESOURCE_ZIP_FILE_NAME);
                    logger.println("[JaCoCo plugin] Successfully compressed jacoco resource folder");
                } catch (IOException ex) {
                    logger.println("Error occured while creating Jacoco resource zip file." + ex.getMessage());
                }
            }
        } else {
            logger.println("Jacoco resources zip is not created.User disabled the feature.");
        }
    }

    /**
     * This method compress the file in the sourcePath and copy it to destinationPath
     *
     * @param sourcePath      file to zip
     * @param destinationPath destination file path
     * @throws IOException If the given source path is not available the method throws an IOException
     */
    private static void compressAndCopy(String sourcePath, String destinationPath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(destinationPath); ZipOutputStream zipOutputStream
                = new ZipOutputStream(fileOutputStream);) {

            File fileToZip = new File(sourcePath);
            compressFolder(fileToZip, fileToZip.getName(), zipOutputStream);
            FileUtils.deleteDirectory(new File(sourcePath));
        }
    }

    /**
     * This method compress a given folder
     *
     * @param sourceFile      path of the file to zip
     * @param sourceFileName  name of the file to zip
     * @param zipOutputStream ZipOutputStream to store the zipped content
     * @throws IOException If the the given file is not exist, the method will throw an IOException
     */
    private static void compressFolder(File sourceFile, String sourceFileName, ZipOutputStream zipOutputStream) throws
            IOException {
        if (sourceFile.isDirectory()) {
            File[] subFiles = sourceFile.listFiles();
            for (File subFile : subFiles) {
                compressFolder(subFile, sourceFileName + File.separator + subFile.getName(), zipOutputStream);
            }
        } else {
            try (FileInputStream fileInputStream = new FileInputStream(sourceFile)) {
                ZipEntry zipEntry = new ZipEntry(sourceFileName);
                zipOutputStream.putNextEntry(zipEntry);
                byte[] bytes = new byte[BYTE_BUFFER_LENGTH];
                int length;
                while ((length = fileInputStream.read(bytes)) >= 0) {
                    zipOutputStream.write(bytes, 0, length);
                }
            }
        }
    }
}
