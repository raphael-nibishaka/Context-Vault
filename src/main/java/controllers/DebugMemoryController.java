package controllers;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.DebugEntry;
import services.ApplicationCoordinator;
import utils.AnimationUtils;
import utils.ButtonFactory;
import utils.DateTimeUtils;
import utils.DialogUtils;

public class DebugMemoryController {
    @FXML
    private Label summaryLabel;
    @FXML
    private VBox similarMatchBanner;
    @FXML
    private Label similarMatchTitleLabel;
    @FXML
    private Label similarMatchSolutionLabel;
    @FXML
    private TextArea probeErrorArea;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private ScrollPane cardsScrollPane;
    @FXML
    private Button logFixButton;
    @FXML
    private Button viewPreviousFixButton;

    private ApplicationCoordinator coordinator;
    private Stage ownerStage;

    public void initialize(ApplicationCoordinator coordinator, Stage ownerStage) {
        this.coordinator = coordinator;
        this.ownerStage = ownerStage;

        var viewModel = coordinator.getDebugMemoryViewModel();
        probeErrorArea.textProperty().bindBidirectional(viewModel.probeErrorTextProperty());
        similarMatchBanner.visibleProperty().bind(viewModel.similarMatchVisibleProperty());
        similarMatchBanner.managedProperty().bind(viewModel.similarMatchVisibleProperty());
        similarMatchTitleLabel.textProperty().bind(viewModel.similarMatchTitleProperty());
        similarMatchSolutionLabel.textProperty().bind(viewModel.similarMatchSolutionProperty());

        probeErrorArea.textProperty().addListener((observable, oldValue, newValue) -> viewModel.refreshSimilarMatch());
        viewModel.getEntries().addListener((ListChangeListener<DebugEntry>) change -> renderCards());

        ButtonFactory.decorate(logFixButton, "fas-bug");
        ButtonFactory.decorate(viewPreviousFixButton, "fas-history");
        logFixButton.setOnAction(event -> coordinator.logDebugFix(probeErrorArea.getText()));
        viewPreviousFixButton.setOnAction(event -> openSimilarMatch(viewModel.similarMatchEntryProperty().get()));

        viewModel.loadEntries();
        renderCards();
    }

    public void applySearch(String query) {
        coordinator.getDebugMemoryViewModel().search(query);
    }

    private void renderCards() {
        cardsPane.getChildren().clear();
        var entries = coordinator.getDebugMemoryViewModel().getEntries();
        summaryLabel.setText(entries.size() + " saved debug fix" + (entries.size() == 1 ? "" : "es") + " in your engineering memory");
        emptyStateLabel.setVisible(entries.isEmpty());
        emptyStateLabel.setManaged(entries.isEmpty());

        for (DebugEntry entry : entries) {
            cardsPane.getChildren().add(createCard(entry));
        }
        AnimationUtils.fadeIn(cardsScrollPane);
    }

    private VBox createCard(DebugEntry entry) {
        Label errorChip = new Label(blankFallback(entry.getErrorType(), entry.displayTitle()));
        errorChip.getStyleClass().add("branch-chip");

        Label projectChip = new Label(blankFallback(entry.getProjectName(), "Unknown project"));
        projectChip.getStyleClass().add("branch-chip");

        Label titleLabel = new Label(entry.displayTitle());
        titleLabel.getStyleClass().add("card-title");

        Label fileLabel = new Label(blankFallback(entry.getSourceFile(), "No file recorded"));
        fileLabel.getStyleClass().add("card-subtitle");

        Label solutionPreview = new Label(buildPreview(entry.getSolution(), "No solution recorded yet."));
        solutionPreview.getStyleClass().add("card-note");
        solutionPreview.setWrapText(true);

        Label fixPreview = new Label(buildPreview(entry.getFixCommand(), ""));
        fixPreview.getStyleClass().add("card-path");
        fixPreview.setWrapText(true);
        fixPreview.setVisible(entry.getFixCommand() != null && !entry.getFixCommand().isBlank());
        fixPreview.setManaged(fixPreview.isVisible());

        Label updatedLabel = new Label("Logged  " + DateTimeUtils.format(entry.getUpdatedAt()));
        updatedLabel.getStyleClass().add("card-meta");

        Button viewButton = ButtonFactory.primary("View Fix", "fas-eye", "Open this debug fix");
        Button editButton = ButtonFactory.secondary("Edit", "fas-edit", "Edit this debug fix");
        Button deleteButton = ButtonFactory.secondary("Delete", "fas-trash", "Delete this debug fix");

        viewButton.setOnAction(event -> showFixDetails(entry));
        editButton.setOnAction(event -> coordinator.editDebugEntry(entry));
        deleteButton.setOnAction(event -> confirmDelete(entry));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox chipRow = new HBox(8, errorChip, projectChip);
        HBox actions = new HBox(10, viewButton, editButton, deleteButton);

        VBox card = new VBox(12,
                chipRow,
                titleLabel,
                fileLabel,
                solutionPreview,
                fixPreview,
                updatedLabel,
                actions
        );
        card.getStyleClass().addAll("context-card", "glass-inset");
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        return card;
    }

    private void openSimilarMatch(DebugEntry entry) {
        if (entry != null) {
            showFixDetails(entry);
        }
    }

    private void showFixDetails(DebugEntry entry) {
        StringBuilder content = new StringBuilder();
        content.append("Error\n").append(entry.getErrorMessage()).append("\n\n");
        if (entry.getStackTrace() != null && !entry.getStackTrace().isBlank()) {
            content.append("Stack trace\n").append(entry.getStackTrace()).append("\n\n");
        }
        content.append("Solution\n").append(blankFallback(entry.getSolution(), "No solution recorded.")).append("\n\n");
        if (entry.getFixCommand() != null && !entry.getFixCommand().isBlank()) {
            content.append("Fix command\n").append(entry.getFixCommand()).append("\n\n");
        }
        if (entry.getRelatedContext() != null && !entry.getRelatedContext().isBlank()) {
            content.append("Related context\n").append(entry.getRelatedContext());
        }
        DialogUtils.showInfo(ownerStage, "Previous Fix", entry.displayTitle(), content.toString());
    }

    private void confirmDelete(DebugEntry entry) {
        boolean confirmed = DialogUtils.confirm(
                ownerStage,
                "Delete Debug Fix",
                "Remove this debugging memory?",
                entry.displayTitle()
        );
        if (confirmed) {
            coordinator.getDebugMemoryViewModel().deleteEntry(entry);
        }
    }

    private String buildPreview(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() > 140 ? trimmed.substring(0, 137) + "..." : trimmed;
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
