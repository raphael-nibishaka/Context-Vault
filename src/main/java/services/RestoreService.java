package services;

import models.AppSettings;
import models.ContextEntry;
import models.RestoreStepStatus;
import utils.CommandParser;
import utils.FileListParser;

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

        restoreProjectFolder(projectPath, result);
        restoreGitBranch(projectPath, contextEntry.getGitBranch(), result);
        restoreEditor(projectPath, result);
        restoreOpenFiles(projectPath, contextEntry.getOpenFiles(), result);
        restoreTerminal(settings, projectPath, result);
        restoreBrowserTabs(contextEntry.getBrowserUrls(), result);
        restoreNotes(result);

        return result;
    }

    public Optional<String> detectGitBranch(Path projectPath) {
        return gitService.detectCurrentBranch(projectPath);
    }

    public void runSavedCommands(ContextEntry contextEntry) throws IOException {
        Path projectPath = resolveProjectPath(contextEntry.getProjectPath());
        AppSettings settings = settingsService.loadSettings();
        List<String> commands = CommandParser.parse(contextEntry.getCommands());
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("No saved commands to run.");
        }
        externalLaunchService.openTerminalWithCommands(settings.preferredTerminal(), projectPath, commands);
    }

    private Path resolveProjectPath(String rawPath) {
        return Path.of(rawPath.trim()).toAbsolutePath().normalize();
    }

    private void restoreProjectFolder(Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openFolder(projectPath);
            result.addInfo("Opened project folder.");
            result.addStep("Project", RestoreStepStatus.SUCCESS);
        } catch (IOException exception) {
            result.addWarning("Unable to open the project folder in Explorer.");
            result.addStep("Project", RestoreStepStatus.WARNING);
        }
    }

    private void restoreGitBranch(Path projectPath, String savedBranch, RestoreResult result) {
        Optional<String> currentBranch = gitService.detectCurrentBranch(projectPath);
        currentBranch.ifPresent(branch -> {
            result.setDetectedGitBranch(branch);
            result.addInfo("Detected git branch: " + branch);
        });

        if (savedBranch == null || savedBranch.isBlank()) {
            result.addStep("Git repository", currentBranch.isPresent()
                    ? RestoreStepStatus.SUCCESS
                    : RestoreStepStatus.SKIPPED);
            return;
        }

        result.setTargetGitBranch(savedBranch.trim());
        if (currentBranch.isPresent() && currentBranch.get().equalsIgnoreCase(savedBranch.trim())) {
            result.addInfo("Already on saved branch: " + savedBranch.trim());
            result.addStep("Git repository", RestoreStepStatus.SUCCESS);
            return;
        }

        if (!gitService.isGitRepository(projectPath)) {
            result.addWarning("Saved branch '" + savedBranch.trim() + "' could not be applied because this folder is not a git repository.");
            result.addStep("Git repository", RestoreStepStatus.WARNING);
            return;
        }

        if (gitService.checkoutBranch(projectPath, savedBranch)) {
            result.setBranchSwitched(true);
            result.setDetectedGitBranch(savedBranch.trim());
            result.addInfo("Switched to saved branch: " + savedBranch.trim());
            result.addStep("Git repository", RestoreStepStatus.SUCCESS);
        } else {
            result.addWarning("Unable to switch to saved branch '" + savedBranch.trim() + "'.");
            result.addStep("Git repository", RestoreStepStatus.WARNING);
        }
    }

    private void restoreEditor(Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openVsCode(projectPath);
            result.addInfo("Opened project in VS Code.");
            result.addStep("VS Code", RestoreStepStatus.SUCCESS);
            return;
        } catch (IOException ignored) {
            // Try Cursor next.
        }

        try {
            externalLaunchService.openCursor(projectPath);
            result.addInfo("Opened project in Cursor.");
            result.addStep("VS Code", RestoreStepStatus.SUCCESS);
            return;
        } catch (IOException ignored) {
            // Fall back to configured editor.
        }

        try {
            AppSettings settings = settingsService.loadSettings();
            externalLaunchService.openEditor(settings.preferredEditor(), projectPath);
            result.addInfo("Opened project in " + settings.preferredEditor().getDisplayName() + ".");
            result.addStep("VS Code", RestoreStepStatus.SUCCESS);
        } catch (IOException exception) {
            result.addWarning("No supported editor could be launched.");
            result.addStep("VS Code", RestoreStepStatus.WARNING);
        }
    }

    private void restoreOpenFiles(Path projectPath, String rawFiles, RestoreResult result) {
        List<String> files = FileListParser.parse(rawFiles);
        if (files.isEmpty()) {
            result.addStep("Files", RestoreStepStatus.SKIPPED);
            return;
        }

        try {
            int opened = externalLaunchService.openFilesInEditor(projectPath, files);
            result.setFilesOpened(opened);
            result.addInfo("Opened " + opened + " saved file(s) in the editor.");
            result.addStep("Files", opened > 0 ? RestoreStepStatus.SUCCESS : RestoreStepStatus.WARNING);
        } catch (IOException exception) {
            result.addWarning("Saved files could not be opened in the editor.");
            result.addStep("Files", RestoreStepStatus.WARNING);
        }
    }

    private void restoreTerminal(AppSettings settings, Path projectPath, RestoreResult result) {
        try {
            externalLaunchService.openTerminal(settings.preferredTerminal(), projectPath);
            result.addInfo("Opened terminal in project folder.");
            result.addStep("Terminal", RestoreStepStatus.SUCCESS);
        } catch (IOException exception) {
            result.addWarning(settings.preferredTerminal().getDisplayName() + " could not be launched.");
            result.addStep("Terminal", RestoreStepStatus.WARNING);
        }
    }

    private void restoreBrowserTabs(String rawUrls, RestoreResult result) {
        List<String> urls = CommandParser.parse(rawUrls);
        if (urls.isEmpty()) {
            result.addStep("Browser", RestoreStepStatus.SKIPPED);
            return;
        }

        try {
            externalLaunchService.openBrowserUrls(urls);
            result.setBrowserTabsOpened(urls.size());
            result.addInfo("Opened " + urls.size() + " browser tab(s).");
            result.addStep("Browser", RestoreStepStatus.SUCCESS);
        } catch (IOException exception) {
            result.addWarning("Saved browser URLs could not be opened.");
            result.addStep("Browser", RestoreStepStatus.WARNING);
        }
    }

    private void restoreNotes(RestoreResult result) {
        result.addStep("Notes", RestoreStepStatus.SUCCESS);
    }
}
