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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.ContextEntry;
import org.kordamp.ikonli.javafx.FontIcon;
import services.ApplicationCoordinator;
import services.RestoreResult;
import utils.AnimationUtils;
import utils.DateTimeUtils;
import utils.DialogUtils;

import java.io.IOException;
import java.util.StringJoiner;

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
        summaryLabel.setText(contexts.size() + " saved context" + (contexts.size() == 1 ? "" : "s"));
        emptyStateLabel.setVisible(contexts.isEmpty());
        emptyStateLabel.setManaged(contexts.isEmpty());

        for (ContextEntry contextEntry : contexts) {
            VBox card = createCard(contextEntry);
            cardsPane.getChildren().add(card);
        }
        AnimationUtils.fadeIn(cardsScrollPane);
    }

    private VBox createCard(ContextEntry contextEntry) {
        Label branchChip = new Label(blankFallback(contextEntry.getGitBranch()));
        branchChip.getStyleClass().add("branch-chip");

        Label titleLabel = new Label(contextEntry.getName());
        titleLabel.getStyleClass().add("card-title");

        Label branchLabel = new Label("Current branch");
        branchLabel.getStyleClass().add("card-label");

        Label pathLabel = new Label(contextEntry.getProjectPath());
        pathLabel.getStyleClass().add("card-path");
        pathLabel.setWrapText(true);

        Label notePreview = new Label(buildPreview(contextEntry.getNote(), "No saved note for this context."));
        notePreview.getStyleClass().add("card-note");
        notePreview.setWrapText(true);

        Label createdLabel = new Label("Created: " + DateTimeUtils.format(contextEntry.getCreatedAt()));
        createdLabel.getStyleClass().add("card-meta");

        Label updatedLabel = new Label("Updated: " + DateTimeUtils.format(contextEntry.getUpdatedAt()));
        updatedLabel.getStyleClass().add("card-meta");

        Button openButton = createActionButton("Open", "fas-play");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(event -> restoreContext(contextEntry));

        Button editButton = createActionButton("Edit", "fas-pen");
        editButton.setOnAction(event -> coordinator.editContext(contextEntry.copy()));

        Button deleteButton = createActionButton("Delete", "fas-trash");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteContext(contextEntry));

        HBox actions = new HBox(10, openButton, editButton, deleteButton);
        HBox topRow = new HBox(12, branchChip);

        VBox card = new VBox(12, topRow, titleLabel, branchLabel, pathLabel, notePreview, createdLabel, updatedLabel, actions);
        card.getStyleClass().add("context-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(340);
        card.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> AnimationUtils.pulse(card));
        return card;
    }

    private Button createActionButton(String text, String iconLiteral) {
        Button button = new Button(text, new FontIcon(iconLiteral));
        button.getStyleClass().add("secondary-button");
        return button;
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
            controller.initialize(coordinator, contextEntry, buildWarningText(result));

            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Restore Context");
            Scene scene = new Scene(root, 680, 520);
            coordinator.getSettingsService().applyTheme(scene, coordinator.getSettingsService().loadSettings());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open restore dialog", exception);
        }
    }

    private String buildWarningText(RestoreResult result) {
        if (!result.hasWarnings()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        result.getWarnings().forEach(joiner::add);
        return joiner.toString();
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
        return value == null || value.isBlank() ? "Not specified" : value;
    }

    private String buildPreview(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.replace('\n', ' ').trim();
        return normalized.length() > 120 ? normalized.substring(0, 117) + "..." : normalized;
    }
}
