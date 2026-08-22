package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitRepositoryInfo {
    private boolean gitRepository;
    private String repositoryRoot = "";
    private String repositoryName = "";
    private String branch = "";
    private String remoteUrl = "";
    private String currentCommit = "";
    private String lastCommitMessage = "";
    private final List<String> modifiedFiles = new ArrayList<>();
    private final List<String> untrackedFiles = new ArrayList<>();
    private final List<String> stagedFiles = new ArrayList<>();

    public static GitRepositoryInfo empty() {
        return new GitRepositoryInfo();
    }

    public boolean isGitRepository() {
        return gitRepository;
    }

    public void setGitRepository(boolean gitRepository) {
        this.gitRepository = gitRepository;
    }

    public String getRepositoryRoot() {
        return repositoryRoot;
    }

    public void setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getCurrentCommit() {
        return currentCommit;
    }

    public void setCurrentCommit(String currentCommit) {
        this.currentCommit = currentCommit;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage) {
        this.lastCommitMessage = lastCommitMessage;
    }

    public List<String> getModifiedFiles() {
        return Collections.unmodifiableList(modifiedFiles);
    }

    public void setModifiedFiles(List<String> modifiedFiles) {
        this.modifiedFiles.clear();
        if (modifiedFiles != null) {
            this.modifiedFiles.addAll(modifiedFiles);
        }
    }

    public List<String> getUntrackedFiles() {
        return Collections.unmodifiableList(untrackedFiles);
    }

    public void setUntrackedFiles(List<String> untrackedFiles) {
        this.untrackedFiles.clear();
        if (untrackedFiles != null) {
            this.untrackedFiles.addAll(untrackedFiles);
        }
    }

    public List<String> getStagedFiles() {
        return Collections.unmodifiableList(stagedFiles);
    }

    public void setStagedFiles(List<String> stagedFiles) {
        this.stagedFiles.clear();
        if (stagedFiles != null) {
            this.stagedFiles.addAll(stagedFiles);
        }
    }
}
