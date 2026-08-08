package utils;

import controllers.GlassAlertController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void showInfo(Window owner, String title, String header, String content) {
        show(owner, GlassAlertController.AlertKind.INFO, title, header, content, false);
    }

    public static void showError(Window owner, String title, String header, String content) {
        show(owner, GlassAlertController.AlertKind.ERROR, title, header, content, false);
    }

    public static boolean confirm(Window owner, String title, String header, String content) {
        return show(owner, GlassAlertController.AlertKind.CONFIRM, title, header, content, true);
    }

    private static boolean show(Window owner,
                                GlassAlertController.AlertKind kind,
                                String title,
                                String header,
                                String content,
                                boolean showCancel) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource("/fxml/glass-alert-view.fxml"));
            Parent root = loader.load();
            GlassAlertController controller = loader.getController();
            controller.configure(kind, title, header, content, showCancel);

            Stage dialogStage = new Stage();
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setTitle(title);

            Scene scene = new Scene(root, 500, 280);
            scene.setFill(Color.TRANSPARENT);
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().setAll(owner.getScene().getStylesheets());
            } else {
                scene.getStylesheets().add(DialogUtils.class.getResource("/css/base.css").toExternalForm());
                scene.getStylesheets().add(DialogUtils.class.getResource("/css/theme-dark.css").toExternalForm());
            }

            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            return controller.isConfirmed();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open glass dialog", exception);
        }
    }
}
