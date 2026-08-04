package viewmodels;

import config.Page;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MainViewModel {
    private final ObjectProperty<Page> currentPage = new SimpleObjectProperty<>(Page.DASHBOARD);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    public void navigate(Page page) {
        currentPage.set(page);
    }

    public ObjectProperty<Page> currentPageProperty() {
        return currentPage;
    }

    public Page getCurrentPage() {
        return currentPage.get();
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public String getSearchQuery() {
        return searchQuery.get();
    }
}
