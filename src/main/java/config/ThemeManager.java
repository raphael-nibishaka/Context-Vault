package config;

import javafx.scene.Scene;
import models.AppTheme;

public class ThemeManager {
    private static final String BASE_STYLESHEET = "/css/base.css";
    private static final String DARK_THEME = "/css/theme-dark.css";
    private static final String LIGHT_THEME = "/css/theme-light.css";

    public void applyTheme(Scene scene, AppTheme theme) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource(BASE_STYLESHEET).toExternalForm());
        scene.getStylesheets().add(getClass().getResource(resolveTheme(theme)).toExternalForm());
    }

    private String resolveTheme(AppTheme theme) {
        return theme == AppTheme.LIGHT ? LIGHT_THEME : DARK_THEME;
    }
}
