package services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitService.class);
    private static final int GIT_TIMEOUT_SECONDS = 8;

    public boolean isGitRepository(Path projectPath) {
        return Files.isDirectory(projectPath.resolve(".git"))
                || detectCurrentBranch(projectPath).isPresent();
    }

    public Optional<String> detectCurrentBranch(Path projectPath) {
        try {
            Process process = new ProcessBuilder("git", "-C", projectPath.toString(), "rev-parse", "--abbrev-ref", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            String branch = new String(process.getInputStream().readAllBytes()).trim();
            if (branch.isBlank() || "HEAD".equals(branch)) {
                return Optional.empty();
            }
            return Optional.of(branch);
        } catch (IOException exception) {
            LOGGER.debug("Unable to detect git branch for {}", projectPath, exception);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public boolean checkoutBranch(Path projectPath, String branch) {
        if (branch == null || branch.isBlank()) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("git", "-C", projectPath.toString(), "checkout", branch.trim())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException exception) {
            LOGGER.debug("Unable to checkout branch {} in {}", branch, projectPath, exception);
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
