package com.bidding.controller.auctiondetail;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * Countdown timer for auction start/end on the detail screen.
 */
public class AuctionDetailCountdown {

    private final AuctionDetailState state;
    private final Label timeRemainingLabel;
    private final Button bidButton;
    private final Button autoBidButton;
    private Timeline countdownTimer;

    public AuctionDetailCountdown(AuctionDetailState state,
                                  Label timeRemainingLabel,
                                  Button bidButton,
                                  Button autoBidButton) {
        this.state = state;
        this.timeRemainingLabel = timeRemainingLabel;
        this.bidButton = bidButton;
        this.autoBidButton = autoBidButton;
    }

    public void start(String targetTimeStr, String status) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        state.endTime = LocalDateTime.parse(targetTimeStr);

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick(status)));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    public void stop() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    public void applyAntiSnipingEndTime(String newEndTimeStr) {
        if (newEndTimeStr == null) {
            return;
        }
        try {
            state.endTime = LocalDateTime.parse(newEndTimeStr);
            if (timeRemainingLabel != null) {
                timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-scale-x: 1.3; -fx-scale-y: 1.3; -fx-font-weight: bold;");
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(ev -> timeRemainingLabel.setStyle(
                        "-fx-text-fill: #e74c3c; -fx-scale-x: 1; -fx-scale-y: 1; -fx-font-weight: bold;"));
                pause.play();
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse giờ Anti-Sniping: " + e.getMessage());
        }
    }

    private void tick(String status) {
        java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), state.endTime);

        if (duration.isNegative() || duration.isZero()) {
            if ("OPEN".equals(status)) {
                timeRemainingLabel.setText("ĐANG MỞ SẠP...");
                timeRemainingLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
            } else {
                timeRemainingLabel.setText("ĐÃ KẾT THÚC");
                timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                if (bidButton != null) {
                    bidButton.setDisable(true);
                }
                if (autoBidButton != null) {
                    autoBidButton.setDisable(true);
                }
            }
            countdownTimer.stop();
        } else {
            long h = duration.toHours();
            long m = duration.toMinutesPart();
            long s = duration.toSecondsPart();
            timeRemainingLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }
}
