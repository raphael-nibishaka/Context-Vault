package controllers;

import config.Page;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import services.ApplicationCoordinator;
import utils.ButtonFactory;

import java.io.File;

public class DebugFormController {
    @FXML
    private Label formTitleLabel;
    @FXML
    private TextArea errorMessageArea;
    @FXML
    private TextArea stackTraceArea;
    @FXML
    private TextField errorTypeField;
    @FXML
    private TextField projectNameField;
    @FXML
    private TextField projectPathField;
    @FXML
    private TextField sourceFileField;
    @FXML
    private TextArea solutionArea;
    @FXML
    private TextField fixCommandField;
    @FXML
    private TextArea relatedContextArea;
    @FXML
    private TextField tagsField;
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

        var viewModel = coordinator.getDebugFormViewModel();
        errorMessageArea.textProperty().bindBidirectional(viewModel.errorMessageProperty());
        stackTraceArea.textProperty().bindBidirectional(viewModel.stackTraceProperty());
        errorTypeField.textProperty().bindBidirectional(viewModel.errorTypeProperty());
        projectNameField.textProperty().bindBidirectional(viewModel.projectNameProperty());
        projectPathField.textProperty().bindBidirectional(viewModel.projectPathProperty());
        sourceFileField.textProperty().bindBidirectional(viewModel.sourceFileProperty());
        solutionArea.textProperty().bindBidirectional(viewModel.solutionProperty());
        fixCommandField.textProperty().bindBidirectional(viewModel.fixCommandProperty());
        relatedContextArea.textProperty().bindBidirectional(viewModel.relatedContextProperty());
        tagsField.textProperty().bindBidirectional(viewModel.tagsProperty());
        validationLabel.textProperty().bind(viewModel.validationMessageProperty());

        errorMessageArea.textProperty().addListener((observable, oldValue, newValue) -> viewModel.onErrorTextChanged());
        stackTraceArea.textProperty().addListener((observable, oldValue, newValue) -> viewModel.onErrorTextChanged());
        viewModel.editModeProperty().addListener((observable, oldValue, newValue) -> refreshFormLabels());

        ButtonFactory.decorate(browseButton, "fas-folder-open");
        ButtonFactory.decorate(cancelButton, "fas-times");
        ButtonFactory.decorate(saveButton, "fas-save");
        browseButton.setTooltip(new Tooltip("Choose project folder"));
        cancelButton.setTooltip(new Tooltip("Discard changes"));
        saveButton.setTooltip(new Tooltip("Save this debug fix"));

        refreshFormLabels();
    }

    @FXML
    private void onBrowseProjectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose project folder");
        File selected = chooser.showDialog(ownerStage);
        if (selected != null) {
            coordinator.getDebugFormViewModel().projectPathProperty().set(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onSave() {
        if (coordinator.getDebugFormViewModel().save()) {
            coordinator.refreshDebugEntries();
            coordinator.getMainViewModel().navigate(Page.DEBUG_MEMORY);
        }
    }

    @FXML
    private void onCancel() {
        coordinator.getMainViewModel().navigate(config.Page.DEBUG_MEMORY);
    }

    private void refreshFormLabels() {
        boolean editing = coordinator.getDebugFormViewModel().editModeProperty().get();
        formTitleLabel.setText(editing ? "Edit Debug Fix" : "Log Debug Fix");
        saveButton.setText(editing ? "Update Fix" : "Save Fix");
    }
}
