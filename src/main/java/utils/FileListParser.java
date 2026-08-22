package utils;

import java.util.ArrayList;
import java.util.List;

public final class FileListParser {
    private FileListParser() {
    }

    public static List<String> parse(String rawFiles) {
        if (rawFiles == null || rawFiles.isBlank()) {
            return List.of();
        }

        List<String> files = new ArrayList<>();
        for (String line : rawFiles.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            files.add(trimmed);
        }
        return files;
    }

    public static int count(String rawFiles) {
        return parse(rawFiles).size();
    }
}
