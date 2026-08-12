package services;

import models.AppSettings;
import models.ContextEntry;
import utils.CommandParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class RestoreService {
    private final ExternalLaunchService externalLaunchService;
    private final SettingsService settingsService;
    private final GitService gitService;

    public RestoreService(ExternalLaunchService externalLaunchService,
                          SettingsService settingsService,
                          GitService gitService) {
        this.externalLaunchService = externalLaunchService;
        this.settingsService = settingsService;
        this.gitService = gitService;
    }

    public RestoreResult restore(ContextEntry contextEntry) {
        Path projectPath = resolveProjectPath(contextEntry.getProjectPath());
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("The saved project folder is missing or invalid.");
        }

        AppSettings settings = settingsService.loadSettings();
        RestoreResult result = new RestoreResult();
        result.setDetectedProjectPath(projectPath.toString());

        detectAndSwitchBranch(projectPath, contextEntry.getGitBranch(), result);
        launchFolder(projectPath, result);
        launchVsCode(projectPath, result);
        launchTerminalWithCommands(settings, projectPath, contextEntry.getCommands(), result);

        return result;
    }

    public Optional<String> detectGitBranch(Path projectPath) {
        return gitService.detectCurrentBranch(projectPath);
    }

    private Path resolveProjectPath(String rawPath) {
        return Path.of(rawPath.trim()).toAbsolutePath().normalize();
    }

    private void detectAndSwitchBranch(Path projectPath, String savedBranch, RestoreResult result) {
        Optional<String> currentBranch = gitService.detectCurrentBranch(projectPath);
        currentBranch.ifPresent(branch -> {
            result.setDetectedGitBranch(branch);
            result.addInfo("Detected git branch: " + branch);
        });

        if (savedBranch == null || savedBranch.isBlank()) {
            return;
        }

        result.setTargetGitBranch(savedBranch.trim());
        if (currentBranch.isPresent() && currentBranch.get().equalsIgnoreCase(savedBranch.trim())) {
            result.addInfo("Already on saved branch: " + savedBranch.trim());
            return;
        }

        if (!gitService.isGitRepository(projectPath)) {
            result.addWarning("Saved branch '" + savedBranch.trim() + "' could not be applied because this folder is not a git repository.");
            return;
        }

        if (gitService.checkoutBranch(projectPath, savedBranch)) {
            result.setBranchSwitched(true);
            result.setDetectedGitBranch(savedBranch.trim());
            result.addInfo("Switched to saved branch: " + savedBranch.trim());
        } else {
            result.addWarning("Unable to switch to saved branch '" + savedBranch.trim() + "'.");
        }
    }

    private void launchFolder(Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openFolder(projectPath);
            result.addInfo("Opened project folder.");
        } catch (IOException exception) {
            result.addWarning("Unable to open the project folder in Explorer.");
        }
    }

    private void launchVsCode(Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openVsCode(projectPath);
            result.addInfo("Opened project in VS Code.");
            return;
        } catch (IOException ignored) {
            // Try Cursor next — common on developer machines where `code` is not on PATH.
        }

        try {
            externalLaunchService.openCursor(projectPath);
            result.addInfo("Opened project in Cursor.");
            return;
        } catch (IOException ignored) {
            // Fall back to the configured editor.
        }

        try {
            AppSettings settings = settingsService.loadSettings();
            externalLaunchService.openEditor(settings.preferredEditor(), projectPath);
            result.addInfo("Opened project in " + settings.preferredEditor().getDisplayName() + ".");
        } catch (IOException exception) {
            result.addWarning("No supported editor could be launched. Install VS Code or Cursor, or update your preferred editor in Settings.");
        }
    }

    private void launchTerminalWithCommands(AppSettings settings,
                                            Path projectPath,
                                            String rawCommands,
                                            RestoreResult result) {
        List<String> commands = CommandParser.parse(rawCommands);
        try {
            if (commands.isEmpty()) {
                externalLaunchService.openTerminal(settings.preferredTerminal(), projectPath);
                result.addInfo("Opened terminal in project folder.");
                return;
            }

            externalLaunchService.openTerminalWithCommands(settings.preferredTerminal(), projectPath, commands);
            result.setCommandsStarted(commands.size());
            result.addInfo("Started " + commands.size() + " saved command(s) in terminal.");
        } catch (IOException exception) {
            if (commands.isEmpty()) {
                result.addWarning(settings.preferredTerminal().getDisplayName() + " could not be launched.");
            } else {
                result.addWarning("Saved commands could not be started in " + settings.preferredTerminal().getDisplayName() + ".");
            }
        }
    }
}
