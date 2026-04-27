package com.bidding.client.scene;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastManager {

    public enum ToastType {
        SUCCESS("#e8f5e9", "#1b5e20"),
        ERROR("#ffebee", "#b71c1c"),
        INFO("#e3f2fd", "#0d47a1"),
        WARN("#fff8e1", "#8d6e63");

        private final String background;
        private final String foreground;

        ToastType(String background, String foreground) {
            this.background = background;
            this.foreground = foreground;
        }
    }

    private final VBox host;

    public ToastManager(VBox host) {
        this.host = host;
    }

    public void show(String message, ToastType type) {
        HBox toast = new HBox(10);
        toast.setMaxWidth(360);
        toast.setStyle("-fx-background-color: " + type.background + "; -fx-background-radius: 10; -fx-padding: 10 14; -fx-border-color: rgba(0,0,0,0.08); -fx-border-radius: 10;");

        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + type.foreground + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toast.getChildren().addAll(label, spacer);
        toast.setOpacity(0);
        host.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(done -> host.getChildren().remove(toast));
            fadeOut.play();
        });
        pause.play();
    }
}
