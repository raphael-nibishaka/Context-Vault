package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestoreResult {
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();
    private String detectedProjectPath = "";
    private String detectedGitBranch = "";
    private String targetGitBranch = "";
    private boolean branchSwitched;
    private int commandsStarted;

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void addInfo(String info) {
        infoMessages.add(info);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getInfoMessages() {
        return Collections.unmodifiableList(infoMessages);
    }

    public String getDetectedProjectPath() {
        return detectedProjectPath;
    }

    public void setDetectedProjectPath(String detectedProjectPath) {
        this.detectedProjectPath = detectedProjectPath;
    }

    public String getDetectedGitBranch() {
        return detectedGitBranch;
    }

    public void setDetectedGitBranch(String detectedGitBranch) {
        this.detectedGitBranch = detectedGitBranch;
    }

    public String getTargetGitBranch() {
        return targetGitBranch;
    }

    public void setTargetGitBranch(String targetGitBranch) {
        this.targetGitBranch = targetGitBranch;
    }

    public boolean isBranchSwitched() {
        return branchSwitched;
    }

    public void setBranchSwitched(boolean branchSwitched) {
        this.branchSwitched = branchSwitched;
    }

    public int getCommandsStarted() {
        return commandsStarted;
    }

    public void setCommandsStarted(int commandsStarted) {
        this.commandsStarted = commandsStarted;
    }
}
