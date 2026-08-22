package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
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
    private TextField projectNameField;
    @FXML
    private TextField projectPathField;
    @FXML
    private TextField gitRepoPathField;
    @FXML
    private TextField gitBranchField;
    @FXML
    private TextArea openFilesArea;
    @FXML
    private TextArea commandsArea;
    @FXML
    private TextArea notesArea;
    @FXML
    private TextField tagsField;
    @FXML
    private TextArea browserUrlsArea;
    @FXML
    private Label validationLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button browseButton;
    @FXML
    private Button refreshGitButton;
    @FXML
    private Button cancelButton;
    @FXML
    private VBox gitPanel;
    @FXML
    private Label gitRepositoryNameLabel;
    @FXML
    private Label gitRemoteUrlLabel;
    @FXML
    private Label gitCurrentCommitLabel;
    @FXML
    private Label gitLastCommitLabel;
    @FXML
    private Label gitModifiedFilesLabel;
    @FXML
    private Label gitUntrackedFilesLabel;
    @FXML
    private Label gitStagedFilesLabel;

    private ApplicationCoordinator coordinator;
    private Stage ownerStage;

    public void initialize(ApplicationCoordinator coordinator, Stage ownerStage) {
        this.coordinator = coordinator;
        this.ownerStage = ownerStage;

        var viewModel = coordinator.getContextFormViewModel();
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        projectNameField.textProperty().bindBidirectional(viewModel.projectNameProperty());
        projectPathField.textProperty().bindBidirectional(viewModel.projectPathProperty());
        gitRepoPathField.textProperty().bindBidirectional(viewModel.gitRepoPathProperty());
        gitBranchField.textProperty().bindBidirectional(viewModel.gitBranchProperty());
        openFilesArea.textProperty().bindBidirectional(viewModel.openFilesProperty());
        commandsArea.textProperty().bindBidirectional(viewModel.commandsProperty());
        notesArea.textProperty().bindBidirectional(viewModel.notesProperty());
        tagsField.textProperty().bindBidirectional(viewModel.tagsProperty());
        browserUrlsArea.textProperty().bindBidirectional(viewModel.browserUrlsProperty());
        validationLabel.textProperty().bind(viewModel.validationMessageProperty());

        gitRepositoryNameLabel.textProperty().bind(viewModel.gitRepositoryNameProperty());
        gitRemoteUrlLabel.textProperty().bind(viewModel.gitRemoteUrlProperty());
        gitCurrentCommitLabel.textProperty().bind(viewModel.gitCurrentCommitProperty());
        gitLastCommitLabel.textProperty().bind(viewModel.gitLastCommitProperty());
        gitModifiedFilesLabel.textProperty().bind(viewModel.gitModifiedFilesProperty());
        gitUntrackedFilesLabel.textProperty().bind(viewModel.gitUntrackedFilesProperty());
        gitStagedFilesLabel.textProperty().bind(viewModel.gitStagedFilesProperty());

        gitPanel.visibleProperty().bind(viewModel.gitRepositoryProperty());
        gitPanel.managedProperty().bind(viewModel.gitRepositoryProperty());

        projectPathField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                viewModel.onProjectPathChanged();
            }
        });

        ButtonFactory.decorate(browseButton, "fas-folder-open");
        ButtonFactory.decorate(refreshGitButton, "fas-sync");
        ButtonFactory.decorate(cancelButton, "fas-times");
        ButtonFactory.decorate(saveButton, "fas-save");
        browseButton.setTooltip(new Tooltip("Choose project folder"));
        refreshGitButton.setTooltip(new Tooltip("Refresh Git repository information"));
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
            coordinator.getContextFormViewModel().onProjectPathChanged();
        }
    }

    @FXML
    private void onRefreshGit() {
        coordinator.getContextFormViewModel().refreshGitInfo();
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
