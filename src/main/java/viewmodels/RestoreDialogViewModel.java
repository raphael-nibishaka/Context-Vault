package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.ContextEntry;
import models.RestoreStep;
import models.RestoreStepStatus;
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
    private final StringProperty restoreChecklist = new SimpleStringProperty();
    private final StringProperty warnings = new SimpleStringProperty();
    private final BooleanProperty hasWarnings = new SimpleBooleanProperty();
    private final BooleanProperty hasCommands = new SimpleBooleanProperty();

    public void setContext(ContextEntry contextEntry, RestoreResult restoreResult) {
        projectName.set(safe(contextEntry.getName(), "Untitled context"));
        gitBranch.set(formatBranch(contextEntry.getGitBranch()));
        projectPath.set(safe(contextEntry.getProjectPath(), "No path saved"));
        commands.set(safe(contextEntry.getCommands(), ""));
        notes.set(safe(contextEntry.getNote(), "No notes for this context."));
        hasCommands.set(contextEntry.getCommands() != null && !contextEntry.getCommands().isBlank());

        if (restoreResult != null) {
            detectedBranch.set(formatBranch(restoreResult.getDetectedGitBranch()));
            hasWarnings.set(restoreResult.hasWarnings());
            statusHeadline.set(restoreResult.hasWarnings() ? "Workspace restored with warnings" : "Workspace restored");
            statusMessage.set(buildStatusMessage(restoreResult));
            restoreChecklist.set(buildChecklist(restoreResult));
            warnings.set(buildWarnings(restoreResult));
        } else {
            detectedBranch.set("Not detected");
            hasWarnings.set(false);
            statusHeadline.set("Context ready");
            statusMessage.set("Review the saved details below.");
            restoreChecklist.set("");
            warnings.set("");
        }
    }

    private String buildStatusMessage(RestoreResult restoreResult) {
        if (restoreResult.hasWarnings()) {
            return "Your workspace was reconstructed, but one or more steps need attention.";
        }
        return "Project, Git, editor, files, terminal, browser, and notes were restored.";
    }

    private String buildChecklist(RestoreResult restoreResult) {
        if (restoreResult.getSteps().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Restoring...").append(System.lineSeparator()).append(System.lineSeparator());
        for (RestoreStep step : restoreResult.getSteps()) {
            builder.append(formatStep(step)).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("Workspace restored.");
        return builder.toString().trim();
    }

    private String formatStep(RestoreStep step) {
        String icon = switch (step.status()) {
            case SUCCESS -> "✓";
            case WARNING -> "!";
            case SKIPPED -> "–";
        };
        return icon + " " + step.label();
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

    public StringProperty restoreChecklistProperty() {
        return restoreChecklist;
    }

    public StringProperty warningsProperty() {
        return warnings;
    }

    public BooleanProperty hasWarningsProperty() {
        return hasWarnings;
    }

    public BooleanProperty hasCommandsProperty() {
        return hasCommands;
    }
}
