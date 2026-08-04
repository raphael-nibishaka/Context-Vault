package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.ContextEntry;
import services.ContextService;
import utils.ValidationResult;

import java.time.LocalDateTime;

public class ContextFormViewModel {
    private final ContextService contextService;
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty projectPath = new SimpleStringProperty("");
    private final StringProperty gitBranch = new SimpleStringProperty("");
    private final StringProperty commands = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");
    private final StringProperty validationMessage = new SimpleStringProperty("");
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);

    private long editingId;
    private LocalDateTime createdAt;

    public ContextFormViewModel(ContextService contextService) {
        this.contextService = contextService;
    }

    public void prepareForCreate() {
        editingId = 0L;
        createdAt = null;
        editMode.set(false);
        name.set("");
        projectPath.set("");
        gitBranch.set("");
        commands.set("");
        notes.set("");
        validationMessage.set("");
    }

    public void editContext(ContextEntry contextEntry) {
        editingId = contextEntry.getId();
        createdAt = contextEntry.getCreatedAt();
        editMode.set(true);
        name.set(contextEntry.getName());
        projectPath.set(contextEntry.getProjectPath());
        gitBranch.set(contextEntry.getGitBranch());
        commands.set(contextEntry.getCommands());
        notes.set(contextEntry.getNote());
        validationMessage.set("");
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
                safe(projectPath.get()),
                safe(gitBranch.get()),
                safe(notes.get()),
                safe(commands.get())
        );
        entry.setId(editingId);
        if (createdAt != null) {
            entry.setCreatedAt(createdAt);
        }
        return entry;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty projectPathProperty() {
        return projectPath;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty commandsProperty() {
        return commands;
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public StringProperty validationMessageProperty() {
        return validationMessage;
    }

    public BooleanProperty editModeProperty() {
        return editMode;
    }
}
