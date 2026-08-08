package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import models.ContextEntry;
import services.ApplicationCoordinator;
import utils.ButtonFactory;
import viewmodels.RestoreDialogViewModel;

public class RestoreDialogController {
    @FXML
    private Label projectNameLabel;
    @FXML
    private Label gitBranchLabel;
    @FXML
    private Label projectPathLabel;
    @FXML
    private TextArea commandsArea;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label warningsLabel;
    @FXML
    private Button copyCommandsButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button headerCloseButton;

    private final RestoreDialogViewModel viewModel = new RestoreDialogViewModel();
    private ApplicationCoordinator coordinator;

    public void initialize(ApplicationCoordinator coordinator, ContextEntry contextEntry, String warnings) {
        this.coordinator = coordinator;
        viewModel.setContext(contextEntry, warnings);

        projectNameLabel.textProperty().bind(viewModel.projectNameProperty());
        gitBranchLabel.textProperty().bind(viewModel.gitBranchProperty());
        projectPathLabel.textProperty().bind(viewModel.projectPathProperty());
        commandsArea.textProperty().bind(viewModel.commandsProperty());
        notesArea.textProperty().bind(viewModel.notesProperty());
        warningsLabel.textProperty().bind(viewModel.warningsProperty());
        warningsLabel.managedProperty().bind(warningsLabel.visibleProperty());
        warningsLabel.visibleProperty().bind(viewModel.warningsProperty().isNotEmpty());

        ButtonFactory.decorate(copyCommandsButton, "fas-copy");
        ButtonFactory.decorate(closeButton, "fas-check");
        copyCommandsButton.setTooltip(new Tooltip("Copy saved commands to clipboard"));
        closeButton.setTooltip(new Tooltip("Close restore panel"));
        if (headerCloseButton != null) {
            headerCloseButton.setTooltip(new Tooltip("Close"));
        }
    }

    @FXML
    private void onCopyCommands() {
        coordinator.getClipboardService().copyText(commandsArea.getText());
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) projectNameLabel.getScene().getWindow();
        stage.close();
    }
}
