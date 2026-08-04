package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import viewmodels.AboutViewModel;

public class AboutController {
    @FXML
    private Label appNameLabel;
    @FXML
    private Label versionLabel;
    @FXML
    private Label developerLabel;
    @FXML
    private Label taglineLabel;

    private final AboutViewModel viewModel = new AboutViewModel();

    public void initialize() {
        appNameLabel.textProperty().bind(viewModel.appNameProperty());
        versionLabel.textProperty().bind(viewModel.versionProperty());
        developerLabel.textProperty().bind(viewModel.developerProperty());
        taglineLabel.textProperty().bind(viewModel.taglineProperty());
    }
}
