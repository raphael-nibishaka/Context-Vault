package controllers;

import config.Page;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ApplicationCoordinator;
import utils.AnimationUtils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class MainController {
    @FXML
    private Label pageTitleLabel;
    @FXML
    private TextField searchField;
    @FXML
    private StackPane contentContainer;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button contextsButton;
    @FXML
    private Button createContextButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Button topNewContextButton;

    private final Map<Page, Button> navigationButtons = new EnumMap<>(Page.class);
    private Stage primaryStage;
    private ApplicationCoordinator coordinator;
    private DashboardController dashboardController;

    public void initialize(Stage primaryStage, ApplicationCoordinator coordinator) {
        this.primaryStage = primaryStage;
        this.coordinator = coordinator;

        navigationButtons.put(Page.DASHBOARD, dashboardButton);
        navigationButtons.put(Page.CONTEXTS, contextsButton);
        navigationButtons.put(Page.CREATE_CONTEXT, createContextButton);
        navigationButtons.put(Page.SETTINGS, settingsButton);
        navigationButtons.put(Page.ABOUT, aboutButton);

        configureIcons();
        configureActions();
        configureBindings();
    }

    public void onShown(Scene scene) {
        coordinator.initialize();
        loadPage(coordinator.getMainViewModel().getCurrentPage(), scene);
    }

    private void configureIcons() {
        dashboardButton.setGraphic(new FontIcon("fas-chart-line"));
        contextsButton.setGraphic(new FontIcon("fas-folder-open"));
        createContextButton.setGraphic(new FontIcon("fas-plus"));
        settingsButton.setGraphic(new FontIcon("fas-cog"));
        aboutButton.setGraphic(new FontIcon("fas-info-circle"));
        topNewContextButton.setGraphic(new FontIcon("fas-plus"));
    }

    private void configureActions() {
        dashboardButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.DASHBOARD));
        contextsButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.CONTEXTS));
        createContextButton.setOnAction(event -> coordinator.createContext());
        settingsButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.SETTINGS));
        aboutButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.ABOUT));
        topNewContextButton.setOnAction(event -> coordinator.createContext());
    }

    private void configureBindings() {
        coordinator.getMainViewModel().currentPageProperty().addListener((observable, oldPage, newPage) -> {
            if (primaryStage.getScene() != null) {
                loadPage(newPage, primaryStage.getScene());
            }
        });

        searchField.textProperty().bindBidirectional(coordinator.getMainViewModel().searchQueryProperty());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (dashboardController != null) {
                dashboardController.applySearch(newValue);
            }
        });
    }

    private void loadPage(Page page, Scene scene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(page.getFxmlPath()));
            Parent view = loader.load();

            switch (page) {
                case DASHBOARD, CONTEXTS -> {
                    dashboardController = loader.getController();
                    dashboardController.initialize(coordinator, primaryStage, page.getTitle());
                    dashboardController.applySearch(searchField.getText());
                }
                case CREATE_CONTEXT -> {
                    ContextFormController controller = loader.getController();
                    controller.initialize(coordinator, primaryStage);
                    dashboardController = null;
                }
                case SETTINGS -> {
                    SettingsController controller = loader.getController();
                    controller.initialize(coordinator, primaryStage);
                    dashboardController = null;
                }
                case ABOUT -> {
                    AboutController controller = loader.getController();
                    controller.initialize();
                    dashboardController = null;
                }
            }

            contentContainer.getChildren().setAll(view);
            AnimationUtils.fadeIn(view);
            pageTitleLabel.setText(page.getTitle());
            searchField.setDisable(!(page == Page.DASHBOARD || page == Page.CONTEXTS));
            searchField.setPromptText(page == Page.CREATE_CONTEXT ? "Search contexts" : "Search contexts by name, branch, or path");
            setActiveButton(page);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load page: " + page, exception);
        }
    }

    private void setActiveButton(Page page) {
        navigationButtons.values().forEach(button -> button.getStyleClass().remove("nav-button-active"));
        Button activeButton = navigationButtons.get(page);
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }
}
