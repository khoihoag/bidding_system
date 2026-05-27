package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import com.bidding.util.FollowHeartButtonFactory;
import com.bidding.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WatchlistController {
    @FXML private VBox watchlistContainer;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private JsonArray followedAuctions = new JsonArray();
    private final List<CardCountdownEntry> cardCountdownEntries = new ArrayList<>();
    private Timeline countdownTimeline;

    private static final class CardCountdownEntry {
        private final Label label;
        private final LocalDateTime target;
        private final boolean ended;
        private final boolean upcoming;

        private CardCountdownEntry(Label label, LocalDateTime target, boolean ended, boolean upcoming) {
            this.label = label;
            this.target = target;
            this.ended = ended;
            this.upcoming = upcoming;
        }
    }

    @FXML
    public void initialize() {
        networkClient.setMessageHandler(this::handleServerMessage);
        requestAuctions();
    }

    @FXML
    private void handleRefresh() {
        networkClient.setMessageHandler(this::handleServerMessage);
        requestAuctions();
    }

    private void requestAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        networkClient.sendJson(request);
    }

    private void handleServerMessage(JsonObject json) {
        if (json.has("balance")) {
            double newBalance = json.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(newBalance);
            if (MainController.getInstance() != null) {
                MainController.getInstance().updateBalanceDisplay();
            }
        }
        if (!json.has("action")) {
            return;
        }

        switch (json.get("action").getAsString()) {
            case "AUCTIONS_LIST" -> handleAuctionsList(json);
            case "REALTIME_BID_UPDATE", "NEW_BID" -> handleRealtimeBidUpdate(json);
            case "GLOBAL_NOTIFY" -> requestAuctions();
            case "ERROR" -> System.err.println("[Watchlist] " + (json.has("message") ? json.get("message").getAsString() : "Server error"));
            default -> { }
        }
    }

    private void handleAuctionsList(JsonObject json) {
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            return;
        }
        JsonArray data = json.getAsJsonArray("data");
        Platform.runLater(() -> {
            followedAuctions = filterFollowedAuctions(data);
            renderAuctions(followedAuctions);
        });
    }

    private JsonArray filterFollowedAuctions(JsonArray source) {
        JsonArray filtered = new JsonArray();
        for (JsonElement element : source) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (auction.has("isFollowing") && auction.get("isFollowing").getAsBoolean()) {
                filtered.add(auction.deepCopy());
            }
        }
        return filtered;
    }

    private void renderAuctions(JsonArray data) {
        watchlistContainer.getChildren().clear();
        stopCountdowns();

        if (data.isEmpty()) {
            showEmptyMessage();
            return;
        }

        HBox runningRow = buildAuctionRow();
        HBox upcomingRow = buildAuctionRow();
        HBox endedRow = buildAuctionRow();

        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            String auctionId = getStringSafe(auction, "id");
            double currentPrice = auction.has("currentPrice") ? auction.get("currentPrice").getAsDouble() : 0.0;
            String status = getStringSafe(auction, "status");
            String startTimeStr = getStringSafe(auction, "startTime");
            String endTimeStr = getStringSafe(auction, "endTime");
            boolean isEnded = isEndedAuction(status, endTimeStr);
            boolean isUpcoming = !isEnded && isUpcomingAuction(status, startTimeStr);
            String targetTimeStr = isEnded ? endTimeStr : (isUpcoming ? startTimeStr : endTimeStr);

            String itemName = "Món hàng không xác định";
            String imagePath = "";
            String description = "";
            if (auction.has("item") && auction.get("item").isJsonObject()) {
                JsonObject item = auction.getAsJsonObject("item");
                itemName = getStringSafe(item, "name");
                description = getStringSafe(item, "description");
                if (item.has("images") && item.get("images").isJsonArray()) {
                    JsonArray imgs = item.getAsJsonArray("images");
                    if (!imgs.isEmpty()) {
                        imagePath = imgs.get(0).getAsString();
                    }
                }
            }

            StackPane card = buildAuctionCard(auctionId, itemName, description, currentPrice, imagePath, targetTimeStr, isEnded, isUpcoming);
            if (isEnded) {
                endedRow.getChildren().add(card);
            } else if (isUpcoming) {
                upcomingRow.getChildren().add(card);
            } else {
                runningRow.getChildren().add(card);
            }
        }

        addAuctionSection("Đang đấu giá", runningRow);
        addAuctionSection("Chuẩn bị đấu giá", upcomingRow);
        addAuctionSection("Đã kết thúc", endedRow);
        if (watchlistContainer.getChildren().isEmpty()) {
            showEmptyMessage();
        } else {
            startCountdowns();
        }
    }

    private StackPane buildAuctionCard(String auctionId, String itemName, String description, double currentPrice,
                                       String imagePath, String targetTimeStr, boolean isEnded, boolean isUpcoming) {
        final double cardWidth = 276;
        final double imageWidth = 248;
        final double imageHeight = 236;
        final double cardHeight = 448;

        VBox card = new VBox(12);
        card.getStyleClass().addAll("auction-card", "dashboard-auction-card");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setPrefHeight(cardHeight);
        card.setMinHeight(cardHeight);
        card.setMaxHeight(cardHeight);
        card.setUserData(auctionId);

        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("dashboard-card-image-box");
        imageContainer.setPrefSize(imageWidth, imageHeight);
        imageContainer.setMinSize(imageWidth, imageHeight);
        imageContainer.setMaxSize(imageWidth, imageHeight);
        Rectangle imageClip = new Rectangle(imageWidth, imageHeight);
        imageClip.setArcWidth(20);
        imageClip.setArcHeight(20);
        imageContainer.setClip(imageClip);
        imageContainer.setAlignment(Pos.CENTER);

        if (imagePath != null && !imagePath.isEmpty()) {
            File file = new File(imagePath);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setSmooth(true);
                fitImageCover(imageView, image, imageWidth, imageHeight);
                StackPane.setAlignment(imageView, Pos.CENTER);
                imageContainer.getChildren().add(imageView);
            } else {
                Label noImg = new Label("Lỗi hiển thị ảnh");
                noImg.getStyleClass().add("dashboard-card-image-placeholder");
                imageContainer.getChildren().add(noImg);
            }
        } else {
            Label noImg = new Label("Không có ảnh");
            noImg.getStyleClass().add("dashboard-card-image-placeholder");
            imageContainer.getChildren().add(noImg);
        }

        if (!isEnded) {
            Label timerValueLabel = new Label("--:--:--");
            timerValueLabel.getStyleClass().add("dashboard-card-timer-value");
            Label timerIconLabel = new Label("◷");
            timerIconLabel.getStyleClass().add("dashboard-card-timer-icon");
            HBox timerBadge = new HBox(3, timerIconLabel, timerValueLabel);
            timerBadge.getStyleClass().add("dashboard-card-timer-badge");
            timerBadge.setAlignment(Pos.CENTER);
            timerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(timerBadge, Pos.TOP_LEFT);
            StackPane.setMargin(timerBadge, new Insets(6, 0, 0, 6));
            imageContainer.getChildren().add(timerBadge);
            cardCountdownEntries.add(new CardCountdownEntry(timerValueLabel, parseDateTime(targetTimeStr), false, isUpcoming));
        }

        VBox body = new VBox(10);
        body.getStyleClass().add("dashboard-card-body");
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxWidth(imageWidth);
        VBox.setVgrow(body, Priority.ALWAYS);

        Label nameLabel = new Label(itemName);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("dashboard-card-title");
        nameLabel.setMaxWidth(imageWidth - 42);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button favoriteButton = FollowHeartButtonFactory.create(true);
        favoriteButton.getStyleClass().add("dashboard-card-heart-btn");
        favoriteButton.setOnAction(e -> handleUnfollowAuction(auctionId));

        HBox titleRow = new HBox(8, nameLabel, favoriteButton);
        titleRow.getStyleClass().add("dashboard-card-title-row");
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(imageWidth);

        Label descLabel = new Label(truncateDescription(description));
        descLabel.getStyleClass().add("dashboard-card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(imageWidth);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);

        Label priceCaption = new Label("Giá hiện tại");
        priceCaption.getStyleClass().add("dashboard-card-price-caption");
        Label priceLabel = new Label(currencyFmt.format(currentPrice) + " đ");
        priceLabel.getStyleClass().add("dashboard-card-price-value");

        Button viewBtn = new Button("Vào xem chi tiết");
        viewBtn.getStyleClass().add("dashboard-card-detail-btn");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> handleViewAuction(auctionId));

        body.getChildren().addAll(titleRow, descLabel, priceCaption, priceLabel, viewBtn);
        card.getChildren().addAll(imageContainer, body);
        StackPane cardShell = new StackPane(card);
        cardShell.setUserData(auctionId);
        return cardShell;
    }

    private void handleUnfollowAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }
        JsonObject request = new JsonObject();
        request.addProperty("action", "UNFOLLOW_AUCTION");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);
        removeFollowedAuction(auctionId);
        renderAuctions(followedAuctions);
    }

    private void removeFollowedAuction(String auctionId) {
        JsonArray updated = new JsonArray();
        for (JsonElement element : followedAuctions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (!auctionId.equals(getStringSafe(auction, "id"))) {
                updated.add(auction);
            }
        }
        followedAuctions = updated;
    }

    private void handleViewAuction(String auctionId) {
        AppNavigator.passData(auctionId);
        AppNavigator.navigate("AuctionDetail.fxml");
    }

    private void handleRealtimeBidUpdate(JsonObject json) {
        String auctionId = getStringSafe(json, "auctionId");
        double newPrice = json.has("newPrice") ? json.get("newPrice").getAsDouble() : 0.0;
        Platform.runLater(() -> {
            updateCachedAuctionPrice(auctionId, newPrice);
            updateVisibleAuctionPrice(watchlistContainer, auctionId, newPrice);
        });
    }

    private void updateCachedAuctionPrice(String auctionId, double newPrice) {
        if (auctionId == null || auctionId.isEmpty()) {
            return;
        }
        for (JsonElement element : followedAuctions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (auctionId.equals(getStringSafe(auction, "id"))) {
                auction.addProperty("currentPrice", newPrice);
                return;
            }
        }
    }

    private boolean updateVisibleAuctionPrice(Node node, String auctionId, double newPrice) {
        if (node instanceof VBox card && auctionId.equals(card.getUserData())) {
            for (Node child : card.getChildren()) {
                if (child instanceof Label lbl && lbl.getStyleClass().contains("dashboard-card-price-value")) {
                    lbl.setText(currencyFmt.format(newPrice) + " đ");
                    return true;
                }
                if (child instanceof Pane pane && updateVisibleAuctionPrice(pane, auctionId, newPrice)) {
                    return true;
                }
            }
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                if (updateVisibleAuctionPrice(child, auctionId, newPrice)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addAuctionSection(String title, HBox row) {
        if (!row.getChildren().isEmpty()) {
            watchlistContainer.getChildren().addAll(buildSectionHeader(title), buildHorizontalAuctionScroll(row));
        }
    }

    private Label buildSectionHeader(String text) {
        Label header = new Label(text);
        header.getStyleClass().add("dashboard-section-title");
        header.setStyle("-fx-text-fill: #111827;");
        return header;
    }

    private HBox buildAuctionRow() {
        HBox row = new HBox(24);
        row.setFillHeight(false);
        row.setMinHeight(468);
        return row;
    }

    private ScrollPane buildHorizontalAuctionScroll(HBox row) {
        ScrollPane scrollPane = new ScrollPane(row);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("dashboard-auction-strip");
        scrollPane.setMinHeight(488);
        scrollPane.setPrefHeight(498);
        return scrollPane;
    }

    private void showEmptyMessage() {
        Label emptyLabel = new Label("Chưa có phiên đấu giá nào trong danh sách theo dõi.");
        emptyLabel.getStyleClass().add("watchlist-empty-label");
        watchlistContainer.getChildren().add(emptyLabel);
    }

    private void stopCountdowns() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        cardCountdownEntries.clear();
    }

    private void startCountdowns() {
        if (cardCountdownEntries.isEmpty()) {
            return;
        }
        tickCountdowns();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdowns()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void tickCountdowns() {
        LocalDateTime now = LocalDateTime.now();
        for (CardCountdownEntry entry : cardCountdownEntries) {
            if (entry.ended) {
                entry.label.setText("Đã kết thúc");
                continue;
            }
            if (entry.target == null) {
                entry.label.setText("--:--:--");
                continue;
            }
            java.time.Duration remaining = java.time.Duration.between(now, entry.target);
            if (remaining.isNegative() || remaining.isZero()) {
                entry.label.setText(entry.upcoming ? "Sắp mở" : "Đã kết thúc");
            } else {
                entry.label.setText(String.format("%02d:%02d:%02d",
                        remaining.toHours(), remaining.toMinutesPart(), remaining.toSecondsPart()));
            }
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncateDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Chưa có mô tả sản phẩm.";
        }
        String normalized = description.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 110 ? normalized : normalized.substring(0, 107) + "...";
    }

    private boolean isEndedAuction(String status, String endTimeStr) {
        if ("FINISHED".equals(status) || "CLOSED".equals(status) || "CANCELED".equals(status)
                || "CANCELLED".equals(status) || "PAID".equals(status) || "FAILED".equals(status)) {
            return true;
        }
        if (endTimeStr == null || endTimeStr.isBlank()) {
            return false;
        }
        try {
            return LocalDateTime.now().isAfter(LocalDateTime.parse(endTimeStr));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isUpcomingAuction(String status, String startTimeStr) {
        if ("OPEN".equals(status) || "SCHEDULED".equals(status)) {
            return true;
        }
        if (startTimeStr == null || startTimeStr.isBlank()) {
            return false;
        }
        try {
            return LocalDateTime.now().isBefore(LocalDateTime.parse(startTimeStr));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void fitImageCover(ImageView imageView, Image image, double targetWidth, double targetHeight) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }
        imageView.setPreserveRatio(true);
        double scale = Math.max(targetWidth / image.getWidth(), targetHeight / image.getHeight());
        imageView.setFitWidth(image.getWidth() * scale);
        imageView.setFitHeight(image.getHeight() * scale);
    }

    private String getStringSafe(JsonObject obj, String key) {
        return JsonUtil.getString(obj, key);
    }
}
