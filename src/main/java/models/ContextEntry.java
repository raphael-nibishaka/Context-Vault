package models;

import java.time.LocalDateTime;
import java.util.Objects;

public class ContextEntry {
    private long id;
    private String name;
    private String projectName;
    private String projectPath;
    private String gitRepoPath;
    private String gitBranch;
    private String openFiles;
    private String note;
    private String commands;
    private String tags;
    private String browserUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContextEntry() {
    }

    public ContextEntry(long id,
                        String name,
                        String projectName,
                        String projectPath,
                        String gitRepoPath,
                        String gitBranch,
                        String openFiles,
                        String note,
                        String commands,
                        String tags,
                        String browserUrls,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.gitRepoPath = gitRepoPath;
        this.gitBranch = gitBranch;
        this.openFiles = openFiles;
        this.note = note;
        this.commands = commands;
        this.tags = tags;
        this.browserUrls = browserUrls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContextEntry newEntry(String name,
                                        String projectName,
                                        String projectPath,
                                        String gitRepoPath,
                                        String gitBranch,
                                        String openFiles,
                                        String note,
                                        String commands,
                                        String tags,
                                        String browserUrls) {
        LocalDateTime now = LocalDateTime.now();
        return new ContextEntry(
                0L,
                name,
                projectName,
                projectPath,
                gitRepoPath,
                gitBranch,
                openFiles,
                note,
                commands,
                tags,
                browserUrls,
                now,
                now
        );
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getGitRepoPath() {
        return gitRepoPath;
    }

    public void setGitRepoPath(String gitRepoPath) {
        this.gitRepoPath = gitRepoPath;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getOpenFiles() {
        return openFiles;
    }

    public void setOpenFiles(String openFiles) {
        this.openFiles = openFiles;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = commands;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getBrowserUrls() {
        return browserUrls;
    }

    public void setBrowserUrls(String browserUrls) {
        this.browserUrls = browserUrls;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ContextEntry copy() {
        return new ContextEntry(
                id,
                name,
                projectName,
                projectPath,
                gitRepoPath,
                gitBranch,
                openFiles,
                note,
                commands,
                tags,
                browserUrls,
                createdAt,
                updatedAt
        );
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ContextEntry that)) {
            return false;
        }
        return id == that.id
                && Objects.equals(name, that.name)
                && Objects.equals(projectName, that.projectName)
                && Objects.equals(projectPath, that.projectPath)
                && Objects.equals(gitRepoPath, that.gitRepoPath)
                && Objects.equals(gitBranch, that.gitBranch)
                && Objects.equals(openFiles, that.openFiles)
                && Objects.equals(note, that.note)
                && Objects.equals(commands, that.commands)
                && Objects.equals(tags, that.tags)
                && Objects.equals(browserUrls, that.browserUrls)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                projectName,
                projectPath,
                gitRepoPath,
                gitBranch,
                openFiles,
                note,
                commands,
                tags,
                browserUrls,
                createdAt,
                updatedAt
        );
    }
}
