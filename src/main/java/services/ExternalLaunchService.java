package services;

import models.PreferredEditor;
import models.PreferredTerminal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExternalLaunchService {
    public void openFolder(Path path) throws IOException {
        run(List.of("explorer", path.toString()));
    }

    public void openEditor(PreferredEditor preferredEditor, Path path) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(preferredEditor.getCommand());
        command.add(path.toString());
        run(command);
    }

    public void openTerminal(PreferredTerminal terminal, Path path) throws IOException {
        List<String> command = switch (terminal) {
            case WINDOWS_TERMINAL -> List.of("wt", "-d", path.toString());
            case COMMAND_PROMPT -> List.of("cmd", "/K", "cd /d \"" + path + "\"");
            case POWERSHELL -> List.of("powershell", "-NoExit", "-Command", "Set-Location -LiteralPath '" + path + "'");
        };
        run(command);
    }

    private void run(List<String> command) throws IOException {
        new ProcessBuilder(command).start();
    }
}
