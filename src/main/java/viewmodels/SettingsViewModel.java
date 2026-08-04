package viewmodels;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import models.AppSettings;
import models.AppTheme;
import models.PreferredEditor;
import models.PreferredTerminal;
import services.SettingsService;

public class SettingsViewModel {
    private final SettingsService settingsService;
    private final ObjectProperty<PreferredEditor> preferredEditor = new SimpleObjectProperty<>();
    private final ObjectProperty<PreferredTerminal> preferredTerminal = new SimpleObjectProperty<>();
    private final ObjectProperty<AppTheme> theme = new SimpleObjectProperty<>();

    public SettingsViewModel(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void load() {
        AppSettings settings = settingsService.loadSettings();
        preferredEditor.set(settings.preferredEditor());
        preferredTerminal.set(settings.preferredTerminal());
        theme.set(settings.theme());
    }

    public void save() {
        settingsService.saveSettings(toSettings());
    }

    public AppSettings toSettings() {
        return new AppSettings(preferredEditor.get(), preferredTerminal.get(), theme.get());
    }

    public ObjectProperty<PreferredEditor> preferredEditorProperty() {
        return preferredEditor;
    }

    public ObjectProperty<PreferredTerminal> preferredTerminalProperty() {
        return preferredTerminal;
    }

    public ObjectProperty<AppTheme> themeProperty() {
        return theme;
    }
}
