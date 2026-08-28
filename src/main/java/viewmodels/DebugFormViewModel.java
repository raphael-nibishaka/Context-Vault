package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import models.DebugEntry;
import services.DebugMemoryService;

public class DebugFormViewModel {
    private final DebugMemoryService debugMemoryService;
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty stackTrace = new SimpleStringProperty("");
    private final StringProperty errorType = new SimpleStringProperty("");
    private final StringProperty projectName = new SimpleStringProperty("");
    private final StringProperty projectPath = new SimpleStringProperty("");
    private final StringProperty sourceFile = new SimpleStringProperty("");
    private final StringProperty solution = new SimpleStringProperty("");
    private final StringProperty fixCommand = new SimpleStringProperty("");
    private final StringProperty relatedContext = new SimpleStringProperty("");
    private final StringProperty tags = new SimpleStringProperty("");
    private final StringProperty validationMessage = new SimpleStringProperty("");
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);

    private long editingId;

    public DebugFormViewModel(DebugMemoryService debugMemoryService) {
        this.debugMemoryService = debugMemoryService;
    }

    public void prepareForCreate() {
        editingId = 0L;
        editMode.set(false);
        clearFields();
    }

    public void prepareForCreate(String prefilledErrorText) {
        prepareForCreate();
        if (prefilledErrorText != null && !prefilledErrorText.isBlank()) {
            errorMessage.set(prefilledErrorText.trim());
            errorType.set(debugMemoryService.extractErrorType(prefilledErrorText, prefilledErrorText));
        }
    }

    public void editEntry(DebugEntry entry) {
        editingId = entry.getId();
        editMode.set(true);
        errorMessage.set(safe(entry.getErrorMessage()));
        stackTrace.set(safe(entry.getStackTrace()));
        errorType.set(safe(entry.getErrorType()));
        projectName.set(safe(entry.getProjectName()));
        projectPath.set(safe(entry.getProjectPath()));
        sourceFile.set(safe(entry.getSourceFile()));
        solution.set(safe(entry.getSolution()));
        fixCommand.set(safe(entry.getFixCommand()));
        relatedContext.set(safe(entry.getRelatedContext()));
        tags.set(safe(entry.getTags()));
        validationMessage.set("");
    }

    public boolean save() {
        validationMessage.set("");
        DebugEntry entry = buildEntry();
        var validation = debugMemoryService.validate(entry);
        if (!validation.valid()) {
            validationMessage.set(validation.message());
            return false;
        }

        if (editMode.get()) {
            debugMemoryService.update(entry);
        } else {
            debugMemoryService.save(entry);
        }
        return true;
    }

    public void onErrorTextChanged() {
        errorType.set(debugMemoryService.extractErrorType(errorMessage.get(), stackTrace.get()));
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public StringProperty stackTraceProperty() {
        return stackTrace;
    }

    public StringProperty errorTypeProperty() {
        return errorType;
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public StringProperty projectPathProperty() {
        return projectPath;
    }

    public StringProperty sourceFileProperty() {
        return sourceFile;
    }

    public StringProperty solutionProperty() {
        return solution;
    }

    public StringProperty fixCommandProperty() {
        return fixCommand;
    }

    public StringProperty relatedContextProperty() {
        return relatedContext;
    }

    public StringProperty tagsProperty() {
        return tags;
    }

    public StringProperty validationMessageProperty() {
        return validationMessage;
    }

    public BooleanProperty editModeProperty() {
        return editMode;
    }

    private DebugEntry buildEntry() {
        DebugEntry entry = DebugEntry.newEntry(
                errorMessage.get().trim(),
                stackTrace.get().trim(),
                errorType.get().trim(),
                projectName.get().trim(),
                projectPath.get().trim(),
                sourceFile.get().trim(),
                solution.get().trim(),
                fixCommand.get().trim(),
                relatedContext.get().trim(),
                tags.get().trim()
        );
        if (editMode.get()) {
            entry.setId(editingId);
        }
        return entry;
    }

    private void clearFields() {
        errorMessage.set("");
        stackTrace.set("");
        errorType.set("");
        projectName.set("");
        projectPath.set("");
        sourceFile.set("");
        solution.set("");
        fixCommand.set("");
        relatedContext.set("");
        tags.set("");
        validationMessage.set("");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
