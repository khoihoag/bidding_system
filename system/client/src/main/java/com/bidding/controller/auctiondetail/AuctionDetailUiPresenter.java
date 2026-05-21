package com.bidding.controller.auctiondetail;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * UI rendering and form feedback for the auction detail screen.
 */
public class AuctionDetailUiPresenter {

    private final AuctionDetailState state;
    private final Label headerItemName;
    private final Label itemNameLabel;
    private final Label currentPriceLabel;
    private final Label statusBadgeHeader;
    private final Label currentWinnerLabel;
    private final Label totalBidsLabel;
    private final Label timeRemainingLabel;
    private final Label resultLabel;
    private final Label lastUpdateLabel;
    private final Label bidErrorLabel;
    private final Label autoBidErrorLabel;
    private final TextField bidAmountField;
    private final TextField maxBidField;
    private final TextField incrementField;
    private final Button bidButton;
    private final Button autoBidButton;
    private final VBox autoBidContainer;

    public AuctionDetailUiPresenter(AuctionDetailState state,
                                    Label headerItemName,
                                    Label itemNameLabel,
                                    Label currentPriceLabel,
                                    Label statusBadgeHeader,
                                    Label currentWinnerLabel,
                                    Label totalBidsLabel,
                                    Label timeRemainingLabel,
                                    Label resultLabel,
                                    Label lastUpdateLabel,
                                    Label bidErrorLabel,
                                    Label autoBidErrorLabel,
                                    TextField bidAmountField,
                                    TextField maxBidField,
                                    TextField incrementField,
                                    Button bidButton,
                                    Button autoBidButton,
                                    VBox autoBidContainer) {
        this.state = state;
        this.headerItemName = headerItemName;
        this.itemNameLabel = itemNameLabel;
        this.currentPriceLabel = currentPriceLabel;
        this.statusBadgeHeader = statusBadgeHeader;
        this.currentWinnerLabel = currentWinnerLabel;
        this.totalBidsLabel = totalBidsLabel;
        this.timeRemainingLabel = timeRemainingLabel;
        this.resultLabel = resultLabel;
        this.lastUpdateLabel = lastUpdateLabel;
        this.bidErrorLabel = bidErrorLabel;
        this.autoBidErrorLabel = autoBidErrorLabel;
        this.bidAmountField = bidAmountField;
        this.maxBidField = maxBidField;
        this.incrementField = incrementField;
        this.bidButton = bidButton;
        this.autoBidButton = autoBidButton;
        this.autoBidContainer = autoBidContainer;
    }

    public void applyInitialStyles() {
        headerItemName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        currentPriceLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        itemNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        bidButton.getStyleClass().add("action-button");
        autoBidButton.getStyleClass().add("action-button");
        bidAmountField.getStyleClass().add("detail-input");
        maxBidField.getStyleClass().add("detail-input");
        incrementField.getStyleClass().add("detail-input");
    }

    public void updateItemHeader(String itemName) {
        itemNameLabel.setText(itemName);
        headerItemName.setText(itemName);
    }

    public void updateCurrentPrice(double price) {
        currentPriceLabel.setText(AuctionDetailFormats.CURRENCY_FMT.format(price) + " ₫");
    }

    public void updateStatusBadge(String status) {
        statusBadgeHeader.getStyleClass().removeAll("badge-running", "badge-scheduled", "badge-closed", "badge-default");
        switch (status) {
            case "RUNNING", "ACTIVE" -> {
                statusBadgeHeader.setText("🟢 Đang diễn ra");
                statusBadgeHeader.getStyleClass().add("badge-running");
            }
            case "FINISHED", "CLOSED" -> {
                statusBadgeHeader.setText("🔴 Đã kết thúc");
                statusBadgeHeader.getStyleClass().add("badge-closed");
            }
            default -> {
                statusBadgeHeader.setText("⚪ " + status);
                statusBadgeHeader.getStyleClass().add("badge-default");
            }
        }
    }

    public void updateWinner(String winnerId) {
        currentWinnerLabel.setText(winnerId);
    }

    public void updateTotalBids(int count) {
        totalBidsLabel.setText(String.valueOf(count));
    }

    public void updateLastUpdate(String displayTime) {
        lastUpdateLabel.setText("Cập nhật lúc: " + displayTime);
    }

    public void showBidError(String message) {
        bidErrorLabel.setText(message);
        bidErrorLabel.setVisible(true);
    }

    public void clearBidError() {
        bidErrorLabel.setText("");
        bidErrorLabel.setVisible(false);
    }

    public void showAutoBidError(String message) {
        autoBidErrorLabel.setText(message);
        autoBidErrorLabel.setVisible(true);
    }

    public void clearAutoBidError() {
        autoBidErrorLabel.setText("");
        autoBidErrorLabel.setVisible(false);
    }

    public void showResult(String message) {
        resultLabel.setText(message);
        resultLabel.setVisible(true);
    }

    public void clearResult() {
        resultLabel.setText("");
    }

