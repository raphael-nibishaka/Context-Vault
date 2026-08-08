package utils;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Creates consistent glass-styled action buttons used across the app.
 */
public final class ButtonFactory {
    private ButtonFactory() {
    }

    public static Button primary(String text, String iconLiteral, String tooltip) {
        return build(text, iconLiteral, tooltip, "primary-button", "glass-button");
    }

    public static Button secondary(String text, String iconLiteral, String tooltip) {
        return build(text, iconLiteral, tooltip, "secondary-button", "glass-button");
    }

    public static Button danger(String text, String iconLiteral, String tooltip) {
        return build(text, iconLiteral, tooltip, "danger-button", "glass-button");
    }

    public static Button ghost(String text, String iconLiteral, String tooltip) {
        return build(text, iconLiteral, tooltip, "ghost-button", "glass-button");
    }

    public static Button iconOnly(String iconLiteral, String tooltip, String... styleClasses) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("button-icon");
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add("icon-button");
        button.getStyleClass().add("glass-button");
        for (String styleClass : styleClasses) {
            button.getStyleClass().add(styleClass);
        }
        if (tooltip != null && !tooltip.isBlank()) {
            button.setTooltip(new Tooltip(tooltip));
        }
        button.setFocusTraversable(true);
        return button;
    }

    public static void decorate(Button button, String iconLiteral) {
        if (button == null) {
            return;
        }
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("button-icon");
        button.setGraphic(icon);
        if (!button.getStyleClass().contains("glass-button")) {
            button.getStyleClass().add("glass-button");
        }
        button.setFocusTraversable(true);
    }

    private static Button build(String text, String iconLiteral, String tooltip, String... styleClasses) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("button-icon");

        Button button = new Button(text, icon);
        button.getStyleClass().addAll(styleClasses);
        if (tooltip != null && !tooltip.isBlank()) {
            button.setTooltip(new Tooltip(tooltip));
        }
        button.setFocusTraversable(true);
        button.setMnemonicParsing(false);
        return button;
    }
}
