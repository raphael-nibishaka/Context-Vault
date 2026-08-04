package viewmodels;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.ContextEntry;
import services.ContextService;

import java.util.List;

public class DashboardViewModel {
    private final ContextService contextService;
    private final ObservableList<ContextEntry> contexts = FXCollections.observableArrayList();
    private final StringProperty pageTitle = new SimpleStringProperty("Dashboard");

    public DashboardViewModel(ContextService contextService) {
        this.contextService = contextService;
    }

    public void loadContexts() {
        contexts.setAll(contextService.findAll());
    }

    public void search(String query) {
        contexts.setAll(contextService.search(query));
    }

    public ObservableList<ContextEntry> getContexts() {
        return contexts;
    }

    public StringProperty pageTitleProperty() {
        return pageTitle;
    }

    public void setPageTitle(String title) {
        pageTitle.set(title);
    }

    public List<ContextEntry> snapshot() {
        return List.copyOf(contexts);
    }
}
