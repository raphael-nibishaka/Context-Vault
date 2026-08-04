package models;

public enum PreferredTerminal {
    WINDOWS_TERMINAL("Windows Terminal"),
    COMMAND_PROMPT("Command Prompt"),
    POWERSHELL("PowerShell");

    private final String displayName;

    PreferredTerminal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
