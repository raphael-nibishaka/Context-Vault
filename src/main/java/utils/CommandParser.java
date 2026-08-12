package utils;

import java.util.ArrayList;
import java.util.List;

public final class CommandParser {
    private CommandParser() {
    }

    public static List<String> parse(String rawCommands) {
        if (rawCommands == null || rawCommands.isBlank()) {
            return List.of();
        }

        List<String> commands = new ArrayList<>();
        for (String line : rawCommands.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            commands.add(trimmed);
        }
        return commands;
    }
}
