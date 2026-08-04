package repository;

import models.AppSettings;

public interface SettingsRepository {
    AppSettings load();

    void save(AppSettings settings);
}
