package models;

public enum PreferredEditor {
    VS_CODE("VS Code", "code"),
    CURSOR("Cursor", "cursor"),
    INTELLIJ("IntelliJ", "idea64.exe"),
    VISUAL_STUDIO("Visual Studio", "devenv");

    private final String displayName;
    private final String command;

    PreferredEditor(String displayName, String command) {
        this.displayName = displayName;
        this.command = command;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
