package controllers;

import config.Page;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import services.ApplicationCoordinator;
import utils.AnimationUtils;
import utils.ButtonFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class MainController {
    @FXML
    private HBox titleBar;
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
    private Button debugMemoryButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Button topNewContextButton;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button maximizeButton;
    @FXML
    private Button closeButton;

    private final Map<Page, Button> navigationButtons = new EnumMap<>(Page.class);
    private Stage primaryStage;
    private ApplicationCoordinator coordinator;
    private DashboardController dashboardController;
    private DebugMemoryController debugMemoryController;

    private double dragOffsetX;
    private double dragOffsetY;
    private boolean maximized;
    private double restoreX;
    private double restoreY;
    private double restoreWidth;
    private double restoreHeight;

    public void initialize(Stage primaryStage, ApplicationCoordinator coordinator) {
        this.primaryStage = primaryStage;
        this.coordinator = coordinator;

        navigationButtons.put(Page.DASHBOARD, dashboardButton);
        navigationButtons.put(Page.CONTEXTS, contextsButton);
        navigationButtons.put(Page.CREATE_CONTEXT, createContextButton);
        navigationButtons.put(Page.DEBUG_MEMORY, debugMemoryButton);
        navigationButtons.put(Page.SETTINGS, settingsButton);
        navigationButtons.put(Page.ABOUT, aboutButton);

        configureButtons();
        configureWindowControls();
        configureActions();
        configureBindings();
    }

    public void onShown(Scene scene) {
        coordinator.initialize();
        loadPage(coordinator.getMainViewModel().getCurrentPage(), scene);
    }

    private void configureButtons() {
        ButtonFactory.decorate(dashboardButton, "fas-chart-line");
        ButtonFactory.decorate(contextsButton, "fas-folder-open");
        ButtonFactory.decorate(createContextButton, "fas-plus");
        ButtonFactory.decorate(debugMemoryButton, "fas-bug");
        ButtonFactory.decorate(settingsButton, "fas-cog");
        ButtonFactory.decorate(aboutButton, "fas-info-circle");
        ButtonFactory.decorate(topNewContextButton, "fas-plus");

        dashboardButton.setTooltip(new Tooltip("Open dashboard overview"));
        contextsButton.setTooltip(new Tooltip("Browse all saved contexts"));
        createContextButton.setTooltip(new Tooltip("Save a new project context"));
        debugMemoryButton.setTooltip(new Tooltip("Browse debugging history and saved fixes"));
        settingsButton.setTooltip(new Tooltip("Editor, terminal, and theme"));
        aboutButton.setTooltip(new Tooltip("About Context Vault"));
        topNewContextButton.setTooltip(new Tooltip("Create a new context"));
        minimizeButton.setTooltip(new Tooltip("Minimize"));
        maximizeButton.setTooltip(new Tooltip("Maximize"));
        closeButton.setTooltip(new Tooltip("Close"));

        if (!topNewContextButton.getStyleClass().contains("primary-button")) {
            topNewContextButton.getStyleClass().add("primary-button");
        }
        if (!topNewContextButton.getStyleClass().contains("glass-button")) {
            topNewContextButton.getStyleClass().add("glass-button");
        }
    }

    private void configureWindowControls() {
        minimizeButton.setOnAction(event -> primaryStage.setIconified(true));
        maximizeButton.setOnAction(event -> toggleMaximize());
        closeButton.setOnAction(event -> primaryStage.close());

        titleBar.setOnMousePressed(this::beginDrag);
        titleBar.setOnMouseDragged(this::dragWindow);
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private void beginDrag(MouseEvent event) {
        if (maximized) {
            return;
        }
        dragOffsetX = event.getSceneX();
        dragOffsetY = event.getSceneY();
    }

    private void dragWindow(MouseEvent event) {
        if (maximized) {
            return;
        }
        primaryStage.setX(event.getScreenX() - dragOffsetX);
        primaryStage.setY(event.getScreenY() - dragOffsetY);
    }

    private void toggleMaximize() {
        if (!maximized) {
            restoreX = primaryStage.getX();
            restoreY = primaryStage.getY();
            restoreWidth = primaryStage.getWidth();
            restoreHeight = primaryStage.getHeight();

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            maximized = true;
            maximizeButton.setText("❐");
            maximizeButton.setTooltip(new Tooltip("Restore"));
        } else {
            primaryStage.setX(restoreX);
            primaryStage.setY(restoreY);
            primaryStage.setWidth(restoreWidth);
            primaryStage.setHeight(restoreHeight);
            maximized = false;
            maximizeButton.setText("□");
            maximizeButton.setTooltip(new Tooltip("Maximize"));
        }
    }

    private void configureActions() {
        dashboardButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.DASHBOARD));
        contextsButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.CONTEXTS));
        createContextButton.setOnAction(event -> coordinator.createContext());
        debugMemoryButton.setOnAction(event -> coordinator.getMainViewModel().navigate(Page.DEBUG_MEMORY));
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
            if (debugMemoryController != null) {
                debugMemoryController.applySearch(newValue);
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
                    debugMemoryController = null;
                }
                case CREATE_CONTEXT -> {
                    ContextFormController controller = loader.getController();
                    controller.initialize(coordinator, primaryStage);
                    dashboardController = null;
                    debugMemoryController = null;
                }
                case DEBUG_MEMORY -> {
                    debugMemoryController = loader.getController();
                    debugMemoryController.initialize(coordinator, primaryStage);
                    debugMemoryController.applySearch(searchField.getText());
                    dashboardController = null;
                }
                case DEBUG_FORM -> {
                    DebugFormController controller = loader.getController();
                    controller.initialize(coordinator, primaryStage);
                    dashboardController = null;
                    debugMemoryController = null;
                }
                case SETTINGS -> {
                    SettingsController controller = loader.getController();
                    controller.initialize(coordinator, primaryStage);
                    dashboardController = null;
                    debugMemoryController = null;
                }
                case ABOUT -> {
                    AboutController controller = loader.getController();
                    controller.initialize();
                    dashboardController = null;
                    debugMemoryController = null;
                }
            }

            contentContainer.getChildren().setAll(view);
            AnimationUtils.fadeIn(view);
            pageTitleLabel.setText(page.getTitle());
            boolean searchable = page == Page.DASHBOARD || page == Page.CONTEXTS || page == Page.DEBUG_MEMORY;
            searchField.setDisable(!searchable);
            searchField.setPromptText(page == Page.DEBUG_MEMORY
                    ? "Search debugging history..."
                    : "Search by name, branch, or path");
            boolean showNewContext = page == Page.DASHBOARD || page == Page.CONTEXTS;
            topNewContextButton.setVisible(showNewContext);
            topNewContextButton.setManaged(showNewContext);
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
