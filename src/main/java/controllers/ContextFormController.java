package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;
import models.ContextEntry;
import services.ApplicationCoordinator;
import utils.ButtonFactory;
import utils.DialogUtils;

import java.io.File;

public class ContextFormController {
    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField projectPathField;
    @FXML
    private TextField gitBranchField;
    @FXML
    private TextArea commandsArea;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label validationLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button browseButton;
    @FXML
    private Button cancelButton;

    private ApplicationCoordinator coordinator;
    private Stage ownerStage;

    public void initialize(ApplicationCoordinator coordinator, Stage ownerStage) {
        this.coordinator = coordinator;
        this.ownerStage = ownerStage;

        var viewModel = coordinator.getContextFormViewModel();
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        projectPathField.textProperty().bindBidirectional(viewModel.projectPathProperty());
        gitBranchField.textProperty().bindBidirectional(viewModel.gitBranchProperty());
        commandsArea.textProperty().bindBidirectional(viewModel.commandsProperty());
        notesArea.textProperty().bindBidirectional(viewModel.notesProperty());
        validationLabel.textProperty().bind(viewModel.validationMessageProperty());

        ButtonFactory.decorate(browseButton, "fas-folder-open");
        ButtonFactory.decorate(cancelButton, "fas-times");
        ButtonFactory.decorate(saveButton, "fas-save");
        browseButton.setTooltip(new Tooltip("Choose project folder"));
        cancelButton.setTooltip(new Tooltip("Discard changes"));
        saveButton.setTooltip(new Tooltip("Save this context"));

        refreshFormLabels();
        viewModel.editModeProperty().addListener((observable, oldValue, newValue) -> refreshFormLabels());
    }

    @FXML
    private void onBrowseProjectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Project Folder");
        File directory = chooser.showDialog(ownerStage);
        if (directory != null) {
            projectPathField.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    private void onSaveContext() {
        try {
            ContextEntry savedEntry = coordinator.getContextFormViewModel().save();
            coordinator.refreshContexts();
            coordinator.getContextFormViewModel().prepareForCreate();
            coordinator.getMainViewModel().navigate(config.Page.DASHBOARD);
            DialogUtils.showInfo(
                    ownerStage,
                    "Context Saved",
                    savedEntry.getName(),
                    "Your project context has been saved successfully."
            );
        } catch (IllegalArgumentException exception) {
            validationLabel.setText(exception.getMessage());
        } catch (Exception exception) {
            DialogUtils.showError(
                    ownerStage,
                    "Save Failed",
                    "Unable to save context",
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void onCancel() {
        coordinator.getContextFormViewModel().prepareForCreate();
        coordinator.getMainViewModel().navigate(config.Page.DASHBOARD);
    }

    private void refreshFormLabels() {
        boolean editMode = coordinator.getContextFormViewModel().editModeProperty().get();
        formTitleLabel.setText(editMode ? "Edit Context" : "Create Context");
        saveButton.setText(editMode ? "Update Context" : "Save Context");
    }
}
