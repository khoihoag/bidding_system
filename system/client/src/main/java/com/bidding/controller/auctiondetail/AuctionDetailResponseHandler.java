package com.bidding.controller.auctiondetail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import com.bidding.controller.AuctionDetailController;
/**
 * Parses inbound socket messages for the auction detail screen.
 */
public class AuctionDetailResponseHandler {

    private final Class<?> resourceClass;
    private final AuctionDetailState state;
    private final AuctionDetailUiPresenter ui;
    private final AuctionDetailChartBinder chartBinder;
    private final AuctionDetailCountdown countdown;
    private final AuctionDetailController controller;
    public AuctionDetailResponseHandler(Class<?> resourceClass,
                                        AuctionDetailState state,
                                        AuctionDetailUiPresenter ui,
                                        AuctionDetailChartBinder chartBinder,
                                        AuctionDetailCountdown countdown, AuctionDetailController controller) {
        this.resourceClass = resourceClass;
        this.state = state;
        this.ui = ui;
        this.chartBinder = chartBinder;
        this.countdown = countdown;
        this.controller= controller;
    }

    public void handle(JsonObject json) {
        if (!json.has("action")) {
            return;
        }
        String action = json.get("action").getAsString();

        switch (action) {
            case "DEPOSIT_REPLY" -> handleDepositReply(json);
            case "AUCTION_FINISHED" -> handleAuctionFinished(json);
            case "AUCTION_HISTORY_REPLY" -> handleAuctionHistoryReply(json);
            case "BID_REPLY" -> handleBidReply(json);
            case "REGISTER_AUTO_BID_REPLY" -> handleAutoBidReply(json);
            case "NEW_BID" -> handleRealtimeBidUpdate(json);
            case "AUCTIONS_LIST" -> handleAuctionsListForInfo(json);
            case "ERROR" -> handleError(json);
            default -> { }
        }
    }

    private void handleDepositReply(JsonObject json) {
        String status = json.get("status").getAsString();
        if ("SUCCESS".equals(status)) {
            double newBalance = json.get("newBalance").getAsDouble();
            Platform.runLater(() -> AuctionDetailAlerts.show(
                    Alert.AlertType.INFORMATION,
                    "Thành công",
                    "Ting ting! Sếp đã nạp tiền thành công.\nSố dư hiện tại: "
                            + String.format("%,.0f", newBalance) + " VNĐ",
                    resourceClass));
        }
    }

    private void handleAuctionFinished(JsonObject json) {
        String winner = json.get("winnerId").getAsString();
        Platform.runLater(() -> {
            countdown.stop();
            ui.showAuctionFinished(winner);
            ui.applyAuctionAccess("FINISHED", false);
            AuctionDetailAlerts.show(
                    Alert.AlertType.INFORMATION,
                    "Thông báo kết quả",
                    "Phiên đấu giá đã kết thúc!\nChúc mừng đại gia: " + winner,
                    resourceClass);
        });
    }

    private void handleAuctionHistoryReply(JsonObject json) {
        if (!json.has("data")) {
            return;
        }
        JsonArray data = json.getAsJsonArray("data");
        Platform.runLater(() -> chartBinder.loadHistory(data));
    }

    private void handleBidReply(JsonObject json) {
        String status = AuctionDetailFormats.getStringSafe(json, "status");
        Platform.runLater(() -> {
            ui.setBidLoading(false);
            if ("SUCCESS".equals(status)) {
                ui.onBidSuccess();
            } else {
                ui.showBidError("Đặt giá thất bại. Vui lòng thử lại.");
            }
        });
    }

    private void handleAutoBidReply(JsonObject json) {
        String status = AuctionDetailFormats.getStringSafe(json, "status");
        Platform.runLater(() -> {
            ui.setAutoBidLoading(false);
            if ("SUCCESS".equals(status)) {
                ui.onAutoBidActivated();
            } else {
                ui.showAutoBidError("Không thể kích hoạt Auto-Bid. Vui lòng thử lại.");
            }
        });
    }

    private void handleRealtimeBidUpdate(JsonObject json) {
        String auctionId = AuctionDetailFormats.getStringSafe(json, "auctionId");
        if (!auctionId.equals(state.currentAuctionId)) {
            return;
        }

        double newPrice = json.has("newPrice") ? json.get("newPrice").getAsDouble() : 0.0;
        String winnerId = json.has("winnerId") ? json.get("winnerId").getAsString() : "---";
        String timestamp = AuctionDetailFormats.getStringSafe(json, "timestamp");
        String displayTime = AuctionDetailFormats.formatTimestampForDisplay(timestamp);
        String newEndTimeStr = json.has("newEndTime") ? json.get("newEndTime").getAsString() : null;

        Platform.runLater(() -> {
            ui.updateCurrentPrice(newPrice);
            ui.updateWinner(winnerId);
            chartBinder.addRealtimePoint(displayTime, newPrice);
            countdown.applyAntiSnipingEndTime(newEndTimeStr);
            ui.animateReverseBidButton(newPrice);
        });
    }

