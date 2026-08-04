package viewmodels;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.ContextEntry;

public class RestoreDialogViewModel {
    private final StringProperty projectName = new SimpleStringProperty();
    private final StringProperty gitBranch = new SimpleStringProperty();
    private final StringProperty projectPath = new SimpleStringProperty();
    private final StringProperty commands = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final StringProperty warnings = new SimpleStringProperty();

    public void setContext(ContextEntry contextEntry, String warningText) {
        projectName.set(contextEntry.getName());
        gitBranch.set(contextEntry.getGitBranch());
        projectPath.set(contextEntry.getProjectPath());
        commands.set(contextEntry.getCommands());
        notes.set(contextEntry.getNote());
        warnings.set(warningText == null ? "" : warningText);
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty projectPathProperty() {
        return projectPath;
    }

    public StringProperty commandsProperty() {
        return commands;
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public StringProperty warningsProperty() {
        return warnings;
    }
}
