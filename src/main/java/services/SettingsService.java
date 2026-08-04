package services;

import config.ThemeManager;
import javafx.scene.Scene;
import models.AppSettings;
import repository.SettingsRepository;

public class SettingsService {
    private final SettingsRepository settingsRepository;
    private final ThemeManager themeManager;

    public SettingsService(SettingsRepository settingsRepository, ThemeManager themeManager) {
        this.settingsRepository = settingsRepository;
        this.themeManager = themeManager;
    }

    public AppSettings loadSettings() {
        return settingsRepository.load();
    }

    public void saveSettings(AppSettings settings) {
        settingsRepository.save(settings);
    }

    public void applyTheme(Scene scene, AppSettings settings) {
        themeManager.applyTheme(scene, settings.theme());
    }
}
