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

package org.wso2.testcoverageenforcer.GitHubHandler;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.wso2.testcoverageenforcer.Constants;
import org.wso2.testcoverageenforcer.FileHandler.TemplateReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Properties;

/**
 * Represent a Git project
 */
public class GitHubProject {

    /**
     * Credentials
     */
    private String login;
    private String password;
    private String email;

    /**
     * gitHub repository object
     */
    private GHRepository projectRepository;

    /**
     * Forked repository object
     */
    private GHRepository forkedRepository;

    /**
     * Cloned repository object and cloned folder
     */
    private Git clonedRepository;
    private File cloneFolder;

    /**
     * Path to the directory where repository will get cloned
     */
    private String workspacePath = "./temp";

    /**
     * Class constructor
     *
     * @param repositoryName repository name as github calls it('username/reponame')
     * @throws IOException Error while connecting with GitHub
     */
    public GitHubProject(String repositoryName, String propertiesFilePath) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFilePath));

        this.login = properties.getProperty(Constants.GIT_USERNAME);
        this.password = properties.getProperty(Constants.GIT_PASSWORD);
        this.email = properties.getProperty(Constants.GIT_EMAIL);

        GitHub github = GitHub.connectUsingPassword(this.login, this.password);
        this.projectRepository = github.getRepository(repositoryName);
    }

    /**
     * Fork a this project to a specified git user
     *
     * @throws IOException Error while interacting with GitHub
     */
    public void gitFork() throws IOException {

        this.forkedRepository = this.projectRepository.fork();
    }

    /**
     * Delete forked repo
     *
     * @throws IOException Error while interacting with GitHub
     */
    public void gitDeleteForked() throws IOException {

        this.forkedRepository.delete();
    }

    /**
     * To clone this repository. If forked, this will clone from the forked user and set cloned directory path
     */
    public void gitClone() throws GitAPIException, IOException {

        this.cloneFolder = new File(this.workspacePath + File.separator + this.forkedRepository.getFullName());
        if (!cloneFolder.exists()) {
            boolean mkdirStatus = cloneFolder.mkdir();
            if (!mkdirStatus) throw new IOException();
        } else {
            FileUtils.cleanDirectory(cloneFolder);
        }

        this.clonedRepository = Git.cloneRepository()
                .setURI(Constants.GITHUB_URL + this.forkedRepository.getFullName() + ".git")
                .setDirectory(cloneFolder)
                .call();
    }

    /**
     * Commit cloned repository
     *
     * @throws GitAPIException Error during git operation
     */
    public void gitCommit(String committingMessage) throws GitAPIException {

        this.clonedRepository.add()
                .addFilepattern(".")
                .call();
        this.clonedRepository.commit()
                .setMessage(committingMessage)
                .setAuthor(this.login, this.email)
                .call();
    }

    /**
     * Push cloned repo to the forked repository master
     *
     * @throws IOException     Error while interacting with GitHub
     * @throws GitAPIException Error during git operation
     */
    public void gitPush() throws GitAPIException, IOException {

        this.clonedRepository.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(this.login, this.password))
                .call();
        FileUtils.deleteDirectory(cloneFolder);
    }

    /**
     * Request a PR for the forked repository
     *
     * @throws IOException Error while interacting with GitHub
     */
    public void gitPullRequest() throws IOException {

        Path tempFile = Files.createTempFile("pr_message_temp", ".txt");
        try (InputStream stream = TemplateReader.class.getClassLoader().getResourceAsStream(Constants.GIT_PR_BODY)) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        byte[] encoded = Files.readAllBytes(tempFile);
        String pullRequestMessage = new String(encoded);
        this.projectRepository.createPullRequest(
                Constants.GIT_PR_TITLE,
                this.forkedRepository.getOwnerName() + Constants.COLON + Constants.GIT_PR_MASTER,
                Constants.GIT_PR_MASTER,
                pullRequestMessage);
    }

    /**
     * If a clone operation happened, this will retrieve cloned project path
     */
    public String getClonedPath() {

        return this.cloneFolder.getPath();
    }

    /**
     * Set repository workspace path
     */
    public void setWorkspace(String workspace) {

        this.workspacePath = workspace;
    }

    /**
     * Check whether this repository is active within a given period of time
     *
     * @param timePeriodOfInterest Up to which number of months activity is considered
     * @throws IOException Error occurred during date fetching
     */
    public boolean getActiveStatus(int timePeriodOfInterest) throws IOException {

        //Check a Fixed number of commits ordered from latest to oldest and count the number of recent commits
        PagedIterable<GHCommit> commitsList = this.projectRepository.listCommits();
        short recentCommitCount = 0;
        short commitsCount = 0;
        Calendar currentTime = Calendar.getInstance();
        for (GHCommit commit : commitsList) {
            if ((commit.getAuthor() != null)) {
                if (commit.getAuthor().getLogin().equals(Constants.GIT_JENKINS_BOT)) continue;
            }
            if (commitsCount == Constants.GITHUB_COMMITS_OF_INTEREST_COUNT) {
                break;
            }
            Calendar commitTime = Calendar.getInstance();
            commitTime.setTime(commit.getCommitDate());
            int yearDifference = currentTime.get(Calendar.YEAR) - commitTime.get(Calendar.YEAR);
            int monthsDifference = yearDifference * 12 + currentTime.get(Calendar.MONTH) - commitTime.get(Calendar.MONTH);
            if (monthsDifference < timePeriodOfInterest) {
                recentCommitCount++;
            }
            commitsCount++;
        }
        return recentCommitCount >= Constants.GITHUB_RECENT_COMMITS_THRESHOLD;
    }
}
