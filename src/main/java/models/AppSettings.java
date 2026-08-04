package models;

public record AppSettings(
        PreferredEditor preferredEditor,
        PreferredTerminal preferredTerminal,
        AppTheme theme
) {
    public static AppSettings defaults() {
        return new AppSettings(
                PreferredEditor.VS_CODE,
                PreferredTerminal.POWERSHELL,
                AppTheme.DARK
        );
    }
}
