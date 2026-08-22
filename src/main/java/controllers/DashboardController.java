package controllers;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.ContextEntry;
import services.ApplicationCoordinator;
import services.RestoreResult;
import utils.AnimationUtils;
import utils.ButtonFactory;
import utils.CommandParser;
import utils.DateTimeUtils;
import utils.DialogUtils;
import utils.FileListParser;

import java.io.IOException;

public class DashboardController {
    @FXML
    private Label sectionTitleLabel;
    @FXML
    private Label summaryLabel;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private ScrollPane cardsScrollPane;

    private ApplicationCoordinator coordinator;
    private Stage ownerStage;

    public void initialize(ApplicationCoordinator coordinator, Stage ownerStage, String title) {
        this.coordinator = coordinator;
        this.ownerStage = ownerStage;
        sectionTitleLabel.setText(title);
        coordinator.getDashboardViewModel().setPageTitle(title);
        coordinator.getDashboardViewModel().getContexts().addListener((ListChangeListener<ContextEntry>) change -> renderCards());
        renderCards();
    }

    public void applySearch(String query) {
        coordinator.getDashboardViewModel().search(query);
    }

    private void renderCards() {
        cardsPane.getChildren().clear();
        var contexts = coordinator.getDashboardViewModel().getContexts();
        summaryLabel.setText(contexts.size() + " saved context" + (contexts.size() == 1 ? "" : "s") + " ready to restore");
        emptyStateLabel.setVisible(contexts.isEmpty());
        emptyStateLabel.setManaged(contexts.isEmpty());

        for (ContextEntry contextEntry : contexts) {
            cardsPane.getChildren().add(createCard(contextEntry));
        }
        AnimationUtils.fadeIn(cardsScrollPane);
    }

    private VBox createCard(ContextEntry contextEntry) {
        Label branchChip = new Label(blankFallback(contextEntry.getGitBranch()));
        branchChip.getStyleClass().add("branch-chip");

        int fileCount = FileListParser.count(contextEntry.getOpenFiles());
        int commandCount = CommandParser.parse(contextEntry.getCommands()).size();
        Label statsChip = new Label(fileCount + " file" + (fileCount == 1 ? "" : "s")
                + " • " + commandCount + " command" + (commandCount == 1 ? "" : "s"));
        statsChip.getStyleClass().add("branch-chip");

        Label titleLabel = new Label(contextEntry.getName());
        titleLabel.getStyleClass().add("card-title");

        Label projectLabel = new Label(blankFallback(contextEntry.getProjectName()));
        projectLabel.getStyleClass().add("card-subtitle");

        Label pathLabel = new Label(contextEntry.getProjectPath());
        pathLabel.getStyleClass().add("card-path");
        pathLabel.setWrapText(true);

        Label notePreview = new Label(buildPreview(contextEntry.getNote(), "No notes saved for this session."));
        notePreview.getStyleClass().add("card-note");
        notePreview.setWrapText(true);

        Label updatedLabel = new Label("Updated  " + DateTimeUtils.format(contextEntry.getUpdatedAt()));
        updatedLabel.getStyleClass().add("card-meta");

        Button restoreButton = ButtonFactory.primary("Restore", "fas-play", "Restore this workspace");
        restoreButton.setOnAction(event -> restoreContext(contextEntry));

        Button editButton = ButtonFactory.secondary("Edit", "fas-pen", "Edit context details");
        editButton.setOnAction(event -> coordinator.editContext(contextEntry.copy()));

        Button deleteButton = ButtonFactory.danger("Delete", "fas-trash", "Delete this context");
        deleteButton.setOnAction(event -> deleteContext(contextEntry));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, branchChip, statsChip);
        HBox actions = new HBox(8, restoreButton, editButton, spacer, deleteButton);
        actions.getStyleClass().add("card-actions");

        Region divider = new Region();
        divider.getStyleClass().add("card-divider");

        VBox card = new VBox(12, topRow, titleLabel, projectLabel, pathLabel, notePreview, divider, updatedLabel, actions);
        card.getStyleClass().add("context-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(360);
        card.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> AnimationUtils.pulse(card));
        return card;
    }

    private void restoreContext(ContextEntry contextEntry) {
        try {
            RestoreResult result = coordinator.getRestoreService().restore(contextEntry);
            showRestoreDialog(contextEntry, result);
        } catch (IllegalArgumentException exception) {
            DialogUtils.showError(ownerStage, "Restore Failed", "Project folder unavailable", exception.getMessage());
        }
    }

    private void showRestoreDialog(ContextEntry contextEntry, RestoreResult result) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/restore-dialog-view.fxml"));
            Parent root = loader.load();

            RestoreDialogController controller = loader.getController();
            controller.initialize(coordinator, contextEntry, result);

            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            dialogStage.setTitle("Restore Context");
            Scene scene = new Scene(root, 760, 720);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            coordinator.getSettingsService().applyTheme(scene, coordinator.getSettingsService().loadSettings());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open restore dialog", exception);
        }
    }

    private void deleteContext(ContextEntry contextEntry) {
        boolean confirmed = DialogUtils.confirm(
                ownerStage,
                "Delete Context",
                "Delete " + contextEntry.getName() + "?",
                "This action cannot be undone."
        );
        if (!confirmed) {
            return;
        }

        coordinator.getContextService().delete(contextEntry.getId());
        coordinator.refreshContexts();
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "unspecified" : value;
    }

    private String buildPreview(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.replace('\n', ' ').trim();
        return normalized.length() > 120 ? normalized.substring(0, 117) + "..." : normalized;
    }
}
