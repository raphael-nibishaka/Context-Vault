package app;

import config.AppMetadata;
import config.ThemeManager;
import controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import services.ApplicationCoordinator;
import services.ServiceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextVaultApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextVaultApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        ServiceContainer serviceContainer = new ServiceContainer();
        ApplicationCoordinator coordinator = serviceContainer.getApplicationCoordinator();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.initialize(primaryStage, coordinator);

        Scene scene = new Scene(root, 1400, 860);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ThemeManager themeManager = coordinator.getThemeManager();
        themeManager.applyTheme(scene, coordinator.getSettingsService().loadSettings().theme());

        primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        primaryStage.setTitle(AppMetadata.APP_NAME);
        primaryStage.getIcons().setAll(
                new Image(getClass().getResourceAsStream("/images/context-vault-logo.png"))
        );
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.show();

        controller.onShown(scene);
        LOGGER.info("Context Vault started successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
