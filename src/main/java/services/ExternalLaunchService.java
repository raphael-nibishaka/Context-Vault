package services;

import models.PreferredEditor;
import models.PreferredTerminal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExternalLaunchService {
    public void openFolder(Path path) throws IOException {
        run(List.of("explorer", path.toString()));
    }

    public void openVsCode(Path path) throws IOException {
        launchFirstAvailable(resolveVsCodeLaunchers(path), "VS Code");
    }

    public void openCursor(Path path) throws IOException {
        launchFirstAvailable(resolveCursorLaunchers(path), "Cursor");
    }

    public void openEditor(PreferredEditor preferredEditor, Path path) throws IOException {
        if (preferredEditor == PreferredEditor.VS_CODE) {
            openVsCode(path);
            return;
        }
        if (preferredEditor == PreferredEditor.CURSOR) {
            openCursor(path);
            return;
        }
        run(List.of(preferredEditor.getCommand(), path.toString()));
    }

    public Optional<String> resolveEditorName(Path path) {
        for (EditorLauncher launcher : resolveVsCodeLaunchers(path)) {
            if (launcher.isAvailable()) {
                return Optional.of("VS Code");
            }
        }
        for (EditorLauncher launcher : resolveCursorLaunchers(path)) {
            if (launcher.isAvailable()) {
                return Optional.of("Cursor");
            }
        }
        return Optional.empty();
    }

    public void openTerminal(PreferredTerminal terminal, Path path) throws IOException {
        run(buildTerminalCommand(terminal, path, null));
    }

    public void openTerminalWithCommands(PreferredTerminal terminal, Path path, List<String> commands) throws IOException {
        run(buildTerminalCommand(terminal, path, commands));
    }

    public int openFilesInEditor(Path projectPath, List<String> filePaths) throws IOException {
        if (filePaths.isEmpty()) {
            return 0;
        }

        List<String> absolutePaths = new ArrayList<>();
        for (String filePath : filePaths) {
            Path resolved = projectPath.resolve(filePath).normalize();
            if (Files.isRegularFile(resolved)) {
                absolutePaths.add(resolved.toString());
            }
        }

        if (absolutePaths.isEmpty()) {
            return 0;
        }

        List<EditorLauncher> launchers = new ArrayList<>();
        launchers.addAll(resolveVsCodeLaunchers(projectPath, absolutePaths));
        launchers.addAll(resolveCursorLaunchers(projectPath, absolutePaths));
        launchFirstAvailable(launchers, "Editor");
        return absolutePaths.size();
    }

    public void openBrowserUrls(List<String> urls) throws IOException {
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            run(List.of("rundll32", "url.dll,FileProtocolHandler", url.trim()));
        }
    }

    private List<EditorLauncher> resolveVsCodeLaunchers(Path projectPath) {
        List<EditorLauncher> launchers = new ArrayList<>();
        launchers.add(new EditorLauncher("code", List.of("code", projectPath.toString())));

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            Path codeCmd = Path.of(localAppData, "Programs", "Microsoft VS Code", "bin", "code.cmd");
            launchers.add(new EditorLauncher("VS Code", List.of(codeCmd.toString(), projectPath.toString()), codeCmd));

            Path codeExe = Path.of(localAppData, "Programs", "Microsoft VS Code", "Code.exe");
            launchers.add(new EditorLauncher("VS Code", List.of(codeExe.toString(), projectPath.toString()), codeExe));
        }

        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            Path codeCmd = Path.of(programFiles, "Microsoft VS Code", "bin", "code.cmd");
            launchers.add(new EditorLauncher("VS Code", List.of(codeCmd.toString(), projectPath.toString()), codeCmd));

            Path codeExe = Path.of(programFiles, "Microsoft VS Code", "Code.exe");
            launchers.add(new EditorLauncher("VS Code", List.of(codeExe.toString(), projectPath.toString()), codeExe));
        }

        return launchers;
    }

    private List<EditorLauncher> resolveVsCodeLaunchers(Path projectPath, List<String> absolutePaths) {
        List<EditorLauncher> launchers = new ArrayList<>();
        List<String> codeCommand = new ArrayList<>();
        codeCommand.add("code");
        codeCommand.add("-r");
        codeCommand.addAll(absolutePaths);
        launchers.add(new EditorLauncher("code", codeCommand));

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            Path codeCmd = Path.of(localAppData, "Programs", "Microsoft VS Code", "bin", "code.cmd");
            launchers.add(new EditorLauncher("VS Code", buildEditorFileCommand(codeCmd, absolutePaths), codeCmd));

            Path codeExe = Path.of(localAppData, "Programs", "Microsoft VS Code", "Code.exe");
            launchers.add(new EditorLauncher("VS Code", buildEditorFileCommand(codeExe, absolutePaths), codeExe));
        }

        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            Path codeCmd = Path.of(programFiles, "Microsoft VS Code", "bin", "code.cmd");
            launchers.add(new EditorLauncher("VS Code", buildEditorFileCommand(codeCmd, absolutePaths), codeCmd));
        }

        return launchers;
    }

    private List<EditorLauncher> resolveCursorLaunchers(Path projectPath) {
        List<EditorLauncher> launchers = new ArrayList<>();
        launchers.add(new EditorLauncher("cursor", List.of("cursor", projectPath.toString())));

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            Path cursorExe = Path.of(localAppData, "Programs", "cursor", "Cursor.exe");
            launchers.add(new EditorLauncher("Cursor", List.of(cursorExe.toString(), projectPath.toString()), cursorExe));
        }

        return launchers;
    }

    private List<EditorLauncher> resolveCursorLaunchers(Path projectPath, List<String> absolutePaths) {
        List<EditorLauncher> launchers = new ArrayList<>();
        List<String> cursorCommand = new ArrayList<>();
        cursorCommand.add("cursor");
        cursorCommand.add("-r");
        cursorCommand.addAll(absolutePaths);
        launchers.add(new EditorLauncher("cursor", cursorCommand));

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            Path cursorExe = Path.of(localAppData, "Programs", "cursor", "Cursor.exe");
            launchers.add(new EditorLauncher("Cursor", buildEditorFileCommand(cursorExe, absolutePaths), cursorExe));
        }

        return launchers;
    }

    private List<String> buildEditorFileCommand(Path executable, List<String> absolutePaths) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.add("-r");
        command.addAll(absolutePaths);
        return command;
    }

    private void launchFirstAvailable(List<EditorLauncher> launchers, String label) throws IOException {
        IOException lastError = null;
        for (EditorLauncher launcher : launchers) {
            if (!launcher.isAvailable()) {
                continue;
            }
            try {
                run(launcher.command());
                return;
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw lastError != null ? lastError : new IOException(label + " is not available on this system.");
    }

    private List<String> buildTerminalCommand(PreferredTerminal terminal, Path path, List<String> commands) {
        String projectPath = path.toString();
        String script = buildCommandScript(projectPath, commands);

        return switch (terminal) {
            case WINDOWS_TERMINAL -> List.of("wt", "-d", projectPath, "powershell", "-NoExit", "-Command", script);
            case COMMAND_PROMPT -> {
                if (commands == null || commands.isEmpty()) {
                    yield List.of("cmd", "/K", "cd /d \"" + projectPath + "\"");
                }
                String chained = "cd /d \"" + projectPath + "\" && " + String.join(" && ", commands);
                yield List.of("cmd", "/K", chained);
            }
            case POWERSHELL -> List.of("powershell", "-NoExit", "-Command", script);
        };
    }

    private String buildCommandScript(String projectPath, List<String> commands) {
        String location = "Set-Location -LiteralPath '" + escapeSingleQuotes(projectPath) + "'";
        if (commands == null || commands.isEmpty()) {
            return location;
        }
        String chained = commands.stream()
                .map(this::escapeSingleQuotes)
                .collect(Collectors.joining("; "));
        return location + "; " + chained;
    }

    private String escapeSingleQuotes(String value) {
        return value.replace("'", "''");
    }

    private void run(List<String> command) throws IOException {
        new ProcessBuilder(command).start();
    }

    private static final class EditorLauncher {
        private final List<String> command;
        private final Path executablePath;

        private EditorLauncher(String commandName, List<String> command) {
            this(commandName, command, null);
        }

        private EditorLauncher(String commandName, List<String> command, Path executablePath) {
            this.command = command;
            this.executablePath = executablePath;
        }

        private boolean isAvailable() {
            if (executablePath != null) {
                return Files.isRegularFile(executablePath);
            }
            return true;
        }

        private List<String> command() {
            return command;
        }
    }
}
