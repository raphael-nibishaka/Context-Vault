package services;

import models.GitRepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitService.class);
    private static final int GIT_TIMEOUT_SECONDS = 8;

    public boolean isGitRepository(Path projectPath) {
        return inspectRepository(projectPath).isGitRepository();
    }

    public Optional<String> detectCurrentBranch(Path projectPath) {
        return Optional.ofNullable(inspectRepository(projectPath).getBranch())
                .filter(branch -> !branch.isBlank());
    }

    public GitRepositoryInfo inspectRepository(Path projectPath) {
        GitRepositoryInfo info = GitRepositoryInfo.empty();
        if (projectPath == null || !Files.isDirectory(projectPath)) {
            return info;
        }

        Optional<String> root = runGit(projectPath, "rev-parse", "--show-toplevel");
        if (root.isEmpty()) {
            return info;
        }

        Path repositoryRoot = Path.of(root.get());
        info.setGitRepository(true);
        info.setRepositoryRoot(repositoryRoot.toString());
        info.setRepositoryName(repositoryRoot.getFileName().toString());

        runGit(repositoryRoot, "branch", "--show-current")
                .ifPresent(info::setBranch);
        if (info.getBranch().isBlank()) {
            runGit(repositoryRoot, "rev-parse", "--abbrev-ref", "HEAD").ifPresent(info::setBranch);
        }

        runGit(repositoryRoot, "remote", "get-url", "origin").ifPresent(info::setRemoteUrl);
        runGit(repositoryRoot, "rev-parse", "--short", "HEAD").ifPresent(info::setCurrentCommit);
        runGit(repositoryRoot, "log", "-1", "--pretty=%s").ifPresent(info::setLastCommitMessage);
        parseStatus(repositoryRoot, info);

        return info;
    }

    public boolean checkoutBranch(Path projectPath, String branch) {
        if (branch == null || branch.isBlank()) {
            return false;
        }

        Path gitRoot = resolveRepositoryRoot(projectPath).orElse(projectPath);
        try {
            Process process = new ProcessBuilder("git", "-C", gitRoot.toString(), "checkout", branch.trim())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException exception) {
            LOGGER.debug("Unable to checkout branch {} in {}", branch, gitRoot, exception);
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public Optional<Path> resolveRepositoryRoot(Path projectPath) {
        return runGit(projectPath, "rev-parse", "--show-toplevel").map(Path::of);
    }

    private void parseStatus(Path repositoryRoot, GitRepositoryInfo info) {
        Optional<String> statusOutput = runGit(repositoryRoot, "status", "--short");
        if (statusOutput.isEmpty() || statusOutput.get().isBlank()) {
            return;
        }

        List<String> modified = new ArrayList<>();
        List<String> untracked = new ArrayList<>();
        List<String> staged = new ArrayList<>();

        for (String line : statusOutput.get().split("\\R")) {
            if (line.length() < 3) {
                continue;
            }
            String indexStatus = line.substring(0, 1);
            String workTreeStatus = line.substring(1, 2);
            String filePath = line.substring(3).trim();

            if ("??".equals(indexStatus + workTreeStatus)) {
                untracked.add(filePath);
                continue;
            }
            if (!" ".equals(indexStatus) && !"?".equals(indexStatus)) {
                staged.add(filePath);
            }
            if (!" ".equals(workTreeStatus) && !"?".equals(workTreeStatus)) {
                modified.add(filePath);
            }
        }

        info.setModifiedFiles(modified);
        info.setUntrackedFiles(untracked);
        info.setStagedFiles(staged);
    }

    private Optional<String> runGit(Path projectPath, String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(projectPath.toString());
        command.addAll(List.of(args));

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (output.isBlank() || "HEAD".equals(output)) {
                return Optional.empty();
            }
            return Optional.of(output);
        } catch (IOException exception) {
            LOGGER.debug("Git command failed for {} {}", projectPath, String.join(" ", args), exception);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
