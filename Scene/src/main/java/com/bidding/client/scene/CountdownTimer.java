package com.bidding.client.scene;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CountdownTimer extends Label {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean compactMode;
    private final Timeline timeline;

    public CountdownTimer() {
        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> refresh()),
                new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public void configure(LocalDateTime startTime, LocalDateTime endTime, boolean compactMode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.compactMode = compactMode;
        refresh();
        if (endTime != null) {
            timeline.play();
        } else {
            timeline.stop();
        }
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        refresh();
    }

    public void stopTimer() {
        timeline.stop();
    }

    public void refresh() {
        if (endTime == null) {
            setText("--:--:--");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            long seconds = Math.max(0L, ChronoUnit.SECONDS.between(now, startTime));
            setText((compactMode ? "Bat dau sau " : "Phien bat dau sau ") + formatSeconds(seconds));
            setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #166534;");
            return;
        }

        long seconds = Math.max(0L, ChronoUnit.SECONDS.between(now, endTime));
        if (seconds == 0L && now.isAfter(endTime)) {
            setText(compactMode ? "Da ket thuc" : "Phien da ket thuc");
            setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            timeline.stop();
            return;
        }

        String color = seconds <= 300 ? "#dc2626" : "#166534";
        String prefix = compactMode ? "Con " : "";
        setText(prefix + formatSeconds(seconds));
        setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private String formatSeconds(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format("%d ngay %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
