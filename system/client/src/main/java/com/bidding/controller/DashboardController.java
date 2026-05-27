package com.bidding.controller;

import com.bidding.network.NetworkClient;
import com.bidding.util.FollowHeartButtonFactory;
import com.bidding.util.JsonUtil;
import com.bidding.util.TextUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import com.bidding.model.UserSession;
import com.bidding.controller.MainController;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.Tooltip;

public class DashboardController {
    @FXML private StackPane heroStackPane; // <--- Sếp thêm đúng dòng này vào
    @FXML private ImageView carouselImageView;
    @FXML private Label lblCarouselTitle, lblCarouselDesc;
    @FXML private HBox carouselDots;
    private int currentIndex = 0;
    private final String[] images = {
            "images/samsung-s23-ultra-jpg.jpg",
            "images/img_1.png",
            "images/img.png",
            "images/img_2.png",
            "images/What-is-Modern-Art-Definition-History-and-Examples-Featured.jpg"
    };
    private final String[] titles = {
            "SamSung S23 Ultra",
            "Iphone 17 PRO",
            "Lamborghini",
            "Ducati Monster",
            "Modern Art"
    };
    private void setupCarousel() {
        if (carouselImageView == null || lblCarouselTitle == null || carouselDots == null) {
            return;
        }
        updateSlide();

        // TỰ ĐỘNG CHUYỂN CẢNH SAU 5 GIÂY (PHÁP THUẬT REALTIME)
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> handleNextSlide())
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
    @FXML
    private void handleNextSlide() {
        currentIndex = (currentIndex + 1) % images.length;
        updateSlide();
    }
    @FXML
    private void handlePrevSlide() {
        currentIndex = (currentIndex - 1 + images.length) % images.length;
        updateSlide();
    }
    private void updateSlide() {
        // Hiệu ứng mờ dần
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), carouselImageView);
        fade.setFromValue(0.5);
        fade.setToValue(1.0);

        // --- SỬA ĐOẠN NÀY LẠI CHO CHUẨN ---
        String path = images[currentIndex];
        var inputStream = getClass().getClassLoader().getResourceAsStream(path);

        if (inputStream != null) {
            carouselImageView.setImage(new javafx.scene.image.Image(inputStream));
        } else {
            System.err.println("❌ Không tìm thấy ảnh tại: " + path);
        }
        // ---------------------------------

        lblCarouselTitle.setText(titles[currentIndex]);

        // Cập nhật mấy cái chấm tròn bên dưới
        carouselDots.getChildren().clear();
        for (int i = 0; i < images.length; i++) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
            dot.setFill(i == currentIndex ? javafx.scene.paint.Color.WHITE : javafx.scene.paint.Color.web("#6B7280"));
            carouselDots.getChildren().add(dot);
        }
        fade.play();
    }
    @FXML
    private VBox auctionContainer;

    @FXML
    private TextField auctionSearchField;
    @FXML private Label dashboardUsernameLabel;
    @FXML private Label dashboardHandleLabel;
    @FXML private Label dashboardBalanceLabel;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private JsonArray allAuctions = new JsonArray();
    private final List<CardCountdownEntry> cardCountdownEntries = new ArrayList<>();
    private Timeline dashboardCountdownTimeline;

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
        setupDashboardHeader();
        setupSearch();
        requestAuctions();
    }

    private void setupDashboardHeader() {
        UserSession session = UserSession.getInstance();
        String username = session.getUsername();
        if (dashboardUsernameLabel != null) {
            dashboardUsernameLabel.setText(username != null && !username.isBlank() ? username : "Người dùng");
        }
        if (dashboardHandleLabel != null) {
            dashboardHandleLabel.setText(username != null && !username.isBlank() ? "@" + username : "@nguoidung");
        }
        if (dashboardBalanceLabel != null) {
            dashboardBalanceLabel.setStyle("-fx-text-overrun: ellipsis;");
        }
        updateDashboardBalance();
    }

    private void updateDashboardBalance() {
        if (dashboardBalanceLabel == null) return;
        double balance = UserSession.getInstance().getBalance();
        String formatted = currencyFmt.format(balance) + " ₫";
        dashboardBalanceLabel.setText(formatted);
        Tooltip.install(dashboardBalanceLabel, new Tooltip(formatted));
    }

    private void setupSearch() {
        if (auctionSearchField == null) return;
        auctionSearchField.textProperty().addListener((obs, oldText, newText) -> renderAuctions(getFilteredAuctions()));
        if (auctionSearchField.getParent() instanceof HBox searchShell) {
            auctionSearchField.focusedProperty().addListener((obs, wasFocused, focused) -> {
                if (focused) {
                    if (!searchShell.getStyleClass().contains("dashboard-search-shell-focused")) {
                        searchShell.getStyleClass().add("dashboard-search-shell-focused");
                    }
                } else {
                    searchShell.getStyleClass().remove("dashboard-search-shell-focused");
                }
            });
        }
    }

    @FXML
    private void handleRefresh() {
        // Trước khi refresh, phải đảm bảo mình đang cầm "cái tai"
        networkClient.setMessageHandler(this::handleServerMessage);
        requestAuctions();
    }

    @FXML
    private void handleAuctionSearch() {
        renderAuctions(getFilteredAuctions());
    }

    @FXML
    private void clearAuctionSearch() {
        if (auctionSearchField != null) {
            auctionSearchField.clear();
        }
        renderAuctions(getFilteredAuctions());
    }

    @FXML
    private void handleEditProfile() {
        String currentUsername = UserSession.getInstance().getUsername();
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
                currentUsername != null ? currentUsername : ""
        );
        dialog.setTitle("Thông tin người dùng");
        dialog.setHeaderText("Sửa thông tin người dùng");
        dialog.setContentText("Tên hiển thị:");
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        dialog.showAndWait().ifPresent(value -> {
            String username = value.trim();
            if (username.isEmpty()) return;
            UserSession.getInstance().setUsername(username);
            setupDashboardHeader();
        });
    }

    @FXML
    private void handleDashboardLogout() {
        networkClient.setMessageHandler(null);
        UserSession.getInstance().clear();
        AppNavigator.navigate("Login.fxml");
    }

    private void requestAuctions() {
        System.out.println("[Dashboard] Đang yêu cầu danh sách đấu giá...");
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        networkClient.sendJson(request);
    }

    private void handleServerMessage(JsonObject json) {
        // ================= LƯỚI HỨNG TIỀN TỰ ĐỘNG =================
        // Cứ thấy gói hàng nào có dán nhãn "balance" là móc ra cất ví ngay lập tức!
        if (json.has("balance")) {
            double newBalance = json.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(newBalance);

            // Gọi bộ đàm báo cho khung MainController bên ngoài cập nhật số dư hiển thị
            if (MainController.getInstance() != null) {
                MainController.getInstance().updateBalanceDisplay();
            }
            Platform.runLater(this::updateDashboardBalance);
        }
        // ==========================================================

        if (!json.has("action")) return;
        String action = json.get("action").getAsString();

        // Log để ông check console xem Server nó có bắn gì về không
        System.out.println("[Dashboard] Nhận action: " + action);

        switch (action) {
            case "AUCTIONS_LIST" -> handleAuctionsList(json);
            case "REALTIME_BID_UPDATE", "NEW_BID" -> handleRealtimeBidUpdate(json);

            // QUAN TRỌNG: Nghe loa phát thanh để tự refresh khi có hàng mới!
            case "GLOBAL_NOTIFY" -> {
                System.out.println("[Dashboard] Có hàng mới lên sàn! Đang tải lại...");
                requestAuctions();
            }

            case "ERROR" -> {
                String msg = json.has("message") ? json.get("message").getAsString() : "Lỗi Server";
                System.err.println("[Dashboard] Lỗi từ Server: " + msg);
            }
        }
    }

    private void handleAuctionsList(JsonObject json) {
        if (!json.has("data")) return;
        JsonArray data = json.getAsJsonArray("data");

        Platform.runLater(() -> {
            allAuctions = copyAuctions(data);
            renderAuctions(getFilteredAuctions());
        });
    }

    private void renderAuctions(JsonArray data) {
        auctionContainer.getChildren().clear();
        stopDashboardCountdowns();

        if (data.isEmpty()) {
            showEmptyAuctionsMessage();
            return;
        }

        javafx.scene.layout.HBox runningRow = buildAuctionRow();
        javafx.scene.layout.HBox upcomingRow = buildAuctionRow();
        javafx.scene.layout.HBox endedRow = buildAuctionRow();

        for (com.google.gson.JsonElement element : data) {
            try {
                if (!element.isJsonObject()) continue;
                JsonObject auction = element.getAsJsonObject();
                String auctionId = getStringSafe(auction, "id");
                double currentPrice = auction.has("currentPrice") ? auction.get("currentPrice").getAsDouble() : 0.0;
                String status = getStringSafe(auction, "status");
                String startTimeStr = getStringSafe(auction, "startTime");
                String endTimeStr = getStringSafe(auction, "endTime");
                boolean isFollowing = auction.has("isFollowing") && auction.get("isFollowing").getAsBoolean();

                boolean isEnded = isEndedAuction(status, endTimeStr);
                boolean isUpcoming = !isEnded && isUpcomingAuction(status, startTimeStr);

                String targetTimeStr;
                if (isEnded) {
                    targetTimeStr = endTimeStr;
                } else if (isUpcoming) {
                    targetTimeStr = startTimeStr;
                } else {
                    targetTimeStr = endTimeStr;
                }

                String itemName = "Món hàng không xác định";
                String imagePath = "";
                String description = "";
                JsonObject itemJson = null;

                if (auction.has("item") && auction.get("item").isJsonObject()) {
                    JsonObject item = auction.getAsJsonObject("item");
                    itemJson = item;
                    itemName = getStringSafe(item, "name");
                    description = getStringSafe(item, "description");
                    if (item.has("images") && item.get("images").isJsonArray()) {
                        JsonArray imgs = item.getAsJsonArray("images");
                        if (!imgs.isEmpty()) imagePath = imgs.get(0).getAsString();
                    }
                }

                javafx.scene.layout.StackPane card = buildAuctionCard(
                        auctionId, itemName, description, currentPrice, imagePath, isFollowing,
                        targetTimeStr, isEnded, isUpcoming);
                if (isEnded) {
                    endedRow.getChildren().add(card);
                } else if (isUpcoming) {
                    upcomingRow.getChildren().add(card);
                } else {
                    runningRow.getChildren().add(card);
                }
            } catch (Exception e) {
                System.err.println("Lỗi vẽ thẻ đấu giá: " + e.getMessage());
            }
        }

        if (runningRow.getChildren().isEmpty() && upcomingRow.getChildren().isEmpty() && endedRow.getChildren().isEmpty()) {
            showEmptyAuctionsMessage();
        } else {
            addAuctionSection("Đang đấu giá", runningRow);
            addAuctionSection("Chuẩn bị đấu giá", upcomingRow);
            addAuctionSection("Đã kết thúc", endedRow);
            startDashboardCountdowns();
        }
    }

    private void stopDashboardCountdowns() {
        if (dashboardCountdownTimeline != null) {
            dashboardCountdownTimeline.stop();
            dashboardCountdownTimeline = null;
        }
        cardCountdownEntries.clear();
    }

    private void startDashboardCountdowns() {
        if (cardCountdownEntries.isEmpty()) {
            return;
        }
        tickDashboardCountdowns();
        dashboardCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickDashboardCountdowns()));
        dashboardCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        dashboardCountdownTimeline.play();
    }

    private void tickDashboardCountdowns() {
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
                entry.label.setText(String.format(
                        "%02d:%02d:%02d",
                        remaining.toHours(),
                        remaining.toMinutesPart(),
                        remaining.toSecondsPart()));
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

    private void addAuctionSection(String title, javafx.scene.layout.HBox row) {
        if (row.getChildren().isEmpty()) {
            return;
        }
        auctionContainer.getChildren().addAll(
                buildSectionHeader(title, "#111827", 0),
                buildHorizontalAuctionScroll(row)
        );
    }

    private Label buildSectionHeader(String text, String color, int topPadding) {
        Label header = new Label(text);
        header.getStyleClass().add("dashboard-section-title");
        header.setStyle("-fx-text-fill: " + color + "; -fx-padding: " + topPadding + " 0 0 0;");
        return header;
    }

    private javafx.scene.layout.HBox buildAuctionRow() {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(24);
        row.setFillHeight(false);
        row.setMinHeight(468);
        return row;
    }

    private javafx.scene.control.ScrollPane buildHorizontalAuctionScroll(javafx.scene.layout.HBox row) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(row);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("dashboard-auction-strip");
        scrollPane.setMinHeight(488);
        scrollPane.setPrefHeight(498);
        return scrollPane;
    }

    private void showEmptyAuctionsMessage() {
        Label emptyLabel = new Label(hasSearchKeyword()
                ? "Không tìm thấy phiên đấu giá phù hợp với từ khóa."
                : "Hiện chưa có phiên đấu giá nào trên sàn.");
        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-padding: 20;");
        auctionContainer.getChildren().add(emptyLabel);
    }

    private boolean isEndedAuction(String status, String endTimeStr) {
        if ("FINISHED".equals(status) || "CLOSED".equals(status) || "CANCELED".equals(status)
                || "CANCELLED".equals(status) || "PAID".equals(status) || "FAILED".equals(status)) {
            return true;
        }
        if (endTimeStr == null || endTimeStr.isBlank()) return false;
        try {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(endTimeStr);
            return java.time.LocalDateTime.now().isAfter(endTime);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isUpcomingAuction(String status, String startTimeStr) {
        if ("OPEN".equals(status) || "SCHEDULED".equals(status)) return true;
        if (startTimeStr == null || startTimeStr.isBlank()) return false;
        try {
            java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(startTimeStr);
            return java.time.LocalDateTime.now().isBefore(startTime);
        } catch (Exception ignored) {
            return false;
        }
    }
    private JsonArray getFilteredAuctions() {
        String keyword = TextUtil.normalizeSearchText(auctionSearchField != null ? auctionSearchField.getText() : "");
        if (keyword.isEmpty()) return allAuctions;

        JsonArray filtered = new JsonArray();
        for (JsonElement element : allAuctions) {
            if (!element.isJsonObject()) continue;
            JsonObject auction = element.getAsJsonObject();
            if (matchesSearch(auction, keyword)) {
                filtered.add(element);
            }
        }
        return filtered;
    }

    private boolean matchesSearch(JsonObject auction, String keyword) {
        StringBuilder content = new StringBuilder();
        appendSearchValue(content, getStringSafe(auction, "id"));
        appendSearchValue(content, getStringSafe(auction, "status"));
        appendSearchValue(content, getStringSafe(auction, "startTime"));
        appendSearchValue(content, getStringSafe(auction, "endTime"));
        if (auction.has("currentPrice")) appendSearchValue(content, auction.get("currentPrice").getAsString());

        if (auction.has("item") && auction.get("item").isJsonObject()) {
            JsonObject item = auction.getAsJsonObject("item");
            appendSearchValue(content, getStringSafe(item, "id"));
            appendSearchValue(content, getStringSafe(item, "name"));
            appendSearchValue(content, getStringSafe(item, "description"));
            appendSearchValue(content, getStringSafe(item, "type"));
            appendSearchValue(content, getStringSafe(item, "condition"));
            appendSearchValue(content, getStringSafe(item, "sellerFullName"));

            if (item.has("specifications") && item.get("specifications").isJsonObject()) {
                JsonObject specs = item.getAsJsonObject("specifications");
                for (String key : specs.keySet()) {
                    appendSearchValue(content, key);
                    appendSearchValue(content, getStringSafe(specs, key));
                }
            }
        }

        return TextUtil.normalizeSearchText(content.toString()).contains(keyword);
    }

    private void appendSearchValue(StringBuilder content, String value) {
        if (value != null && !value.isBlank()) {
            content.append(' ').append(value);
        }
    }

    private boolean hasSearchKeyword() {
        return auctionSearchField != null && !auctionSearchField.getText().trim().isEmpty();
    }

    private JsonArray copyAuctions(JsonArray source) {
        JsonArray copy = new JsonArray();
        for (JsonElement element : source) {
            copy.add(element.deepCopy());
        }
        return copy;
    }

    // ... (Giữ nguyên hàm handleRealtimeBidUpdate và mapStatus như cũ) ...

    private javafx.scene.layout.StackPane buildAuctionCard(String auctionId,
                                                         String itemName,
                                                         String description,
                                                         double currentPrice,
                                                         String imagePath,
                                                         boolean isFollowing,
                                                         String targetTimeStr,
                                                         boolean isEnded,
                                                         boolean isUpcoming) {
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

            LocalDateTime targetTime = parseDateTime(targetTimeStr);
            cardCountdownEntries.add(new CardCountdownEntry(timerValueLabel, targetTime, false, isUpcoming));
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

        Button favoriteButton = FollowHeartButtonFactory.create(isFollowing);
        favoriteButton.getStyleClass().add("dashboard-card-heart-btn");
        favoriteButton.setOnAction(e -> handleFavoriteAuction(auctionId, favoriteButton));

        HBox titleRow = new HBox(8, nameLabel, favoriteButton);
        titleRow.getStyleClass().add("dashboard-card-title-row");
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setMaxWidth(imageWidth);

        Label descLabel = new Label(truncateDescription(description));
        descLabel.getStyleClass().add("dashboard-card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(imageWidth);
        descLabel.setAlignment(Pos.TOP_LEFT);
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

    private void fitImageCover(ImageView imageView, Image image, double targetWidth, double targetHeight) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }
        imageView.setPreserveRatio(true);
        double scale = Math.max(targetWidth / image.getWidth(), targetHeight / image.getHeight());
        imageView.setFitWidth(image.getWidth() * scale);
        imageView.setFitHeight(image.getHeight() * scale);
    }

    private String formatCondition(String condition) {
        return switch (condition) {
            case "NEW" -> "Mới";
            case "LIKE_NEW" -> "Như mới";
            case "USED" -> "Đã sử dụng";
            case "DAMAGED" -> "Có hỏng hóc";
            default -> condition;
        };
    }

    private void handleFavoriteAuction(String auctionId, Button favoriteButton) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        boolean following = FollowHeartButtonFactory.isFollowing(favoriteButton);
        JsonObject request = new JsonObject();
        request.addProperty("action", following ? "UNFOLLOW_AUCTION" : "FOLLOW_AUCTION");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);

        setAuctionFollowing(auctionId, !following);
        FollowHeartButtonFactory.setFollowing(favoriteButton, !following);
    }

    private void setAuctionFollowing(String auctionId, boolean following) {
        for (JsonElement element : allAuctions) {
            if (!element.isJsonObject()) continue;
            JsonObject auction = element.getAsJsonObject();
            if (auctionId.equals(getStringSafe(auction, "id"))) {
                auction.addProperty("isFollowing", following);
                return;
            }
        }
    }

    private void handleViewAuction(String auctionId) {
        System.out.println("Đang mở chi tiết phiên: " + auctionId);

        // 1. Nhét cái ID vào túi hành lý
        AppNavigator.passData(auctionId);

        // 2. Chuyển xe sang màn hình Chi Tiết (Sếp nhớ đảm bảo có file AuctionDetail.fxml nhé)
        AppNavigator.navigate("AuctionDetail.fxml");
    }
    // ========================================================================
    // KHU VỰC CODE BỊ INTELIJ NUỐT MẤT ĐÃ ĐƯỢC KHÔI PHỤC
    // ========================================================================

    private void handleRealtimeBidUpdate(JsonObject json) {
        String auctionId = getStringSafe(json, "auctionId");
        double newPrice = json.has("newPrice") ? json.get("newPrice").getAsDouble() : 0.0;

        Platform.runLater(() -> {
            updateCachedAuctionPrice(auctionId, newPrice);
            // Duyệt qua tất cả các thẻ đang hiển thị trên Sảnh
            updateVisibleAuctionPrice(auctionContainer, auctionId, newPrice);
        });
    }

    private void updateCachedAuctionPrice(String auctionId, double newPrice) {
        if (auctionId == null || auctionId.isEmpty()) return;
        for (JsonElement element : allAuctions) {
            if (!element.isJsonObject()) continue;
            JsonObject auction = element.getAsJsonObject();
            if (auctionId.equals(getStringSafe(auction, "id"))) {
                auction.addProperty("currentPrice", newPrice);
                return;
            }
        }
    }

    private boolean updateVisibleAuctionPrice(Node node, String auctionId, double newPrice) {
        if (node instanceof VBox card && auctionId.equals(card.getUserData())) {
            // Tìm đúng cái Label hiện giá tiền và cập nhật số mới
            for (Node child : card.getChildren()) {
                if (child instanceof Label lbl && lbl.getStyleClass().contains("card-price")) {
                    lbl.setText(currencyFmt.format(newPrice) + " ₫");
                    lbl.setStyle("-fx-text-fill: #e67e22; -fx-scale-x: 1.05; -fx-scale-y: 1.05;");

                    // 0.5s sau trả về màu xanh như cũ
                    new Thread(() -> {
                        try { Thread.sleep(500); } catch (Exception ignored) {}
                        Platform.runLater(() -> lbl.setStyle(""));
                    }).start();
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

    // Hàm tiện ích lấy String an toàn, chống chết NullPointerException
    private String getStringSafe(JsonObject obj, String key) {
        return JsonUtil.getString(obj, key);
    }

    // Hàm dịch trạng thái sang Tiếng Việt (nếu sếp cần hiển thị lên thẻ)
    private String mapStatus(String status) {
        if (status == null) return "Không rõ";
        return switch (status) {
            case "RUNNING" -> "Đang đấu giá";
            case "OPEN" -> "Sắp mở";
            case "FINISHED", "CLOSED" -> "Đã kết thúc";
            case "ACTIVE" -> "Đang diễn ra";
            case "COMPLETED" -> "Đã kết thúc";
            case "CANCELLED" -> "Đã hủy";
            case "SCHEDULED" -> "Sắp diễn ra";
            default -> status;
        };
    }


}          
           
           
           
           
           
           
           
           
           
