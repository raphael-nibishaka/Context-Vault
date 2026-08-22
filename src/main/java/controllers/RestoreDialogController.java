package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.ContextEntry;
import services.ApplicationCoordinator;
import services.RestoreResult;
import utils.ButtonFactory;
import utils.DialogUtils;
import viewmodels.RestoreDialogViewModel;

public class RestoreDialogController {
    @FXML
    private Label projectNameLabel;
    @FXML
    private Label gitBranchLabel;
    @FXML
    private Label detectedBranchLabel;
    @FXML
    private Label projectPathLabel;
    @FXML
    private HBox statusBanner;
    @FXML
    private Label statusIconLabel;
    @FXML
    private Label statusHeadlineLabel;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label restoreChecklistLabel;
    @FXML
    private VBox warningsPanel;
    @FXML
    private TextArea commandsArea;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label warningsLabel;
    @FXML
    private Button copyCommandsButton;
    @FXML
    private Button runAllButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button headerCloseButton;

    private final RestoreDialogViewModel viewModel = new RestoreDialogViewModel();
    private ApplicationCoordinator coordinator;
    private ContextEntry contextEntry;

    public void initialize(ApplicationCoordinator coordinator, ContextEntry contextEntry, RestoreResult restoreResult) {
        this.coordinator = coordinator;
        this.contextEntry = contextEntry;
        viewModel.setContext(contextEntry, restoreResult);

        projectNameLabel.textProperty().bind(viewModel.projectNameProperty());
        gitBranchLabel.textProperty().bind(viewModel.gitBranchProperty());
        detectedBranchLabel.textProperty().bind(viewModel.detectedBranchProperty());
        projectPathLabel.textProperty().bind(viewModel.projectPathProperty());
        statusHeadlineLabel.textProperty().bind(viewModel.statusHeadlineProperty());
        statusMessageLabel.textProperty().bind(viewModel.statusMessageProperty());
        restoreChecklistLabel.textProperty().bind(viewModel.restoreChecklistProperty());
        commandsArea.textProperty().bind(viewModel.commandsProperty());
        notesArea.textProperty().bind(viewModel.notesProperty());
        warningsLabel.textProperty().bind(viewModel.warningsProperty());

        warningsPanel.managedProperty().bind(warningsPanel.visibleProperty());
        warningsPanel.visibleProperty().bind(viewModel.warningsProperty().isNotEmpty());
        runAllButton.managedProperty().bind(runAllButton.visibleProperty());
        runAllButton.visibleProperty().bind(viewModel.hasCommandsProperty());

        viewModel.hasWarningsProperty().addListener((observable, oldValue, hasWarnings) -> applyStatusStyle(hasWarnings));
        applyStatusStyle(viewModel.hasWarningsProperty().get());

        ButtonFactory.decorate(copyCommandsButton, "fas-copy");
        ButtonFactory.decorate(runAllButton, "fas-play");
        ButtonFactory.decorate(closeButton, "fas-check");
        copyCommandsButton.setTooltip(new Tooltip("Copy saved commands to clipboard"));
        runAllButton.setTooltip(new Tooltip("Run all saved commands in the terminal"));
        closeButton.setTooltip(new Tooltip("Close restore panel"));
        if (headerCloseButton != null) {
            headerCloseButton.setTooltip(new Tooltip("Close"));
        }
    }

    private void applyStatusStyle(boolean hasWarnings) {
        statusBanner.getStyleClass().removeAll("restore-status-success", "restore-status-warning");
        if (hasWarnings) {
            statusBanner.getStyleClass().add("restore-status-warning");
            statusIconLabel.setText("!");
        } else {
            statusBanner.getStyleClass().add("restore-status-success");
            statusIconLabel.setText("✓");
        }
    }

    @FXML
    private void onCopyCommands() {
        coordinator.getClipboardService().copyText(commandsArea.getText());
    }

    @FXML
    private void onRunAllCommands() {
        try {
            coordinator.getRestoreService().runSavedCommands(contextEntry);
            DialogUtils.showInfo(
                    (Stage) projectNameLabel.getScene().getWindow(),
                    "Commands Started",
                    "Saved commands launched",
                    "Your saved commands were started in the configured terminal."
            );
        } catch (IllegalArgumentException exception) {
            DialogUtils.showError(
                    (Stage) projectNameLabel.getScene().getWindow(),
                    "No Commands",
                    "Nothing to run",
                    exception.getMessage()
            );
        } catch (Exception exception) {
            DialogUtils.showError(
                    (Stage) projectNameLabel.getScene().getWindow(),
                    "Run Failed",
                    "Unable to start commands",
                    exception.getMessage()
            );
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) projectNameLabel.getScene().getWindow();
        stage.close();
    }
}
