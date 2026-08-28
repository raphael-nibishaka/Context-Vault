package viewmodels;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.DebugEntry;
import models.SimilarDebugMatch;
import services.DebugMemoryService;

import java.util.Optional;

public class DebugMemoryViewModel {
    private final DebugMemoryService debugMemoryService;
    private final ObservableList<DebugEntry> entries = FXCollections.observableArrayList();
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final StringProperty probeErrorText = new SimpleStringProperty("");
    private final BooleanProperty similarMatchVisible = new SimpleBooleanProperty(false);
    private final StringProperty similarMatchTitle = new SimpleStringProperty("");
    private final StringProperty similarMatchSolution = new SimpleStringProperty("");
    private final ObjectProperty<DebugEntry> similarMatchEntry = new SimpleObjectProperty<>();

    public DebugMemoryViewModel(DebugMemoryService debugMemoryService) {
        this.debugMemoryService = debugMemoryService;
    }

    public void loadEntries() {
        entries.setAll(debugMemoryService.findAll());
        refreshSimilarMatch();
    }

    public void search(String query) {
        searchQuery.set(query == null ? "" : query);
        entries.setAll(debugMemoryService.search(searchQuery.get()));
    }

    public void refreshSimilarMatch() {
        Optional<SimilarDebugMatch> match = debugMemoryService.findBestSimilarMatch(probeErrorText.get());
        if (match.isEmpty()) {
            similarMatchVisible.set(false);
            similarMatchTitle.set("");
            similarMatchSolution.set("");
            similarMatchEntry.set(null);
            return;
        }

        DebugEntry entry = match.get().entry();
        similarMatchVisible.set(true);
        similarMatchTitle.set(entry.displayTitle());
        similarMatchSolution.set(buildSolutionPreview(entry));
        similarMatchEntry.set(entry);
    }

    public void deleteEntry(DebugEntry entry) {
        debugMemoryService.delete(entry.getId());
        loadEntries();
    }

    public ObservableList<DebugEntry> getEntries() {
        return entries;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public StringProperty probeErrorTextProperty() {
        return probeErrorText;
    }

    public BooleanProperty similarMatchVisibleProperty() {
        return similarMatchVisible;
    }

    public StringProperty similarMatchTitleProperty() {
        return similarMatchTitle;
    }

    public StringProperty similarMatchSolutionProperty() {
        return similarMatchSolution;
    }

    public ObjectProperty<DebugEntry> similarMatchEntryProperty() {
        return similarMatchEntry;
    }

    private String buildSolutionPreview(DebugEntry entry) {
        if (entry.getSolution() != null && !entry.getSolution().isBlank()) {
            return entry.getSolution();
        }
        if (entry.getFixCommand() != null && !entry.getFixCommand().isBlank()) {
            return entry.getFixCommand();
        }
        return "Open the saved fix to review the previous resolution.";
    }
}
