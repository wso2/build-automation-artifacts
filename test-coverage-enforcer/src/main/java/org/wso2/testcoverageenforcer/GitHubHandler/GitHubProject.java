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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.wso2.testcoverageenforcer.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Represent a Git project capable of from forking to make a pull request
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
     * Initialize
     *
     * @param repositoryName repository name as github calls it('username/reponame')
     */
    public GitHubProject(String repositoryName, String login, String password, String email) throws IOException {

        this.login = login;
        this.password = password;
        this.email = email;

        GitHub github = GitHub.connectUsingPassword(this.login, this.password);
        this.projectRepository = github.getRepository(repositoryName);
    }

    /**
     * Fork a this project to a specified git user
     */
    public void gitFork() throws IOException {

        this.forkedRepository = this.projectRepository.fork();
    }

    /**
     * Delete forked repo
     */
    public void gitDeleteForked() throws IOException {

        this.forkedRepository.delete();
    }

    /**
     * To clone this repository. If forked, this will clone from the forked user and set cloned directory path
     */
    public void gitClone() throws GitAPIException,IOException {

        this.cloneFolder = new File(this.workspacePath + File.separator + this.forkedRepository.getFullName());
        if (!cloneFolder.exists()) {
            cloneFolder.mkdir();
        }
        else {
            FileUtils.cleanDirectory(cloneFolder);
        }

        this.clonedRepository = Git.cloneRepository()
                .setURI(Constants.GITHUB_URL + this.forkedRepository.getFullName() + ".git")
                .setDirectory(cloneFolder)
                .call();
    }

    /**
     * Clear cloned folder
     */
    public void gitClearClonedFolder() throws IOException{

        if (this.cloneFolder.exists()) {
            FileUtils.cleanDirectory(cloneFolder);
        }
    }

    /**
     * Commit cloned repository
     */
    public void gitCommit(String committingMessage) throws GitAPIException{

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
     */
    public void gitPush() throws GitAPIException{

        this.clonedRepository.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(this.login, this.password))
                .call();
    }

    /**
     * Request a PR for the forked repository
     */
    public void gitPullRequest() throws IOException{

        this.projectRepository.createPullRequest(
                Constants.GIT_PR_TITLE,
                this.forkedRepository.getOwnerName() + Constants.COLON + Constants.GIT_PR_MASTER,
                Constants.GIT_PR_MASTER,
                Constants.GIT_PR_BODY);
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
     * get full name of the repository
     */
    public String getRepositoryFullName(String repoType) {

        switch (repoType) {
            case "original":
                return this.projectRepository.getFullName();
            case "forked":
                return this.forkedRepository.getFullName();
            default:
                return null;
        }
    }
}
