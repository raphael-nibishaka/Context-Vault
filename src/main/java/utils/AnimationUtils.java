package utils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class AnimationUtils {
    private AnimationUtils() {
    }

    public static void fadeIn(Node node) {
        if (node == null) {
            return;
        }
        FadeTransition transition = new FadeTransition(Duration.millis(220), node);
        transition.setFromValue(0.0);
        transition.setToValue(1.0);
        transition.play();
    }

    public static void pulse(Node node) {
        if (node == null) {
            return;
        }
        ScaleTransition transition = new ScaleTransition(Duration.millis(160), node);
        transition.setFromX(0.98);
        transition.setFromY(0.98);
        transition.setToX(1.0);
        transition.setToY(1.0);
        transition.play();
    }
}
