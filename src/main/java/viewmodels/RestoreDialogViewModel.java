package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.ContextEntry;
import services.RestoreResult;

public class RestoreDialogViewModel {
    private final StringProperty projectName = new SimpleStringProperty();
    private final StringProperty gitBranch = new SimpleStringProperty();
    private final StringProperty detectedBranch = new SimpleStringProperty();
    private final StringProperty projectPath = new SimpleStringProperty();
    private final StringProperty commands = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final StringProperty statusHeadline = new SimpleStringProperty();
    private final StringProperty statusMessage = new SimpleStringProperty();
    private final StringProperty actionsSummary = new SimpleStringProperty();
    private final StringProperty warnings = new SimpleStringProperty();
    private final BooleanProperty hasWarnings = new SimpleBooleanProperty();

    public void setContext(ContextEntry contextEntry, RestoreResult restoreResult) {
        projectName.set(safe(contextEntry.getName(), "Untitled context"));
        gitBranch.set(formatBranch(contextEntry.getGitBranch()));
        projectPath.set(safe(contextEntry.getProjectPath(), "No path saved"));
        commands.set(safe(contextEntry.getCommands(), "No saved commands."));
        notes.set(safe(contextEntry.getNote(), "No notes for this context."));

        if (restoreResult != null) {
            detectedBranch.set(formatBranch(restoreResult.getDetectedGitBranch()));
            hasWarnings.set(restoreResult.hasWarnings());
            statusHeadline.set(restoreResult.hasWarnings() ? "Restored with warnings" : "Context restored");
            statusMessage.set(buildStatusMessage(restoreResult));
            actionsSummary.set(buildActionsSummary(restoreResult));
            warnings.set(buildWarnings(restoreResult));
        } else {
            detectedBranch.set("Not detected");
            hasWarnings.set(false);
            statusHeadline.set("Context ready");
            statusMessage.set("Review the saved details below.");
            actionsSummary.set("");
            warnings.set("");
        }
    }

    private String buildStatusMessage(RestoreResult restoreResult) {
        if (restoreResult.hasWarnings()) {
            return "Your workspace was opened, but one or more steps need attention.";
        }
        return "Your project folder, editor, and terminal were launched successfully.";
    }

    private String buildActionsSummary(RestoreResult restoreResult) {
        if (restoreResult.getInfoMessages().isEmpty()) {
            return "";
        }
        return restoreResult.getInfoMessages().stream()
                .map(message -> "• " + message)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String buildWarnings(RestoreResult restoreResult) {
        if (!restoreResult.hasWarnings()) {
            return "";
        }
        return restoreResult.getWarnings().stream()
                .map(warning -> "• " + warning)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String formatBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return "Not set";
        }
        return branch.trim();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public StringProperty gitBranchProperty() {
        return gitBranch;
    }

    public StringProperty detectedBranchProperty() {
        return detectedBranch;
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

    public StringProperty statusHeadlineProperty() {
        return statusHeadline;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty actionsSummaryProperty() {
        return actionsSummary;
    }

    public StringProperty warningsProperty() {
        return warnings;
    }

    public BooleanProperty hasWarningsProperty() {
        return hasWarnings;
    }
}