    private void handleAuctionsListForInfo(JsonObject json) {
        if (!json.has("data")) {
            return;
        }
        JsonArray data = json.getAsJsonArray("data");

        for (JsonElement element : data) {
            JsonObject auction = element.getAsJsonObject();
            String id = AuctionDetailFormats.getStringSafe(auction, "id");

            if (!id.equals(state.currentAuctionId)) {
                continue;
            }

            state.currentSelectedAuction = auction;
            String itemName = "Không rõ tên";

            if (auction.has("item") && auction.get("item").isJsonObject()) {
                JsonObject item = auction.getAsJsonObject("item");
                itemName = AuctionDetailFormats.getStringSafe(item, "name");

                // ==============================================================
                // DÂY CÁP MA THUẬT ĐÂY RỒI: TRUYỀN DATA SANG CONTROLLER VẼ ẢNH!
                // ==============================================================
                final JsonObject finalItem = item;
                Platform.runLater(() -> {
                    controller.renderSothebysItemDetails(finalItem);
                });
                // ==============================================================
            }

            double currentPrice = auction.has("currentPrice")
                    ? auction.get("currentPrice").getAsDouble() : 0.0;
            String status = AuctionDetailFormats.getStringSafe(auction, "status");
            String startTimeStr = auction.has("startTime") ? auction.get("startTime").getAsString() : null;
            String endTimeStr = auction.has("endTime") ? auction.get("endTime").getAsString() : null;
            String winnerId = auction.has("winnerId") ? auction.get("winnerId").getAsString() : "---";
            boolean isReverse = auction.has("isReverse") && auction.get("isReverse").getAsBoolean();

            // 1. Phải khai báo biến isFollowing từ JSON trước khi dùng!
            boolean isFollowing = auction.has("isFollowing") && auction.get("isFollowing").getAsBoolean();

            final String finalItemName = itemName;
            final double finalPrice = currentPrice;
            final String finalStatus = status;
            final String finalStartTime = startTimeStr;
            final String finalEndTime = endTimeStr;
            final String finalWinner = winnerId;

            // 2. Chỉ để 1 dòng finalIsReverse thôi
            final boolean finalIsReverse = isReverse;

            // 3. Khai báo biến finalIsFollowing chuẩn xác
            final boolean finalIsFollowing = isFollowing;

            Platform.runLater(() -> applyAuctionInfo(
                    finalItemName,
                    finalPrice,
                    finalStatus,
                    finalStartTime,
                    finalEndTime,
                    finalWinner,
                    finalIsReverse,
                    finalIsFollowing
            ));
            break;
        }
    }

    private void applyAuctionInfo(String itemName,
                                  double currentPrice,
                                  String status,
                                  String startTimeStr,
                                  String endTimeStr,
                                  String winnerId,
                                  boolean isReverse,
                                  boolean isFollowing) { // <--- Thêm biến này vào signature

        ui.updateItemHeader(itemName);
        ui.updateCurrentPrice(currentPrice);
        ui.updateStatusBadge(status);
        ui.updateWinner(winnerId);

        // Xử lý nút Theo dõi ngay tại đây dựa trên trạng thái Server gửi về
        ui.setFollowButtonState(isFollowing);

        if ("OPEN".equals(status)) {
            if (startTimeStr != null && !startTimeStr.isEmpty()) {
                countdown.start(startTimeStr, "OPEN");
            }
            ui.disableBidsForScheduledOpen();
        } else {
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                countdown.start(endTimeStr, status);
            }
            ui.enableBidsIfRunning(status);
        }

        if (isReverse) {
            ui.applyReverseAuctionUi(currentPrice);
        } else {
            ui.applyNormalAuctionUi();
        }

        ui.applyAuctionAccess(status, isReverse);
    }

    private void handleError(JsonObject json) {
        String message = AuctionDetailFormats.getStringSafe(json, "message");
        Platform.runLater(() -> {
            ui.setBidLoading(false);
            ui.setAutoBidLoading(false);

            if (message.contains("đặt giá") || message.contains("BID") || message.contains("giá")) {
                ui.showBidError(message);
            } else if (message.contains("Auto") || message.contains("auto")) {
                ui.showAutoBidError(message);
            } else {
                ui.showResult("❌ " + message);
            }
        });
    }
}
