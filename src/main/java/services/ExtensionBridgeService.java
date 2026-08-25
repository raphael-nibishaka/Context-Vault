package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtensionBridgeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionBridgeService.class);
    private static final String LATEST_CONTEXT_FILE = ".context-vault/latest-context.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<ExtensionContextPayload> loadLatestContext(Path projectPath) {
        if (projectPath == null || !Files.isDirectory(projectPath)) {
            return Optional.empty();
        }

        Path bridgeFile = projectPath.resolve(LATEST_CONTEXT_FILE);
        if (!Files.isRegularFile(bridgeFile)) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(bridgeFile.toFile());
            ExtensionContextPayload payload = new ExtensionContextPayload();
            payload.setWorkspace(text(root, "workspace"));
            payload.setWorkspacePath(text(root, "workspacePath"));
            payload.setActiveFile(text(root, "activeFile"));
            payload.setGitBranch(text(root, "gitBranch"));
            payload.setName(text(root, "name"));
            payload.setOpenFilePaths(readStringArray(root.get("openFilePaths")));
            if (payload.getOpenFilePaths().isEmpty()) {
                payload.setOpenFilePaths(readStringArray(root.get("openFiles")));
            }
            return Optional.of(payload);
        } catch (IOException exception) {
            LOGGER.debug("Unable to read Context Vault extension bridge file at {}", bridgeFile, exception);
            return Optional.empty();
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    public static final class ExtensionContextPayload {
        private String name = "";
        private String workspace = "";
        private String workspacePath = "";
        private String activeFile = "";
        private String gitBranch = "";
        private List<String> openFilePaths = List.of();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWorkspace() {
            return workspace;
        }

        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }

        public String getWorkspacePath() {
            return workspacePath;
        }

        public void setWorkspacePath(String workspacePath) {
            this.workspacePath = workspacePath;
        }

        public String getActiveFile() {
            return activeFile;
        }

        public void setActiveFile(String activeFile) {
            this.activeFile = activeFile;
        }

        public String getGitBranch() {
            return gitBranch;
        }

        public void setGitBranch(String gitBranch) {
            this.gitBranch = gitBranch;
        }

        public List<String> getOpenFilePaths() {
            return openFilePaths;
        }

        public void setOpenFilePaths(List<String> openFilePaths) {
            this.openFilePaths = openFilePaths == null ? List.of() : List.copyOf(openFilePaths);
        }
    }
}
