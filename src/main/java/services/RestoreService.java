package services;

import models.AppSettings;
import models.ContextEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RestoreService {
    private final ExternalLaunchService externalLaunchService;
    private final SettingsService settingsService;

    public RestoreService(ExternalLaunchService externalLaunchService, SettingsService settingsService) {
        this.externalLaunchService = externalLaunchService;
        this.settingsService = settingsService;
    }

    public RestoreResult restore(ContextEntry contextEntry) {
        Path projectPath = Path.of(contextEntry.getProjectPath());
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("The saved project folder is missing or invalid.");
        }

        AppSettings settings = settingsService.loadSettings();
        RestoreResult result = new RestoreResult();

        launchFolder(projectPath, result);
        launchEditor(settings, projectPath, result);
        launchTerminal(settings, projectPath, result);

        return result;
    }

    private void launchFolder(Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openFolder(projectPath);
        } catch (IOException exception) {
            result.addWarning("Unable to open the project folder in Explorer.");
        }
    }

    private void launchEditor(AppSettings settings, Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openEditor(settings.preferredEditor(), projectPath);
        } catch (IOException exception) {
            result.addWarning(settings.preferredEditor().getDisplayName() + " is not available on this system.");
        }
    }

    private void launchTerminal(AppSettings settings, Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openTerminal(settings.preferredTerminal(), projectPath);
        } catch (IOException exception) {
            result.addWarning(settings.preferredTerminal().getDisplayName() + " could not be launched.");
        }
    }
}
