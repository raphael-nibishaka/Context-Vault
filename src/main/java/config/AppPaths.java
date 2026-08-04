package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private static final String APP_DIRECTORY_NAME = "ContextVault";

    private AppPaths() {
    }

    public static Path getAppDataDirectory() {
        String appData = System.getenv("APPDATA");
        Path root = (appData == null || appData.isBlank())
                ? Path.of(System.getProperty("user.home"), "." + APP_DIRECTORY_NAME.toLowerCase())
                : Path.of(appData, APP_DIRECTORY_NAME);
        ensureDirectory(root);
        return root;
    }

    public static Path getDatabaseFile() {
        return getAppDataDirectory().resolve("context-vault.db");
    }

    private static void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create application data directory: " + directory, exception);
        }
    }
}
