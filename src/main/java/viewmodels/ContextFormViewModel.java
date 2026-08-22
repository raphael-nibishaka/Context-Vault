package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.ContextEntry;
import models.GitRepositoryInfo;
import services.ContextService;
import services.GitService;
import utils.ValidationResult;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class ContextFormViewModel {
    private final ContextService contextService;
    private final GitService gitService;
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty projectName = new SimpleStringProperty("");
    private final StringProperty projectPath = new SimpleStringProperty("");
    private final StringProperty gitRepoPath = new SimpleStringProperty("");
    private final StringProperty gitBranch = new SimpleStringProperty("");
    private final StringProperty openFiles = new SimpleStringProperty("");
    private final StringProperty commands = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");
    private final StringProperty tags = new SimpleStringProperty("");
    private final StringProperty browserUrls = new SimpleStringProperty("");
    private final StringProperty validationMessage = new SimpleStringProperty("");
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);
    private final BooleanProperty gitRepository = new SimpleBooleanProperty(false);
    private final StringProperty gitRepositoryName = new SimpleStringProperty("");
    private final StringProperty gitRemoteUrl = new SimpleStringProperty("");
    private final StringProperty gitCurrentCommit = new SimpleStringProperty("");
    private final StringProperty gitLastCommit = new SimpleStringProperty("");
    private final StringProperty gitModifiedFiles = new SimpleStringProperty("");
    private final StringProperty gitUntrackedFiles = new SimpleStringProperty("");
    private final StringProperty gitStagedFiles = new SimpleStringProperty("");

    private long editingId;
    private LocalDateTime createdAt;

    public ContextFormViewModel(ContextService contextService, GitService gitService) {
        this.contextService = contextService;
        this.gitService = gitService;
    }

    public void prepareForCreate() {
        editingId = 0L;
        createdAt = null;
        editMode.set(false);
        clearForm();
    }

    public void editContext(ContextEntry contextEntry) {
        editingId = contextEntry.getId();
        createdAt = contextEntry.getCreatedAt();
        editMode.set(true);
        name.set(safe(contextEntry.getName()));
        projectName.set(safe(contextEntry.getProjectName()));
        projectPath.set(safe(contextEntry.getProjectPath()));
        gitRepoPath.set(safe(contextEntry.getGitRepoPath()));
        gitBranch.set(safe(contextEntry.getGitBranch()));
        openFiles.set(safe(contextEntry.getOpenFiles()));
        commands.set(safe(contextEntry.getCommands()));
        notes.set(safe(contextEntry.getNote()));
        tags.set(safe(contextEntry.getTags()));
        browserUrls.set(safe(contextEntry.getBrowserUrls()));
        validationMessage.set("");
        refreshGitInfo();
    }

    public void refreshGitInfo() {
        if (projectPath.get() == null || projectPath.get().isBlank()) {
            clearGitPanel();
            return;
        }

        GitRepositoryInfo info = gitService.inspectRepository(Path.of(projectPath.get().trim()));
        gitRepository.set(info.isGitRepository());
        if (!info.isGitRepository()) {
            clearGitPanel();
            return;
        }

        gitRepoPath.set(info.getRepositoryRoot());
        if (gitBranch.get() == null || gitBranch.get().isBlank()) {
            gitBranch.set(info.getBranch());
        }
        gitRepositoryName.set(info.getRepositoryName());
        gitRemoteUrl.set(info.getRemoteUrl());
        gitCurrentCommit.set(info.getCurrentCommit());
        gitLastCommit.set(info.getLastCommitMessage());
        gitModifiedFiles.set(formatFileList(info.getModifiedFiles()));
        gitUntrackedFiles.set(formatFileList(info.getUntrackedFiles()));
        gitStagedFiles.set(formatFileList(info.getStagedFiles()));
    }

    public void onProjectPathChanged() {
        if (projectName.get() == null || projectName.get().isBlank()) {
            Path path = Path.of(projectPath.get().trim());
            projectName.set(path.getFileName() != null ? path.getFileName().toString() : "");
        }
        refreshGitInfo();
    }

    public ValidationResult validate() {
        ContextEntry draft = toContextEntry();
        ValidationResult result = contextService.validate(draft);
        validationMessage.set(result.message());
        return result;
    }

    public ContextEntry save() {
        ContextEntry draft = toContextEntry();
        ValidationResult result = contextService.validate(draft);
        validationMessage.set(result.message());
        if (!result.valid()) {
            throw new IllegalArgumentException(result.message());
        }
        return editMode.get() ? contextService.update(draft) : contextService.save(draft);
    }

    public ContextEntry toContextEntry() {
        ContextEntry entry = ContextEntry.newEntry(
                safe(name.get()),
                safe(projectName.get()),
                safe(projectPath.get()),
                safe(gitRepoPath.get()),
                safe(gitBranch.get()),
                safe(openFiles.get()),
                safe(notes.get()),
                safe(commands.get()),
                safe(tags.get()),
                safe(browserUrls.get())
        );
        entry.setId(editingId);
        if (createdAt != null) {
            entry.setCreatedAt(createdAt);
        }
        return entry;
    }

    private void clearForm() {
        name.set("");
        projectName.set("");
        projectPath.set("");
        gitRepoPath.set("");
        gitBranch.set("");
        openFiles.set("");
        commands.set("");
        notes.set("");
        tags.set("");
        browserUrls.set("");
        validationMessage.set("");
        clearGitPanel();
    }

    private void clearGitPanel() {
        gitRepository.set(false);
        gitRepositoryName.set("");
        gitRemoteUrl.set("");
        gitCurrentCommit.set("");
        gitLastCommit.set("");
        gitModifiedFiles.set("");
        gitUntrackedFiles.set("");
        gitStagedFiles.set("");
    }

    private String formatFileList(java.util.List<String> files) {
        if (files == null || files.isEmpty()) {
            return "None";
        }
        return files.stream().limit(8).collect(Collectors.joining(System.lineSeparator()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public StringProperty projectPathProperty() {
        return projectPath;
    }

    public StringProperty gitRepoPathProperty() {
        return gitRepoPath;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty openFilesProperty() {
        return openFiles;
    }

    public StringProperty commandsProperty() {
        return commands;
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public StringProperty tagsProperty() {
        return tags;
    }

    public StringProperty browserUrlsProperty() {
        return browserUrls;
    }

    public StringProperty validationMessageProperty() {
        return validationMessage;
    }

    public BooleanProperty editModeProperty() {
        return editMode;
    }

    public BooleanProperty gitRepositoryProperty() {
        return gitRepository;
    }

    public StringProperty gitRepositoryNameProperty() {
        return gitRepositoryName;
    }

    public StringProperty gitRemoteUrlProperty() {
        return gitRemoteUrl;
    }

    public StringProperty gitCurrentCommitProperty() {
        return gitCurrentCommit;
    }

    public StringProperty gitLastCommitProperty() {
        return gitLastCommit;
    }

    public StringProperty gitModifiedFilesProperty() {
        return gitModifiedFiles;
    }

    public StringProperty gitUntrackedFilesProperty() {
        return gitUntrackedFiles;
    }

    public StringProperty gitStagedFilesProperty() {
        return gitStagedFiles;
    }
}
