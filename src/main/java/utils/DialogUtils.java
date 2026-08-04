package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void showInfo(Window owner, String title, String header, String content) {
        Alert alert = createAlert(owner, Alert.AlertType.INFORMATION, title, header, content);
        alert.showAndWait();
    }

    public static void showError(Window owner, String title, String header, String content) {
        Alert alert = createAlert(owner, Alert.AlertType.ERROR, title, header, content);
        alert.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String header, String content) {
        Alert alert = createAlert(owner, Alert.AlertType.CONFIRMATION, title, header, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static Alert createAlert(Window owner, Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }
}
