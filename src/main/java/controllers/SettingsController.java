package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import models.AppTheme;
import models.PreferredEditor;
import models.PreferredTerminal;
import services.ApplicationCoordinator;
import utils.ButtonFactory;
import utils.DialogUtils;

public class SettingsController {
    @FXML
    private ComboBox<PreferredEditor> editorComboBox;
    @FXML
    private ComboBox<PreferredTerminal> terminalComboBox;
    @FXML
    private ComboBox<AppTheme> themeComboBox;
    @FXML
    private Button saveSettingsButton;

    private ApplicationCoordinator coordinator;
    private Stage ownerStage;

    public void initialize(ApplicationCoordinator coordinator, Stage ownerStage) {
        this.coordinator = coordinator;
        this.ownerStage = ownerStage;

        editorComboBox.setItems(FXCollections.observableArrayList(PreferredEditor.values()));
        terminalComboBox.setItems(FXCollections.observableArrayList(PreferredTerminal.values()));
        themeComboBox.setItems(FXCollections.observableArrayList(AppTheme.values()));

        var viewModel = coordinator.getSettingsViewModel();
        editorComboBox.valueProperty().bindBidirectional(viewModel.preferredEditorProperty());
        terminalComboBox.valueProperty().bindBidirectional(viewModel.preferredTerminalProperty());
        themeComboBox.valueProperty().bindBidirectional(viewModel.themeProperty());

        ButtonFactory.decorate(saveSettingsButton, "fas-save");
        saveSettingsButton.setTooltip(new Tooltip("Save editor, terminal, and theme preferences"));
    }

    @FXML
    private void onSaveSettings() {
        try {
            coordinator.getSettingsViewModel().save();
            Scene scene = ownerStage.getScene();
            coordinator.getSettingsService().applyTheme(scene, coordinator.getSettingsViewModel().toSettings());
            DialogUtils.showInfo(ownerStage, "Settings Saved", "Preferences updated", "Your settings were saved successfully.");
        } catch (Exception exception) {
            DialogUtils.showError(ownerStage, "Settings Failed", "Unable to save settings", exception.getMessage());
        }
    }
}
