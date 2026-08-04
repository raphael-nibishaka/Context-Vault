package config;

public enum Page {
    DASHBOARD("Dashboard", "/fxml/dashboard-view.fxml"),
    CONTEXTS("Contexts", "/fxml/dashboard-view.fxml"),
    CREATE_CONTEXT("Create Context", "/fxml/context-form-view.fxml"),
    SETTINGS("Settings", "/fxml/settings-view.fxml"),
    ABOUT("About", "/fxml/about-view.fxml");

    private final String title;
    private final String fxmlPath;

    Page(String title, String fxmlPath) {
        this.title = title;
        this.fxmlPath = fxmlPath;
    }

    public String getTitle() {
        return title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }
}