    public void setBidLoading(boolean loading) {
        bidButton.setDisable(loading);
        bidButton.setText(loading ? "Đang gửi..." : "🔨 Đặt Giá Ngay");
        if (!loading) {
            clearBidError();
        }
    }

    public void setAutoBidLoading(boolean loading) {
        autoBidButton.setDisable(loading);
        autoBidButton.setText(loading ? "Đang kích hoạt..." : "🤖 Kích Hoạt Auto-Bid");
        if (!loading) {
            clearAutoBidError();
        }
    }

    public void disableBidsForScheduledOpen() {
        if (bidButton != null) {
            bidButton.setDisable(true);
        }
        if (autoBidButton != null) {
            autoBidButton.setDisable(true);
        }
    }

    public void enableBidsIfRunning(String status) {
        if ("RUNNING".equals(status) || "ACTIVE".equals(status)) {
            if (bidButton != null) {
                bidButton.setDisable(false);
            }
            if (autoBidButton != null) {
                autoBidButton.setDisable(false);
            }
        }
    }

    public void applyReverseAuctionUi(double currentPrice) {
        if (bidAmountField != null) {
            bidAmountField.setVisible(false);
            bidAmountField.setManaged(false);
        }
        if (autoBidContainer != null) {
            autoBidContainer.setVisible(false);
            autoBidContainer.setManaged(false);
        }
        if (autoBidButton != null) {
            autoBidButton.setVisible(false);
            autoBidButton.setManaged(false);
        }
        if (maxBidField != null) {
            maxBidField.setVisible(false);
            maxBidField.setManaged(false);
        }
        if (incrementField != null) {
            incrementField.setVisible(false);
            incrementField.setManaged(false);
        }
        if (bidButton != null) {
            bidButton.setText("🔨 CHỐT ĐƠN: " + AuctionDetailFormats.CURRENCY_FMT.format(currentPrice) + " ₫");
            bidButton.setStyle("-fx-background-color: linear-gradient(to bottom right, #f9df9f, #d4af37, #9e7f1e); "
                    + "-fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 10 20; "
                    + "-fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(212, 175, 55, 0.5), 15, 0, 0, 0);");
        }
    }

    public void applyNormalAuctionUi() {
        if (bidAmountField != null) {
            bidAmountField.setVisible(true);
            bidAmountField.setManaged(true);
        }
        if (autoBidButton != null) {
            autoBidButton.setVisible(true);
            autoBidButton.setManaged(true);
        }
        if (maxBidField != null) {
            maxBidField.setVisible(true);
            maxBidField.setManaged(true);
        }
        if (incrementField != null) {
            incrementField.setVisible(true);
            incrementField.setManaged(true);
        }
        if (bidButton != null) {
            bidButton.setText("🔨 Đặt Giá Ngay");
            bidButton.setStyle("");
        }
    }

    public void animateReverseBidButton(double newPrice) {
        if (bidButton == null || state.currentSelectedAuction == null
                || !state.currentSelectedAuction.has("isReverse")
                || !state.currentSelectedAuction.get("isReverse").getAsBoolean()) {
            return;
        }
        bidButton.setText("🔨 CHỐT ĐƠN: " + AuctionDetailFormats.CURRENCY_FMT.format(newPrice) + " ₫");
        bidButton.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f9df9f, #d4af37); "
                + "-fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 19px; -fx-background-radius: 8; "
                + "-fx-scale-x: 1.05; -fx-scale-y: 1.05; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(255, 255, 255, 0.8), 20, 0, 0, 0);");
        javafx.animation.PauseTransition pauseBtn =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.3));
        pauseBtn.setOnFinished(e -> bidButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #f9df9f, #d4af37, #9e7f1e); "
                        + "-fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 18px; -fx-background-radius: 8; "
                        + "-fx-scale-x: 1; -fx-scale-y: 1; "
                        + "-fx-effect: dropshadow(three-pass-box, rgba(212, 175, 55, 0.5), 15, 0, 0, 0);"));
        pauseBtn.play();
    }

    public void showAuctionFinished(String winner) {
        resultLabel.setText("🏆 PHIÊN ĐÃ KẾT THÚC! NGƯỜI THẮNG: " + winner);
        resultLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-font-weight: bold;");
        statusBadgeHeader.setText("🔴 Kết thúc");
        statusBadgeHeader.getStyleClass().setAll("status-badge", "badge-closed");
        bidButton.setDisable(true);
        autoBidButton.setDisable(true);
    }

    public void onAutoBidActivated() {
        maxBidField.clear();
        incrementField.clear();
        clearAutoBidError();
        showResult("🤖 Auto-Bid đã được kích hoạt thành công!");
        autoBidButton.setText("✅ Auto-Bid đang chạy");
        autoBidButton.setDisable(true);
    }

    public void onBidSuccess() {
        bidAmountField.clear();
        clearBidError();
        showResult("✅ Đặt giá thành công! Đang chờ cập nhật...");
    }

    public Label getTimeRemainingLabel() {
        return timeRemainingLabel;
    }

    public Button getBidButton() {
        return bidButton;
    }

    public Button getAutoBidButton() {
        return autoBidButton;
    }
}
