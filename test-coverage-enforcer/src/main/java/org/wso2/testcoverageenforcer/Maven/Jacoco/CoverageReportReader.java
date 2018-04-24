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

package org.wso2.testcoverageenforcer.Maven.Jacoco;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.wso2.testcoverageenforcer.Application;
import org.wso2.testcoverageenforcer.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * This class represents a jacoco coverage report with a relevant jacoco execution data file and capable of
 * getting line coverage information
 */
public class CoverageReportReader {

    private final String title;

    private final File executionDataFile;
    private final File classesDirectory;

    private ExecFileLoader execFileLoader;

    private IBundleCoverage bundleCoverage;

    private static final Logger log = Logger.getLogger(Application.class);

    /**
     * Create a new reader for the jacoco report file
     *
     * @param projectBuildDirectory Project build folder containing classes files and jacoco exec report file
     * @throws FileNotFoundException Cannot Find any jacoco execution file
     */
    public CoverageReportReader(final File projectBuildDirectory) throws IOException {

        this.title = new File(projectBuildDirectory.getPath().replace(Constants.Build.BUILD_TARGET_FOLDER, Constants.EMPTY_STRING)).getName();
        Iterator<File> executionFiles = FileUtils.iterateFiles(projectBuildDirectory, new String[]{Constants.Build.BUILD_EXECUTION_FILE}, true);
        if (!(executionFiles.hasNext())) {
            log.error("Jacoco execution file missing in " + projectBuildDirectory);
            throw new FileNotFoundException();
        }
        this.executionDataFile = executionFiles.next();
        this.classesDirectory = new File(projectBuildDirectory, Constants.Build.BUILD_CLASSES_FOLDER);
        create();
    }

    public static void main(String[] args) throws Exception {

        CoverageReportReader reader = new CoverageReportReader(new File("/home/tharindu/Jenkins_Test/Wso2_repos/siddhi-store-hazelcast/component/target"));
        System.out.println(reader.getCoverageThresholdForBundle());
    }

    /**
     * Create the report.
     *
     * @throws IOException Error reading Jacoco execution data file
     */
    private void create() throws IOException {

        // Read the jacoco.exec file. Multiple data files could be merged
        // at this point
        loadExecutionData();

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        bundleCoverage = analyzeStructure();
    }

    private void loadExecutionData() throws IOException {

        execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
    }

    private IBundleCoverage analyzeStructure() throws IOException {

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        return coverageBuilder.getBundle(title);
    }

    /**
     * Get an overall line coverage ratio value from all packages for the project
     *
     * @return Overall covered ratio
     */
    public double getCoverageThresholdForBundle() {

        return this.bundleCoverage.getLineCounter().getCoveredRatio();
    }

    /**
     * Get a collection of coverages for each package and return the minimum value
     *
     * @return Minimum coverage ratio among bundle packages
     */
    public double getCoverageThresholdForPackages() {

        Iterator<IPackageCoverage> packageIterator = this.bundleCoverage.getPackages().iterator();
        double minimumCoverageRatio = 1;
        while (packageIterator.hasNext()) {
            IPackageCoverage packageCoverage = packageIterator.next();
            if (packageCoverage.getLineCounter().getCoveredRatio() < minimumCoverageRatio) {
                minimumCoverageRatio = packageCoverage.getLineCounter().getCoveredRatio();
            }
        }
        return minimumCoverageRatio;
    }

    /**
     * Return project name
     *
     * @return Name of this project
     */
    public String getProjectName() {

        return this.title;
    }
}
