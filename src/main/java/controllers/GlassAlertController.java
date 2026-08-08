package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import utils.ButtonFactory;

public class GlassAlertController {
    public enum AlertKind {
        INFO,
        ERROR,
        CONFIRM
    }

    @FXML
    private Label titleLabel;
    @FXML
    private Label headerLabel;
    @FXML
    private Label contentLabel;
    @FXML
    private Label iconLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button headerCloseButton;

    private boolean confirmed;

    public void configure(AlertKind kind, String title, String header, String content, boolean showCancel) {
        titleLabel.setText(title == null ? "Context Vault" : title);
        headerLabel.setText(header == null ? "" : header);
        contentLabel.setText(content == null ? "" : content);

        switch (kind) {
            case ERROR -> {
                iconLabel.setText("!");
                iconLabel.getStyleClass().setAll("glass-alert-icon", "glass-alert-icon-error");
                confirmButton.setText("Close");
                ButtonFactory.decorate(confirmButton, "fas-times");
            }
            case CONFIRM -> {
                iconLabel.setText("?");
                iconLabel.getStyleClass().setAll("glass-alert-icon", "glass-alert-icon-confirm");
                confirmButton.setText("OK");
                ButtonFactory.decorate(confirmButton, "fas-check");
            }
            default -> {
                iconLabel.setText("i");
                iconLabel.getStyleClass().setAll("glass-alert-icon", "glass-alert-icon-info");
                confirmButton.setText("OK");
                ButtonFactory.decorate(confirmButton, "fas-check");
            }
        }

        cancelButton.setVisible(showCancel);
        cancelButton.setManaged(showCancel);
        if (showCancel) {
            ButtonFactory.decorate(cancelButton, "fas-times");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        close();
    }

    private void close() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}
