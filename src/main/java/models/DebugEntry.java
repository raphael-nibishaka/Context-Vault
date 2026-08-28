package models;

import java.time.LocalDateTime;
import java.util.Objects;

public class DebugEntry {
    private long id;
    private String errorMessage;
    private String stackTrace;
    private String errorType;
    private String projectName;
    private String projectPath;
    private String sourceFile;
    private String solution;
    private String fixCommand;
    private String relatedContext;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DebugEntry() {
    }

    public DebugEntry(long id,
                      String errorMessage,
                      String stackTrace,
                      String errorType,
                      String projectName,
                      String projectPath,
                      String sourceFile,
                      String solution,
                      String fixCommand,
                      String relatedContext,
                      String tags,
                      LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
        this.id = id;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.errorType = errorType;
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.sourceFile = sourceFile;
        this.solution = solution;
        this.fixCommand = fixCommand;
        this.relatedContext = relatedContext;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DebugEntry newEntry(String errorMessage,
                                      String stackTrace,
                                      String errorType,
                                      String projectName,
                                      String projectPath,
                                      String sourceFile,
                                      String solution,
                                      String fixCommand,
                                      String relatedContext,
                                      String tags) {
        LocalDateTime now = LocalDateTime.now();
        return new DebugEntry(
                0L,
                errorMessage,
                stackTrace,
                errorType,
                projectName,
                projectPath,
                sourceFile,
                solution,
                fixCommand,
                relatedContext,
                tags,
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
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

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getFixCommand() {
        return fixCommand;
    }

    public void setFixCommand(String fixCommand) {
        this.fixCommand = fixCommand;
    }

    public String getRelatedContext() {
        return relatedContext;
    }

    public void setRelatedContext(String relatedContext) {
        this.relatedContext = relatedContext;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public String displayTitle() {
        if (errorType != null && !errorType.isBlank()) {
            return errorType;
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown error";
        }
        String firstLine = errorMessage.lines().findFirst().orElse(errorMessage).trim();
        return firstLine.length() > 72 ? firstLine.substring(0, 69) + "..." : firstLine;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DebugEntry that)) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
