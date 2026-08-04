package models;

import java.time.LocalDateTime;
import java.util.Objects;

public class ContextEntry {
    private long id;
    private String name;
    private String projectPath;
    private String gitBranch;
    private String note;
    private String commands;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContextEntry() {
    }

    public ContextEntry(long id, String name, String projectPath, String gitBranch, String note, String commands,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.projectPath = projectPath;
        this.gitBranch = gitBranch;
        this.note = note;
        this.commands = commands;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContextEntry newEntry(String name, String projectPath, String gitBranch, String note, String commands) {
        LocalDateTime now = LocalDateTime.now();
        return new ContextEntry(0L, name, projectPath, gitBranch, note, commands, now, now);
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

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
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
        return new ContextEntry(id, name, projectPath, gitBranch, note, commands, createdAt, updatedAt);
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
                && Objects.equals(projectPath, that.projectPath)
                && Objects.equals(gitBranch, that.gitBranch)
                && Objects.equals(note, that.note)
                && Objects.equals(commands, that.commands)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, projectPath, gitBranch, note, commands, createdAt, updatedAt);
    }
}
