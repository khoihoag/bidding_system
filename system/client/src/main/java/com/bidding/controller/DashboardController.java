package com.bidding.controller;

import com.bidding.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
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
import javafx.stage.Stage;
import com.bidding.model.UserSession;
import com.bidding.controller.MainController;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.Locale;

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
            dashboardUsernameLabel.setText(username != null && !username.isBlank() ? username : "Người info");
        }
        if (dashboardHandleLabel != null) {
            dashboardHandleLabel.setText(username != null && !username.isBlank() ? username : "owaycwhmanh");
        }
        updateDashboardBalance();
    }

    private void updateDashboardBalance() {
        if (dashboardBalanceLabel == null) return;
        dashboardBalanceLabel.setText(currencyFmt.format(UserSession.getInstance().getBalance()) + " đ");
    }

    private void setupSearch() {
        if (auctionSearchField == null) return;
        auctionSearchField.textProperty().addListener((obs, oldText, newText) -> renderAuctions(getFilteredAuctions()));
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

                boolean isEnded = isEndedAuction(status, endTimeStr);
                boolean isUpcoming = !isEnded && isUpcomingAuction(status, startTimeStr);

                String timeLabelText;
                String targetTimeStr;
                if (isEnded) {
                    timeLabelText = "Đã kết thúc: ";
                    targetTimeStr = endTimeStr;
                } else if (isUpcoming) {
                    timeLabelText = "Sắp mở: ";
                    targetTimeStr = startTimeStr;
                } else {
                    timeLabelText = "Kết thúc: ";
                    targetTimeStr = endTimeStr;
                }

                String itemName = "Món hàng không xác định";
                String imagePath = "";

                if (auction.has("item") && auction.get("item").isJsonObject()) {
                    JsonObject item = auction.getAsJsonObject("item");
                    itemName = getStringSafe(item, "name");
                    if (item.has("images") && item.get("images").isJsonArray()) {
                        JsonArray imgs = item.getAsJsonArray("images");
                        if (!imgs.isEmpty()) imagePath = imgs.get(0).getAsString();
                    }
                }

                javafx.scene.layout.VBox card = buildAuctionCard(auctionId, itemName, currentPrice, status, imagePath, timeLabelText, targetTimeStr);
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
        }
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
        row.setMinHeight(370);
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
        scrollPane.setMinHeight(390);
        scrollPane.setPrefHeight(400);
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
        String keyword = normalizeSearchText(auctionSearchField != null ? auctionSearchField.getText() : "");
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

        return normalizeSearchText(content.toString()).contains(keyword);
    }

    private void appendSearchValue(StringBuilder content, String value) {
        if (value != null && !value.isBlank()) {
            content.append(' ').append(value);
        }
    }

    private boolean hasSearchKeyword() {
        return auctionSearchField != null && !auctionSearchField.getText().trim().isEmpty();
    }

    private String normalizeSearchText(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private JsonArray copyAuctions(JsonArray source) {
        JsonArray copy = new JsonArray();
        for (JsonElement element : source) {
            copy.add(element.deepCopy());
        }
        return copy;
    }

    // ... (Giữ nguyên hàm handleRealtimeBidUpdate và mapStatus như cũ) ...

    private VBox buildAuctionCard(String auctionId, String itemName, double currentPrice, String status, String imagePath, String timeLabelText, String targetTimeStr){
        VBox card = new VBox(14);
        card.getStyleClass().addAll("auction-card", "dashboard-auction-card");
        card.setPrefWidth(312);
        card.setMinWidth(312);
        card.setMaxWidth(312);
        card.setMinHeight(374);
        card.setUserData(auctionId);

        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane();
        imageContainer.getStyleClass().add("dashboard-card-image-box");
        imageContainer.setPrefSize(274, 180);
        imageContainer.setMinSize(274, 180);
        imageContainer.setMaxSize(274, 180);

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitWidth(274);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        if (imagePath != null && !imagePath.isEmpty()) {
            java.io.File file = new java.io.File(imagePath);
            if (file.exists()) {
                imageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
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

        VBox body = new VBox(14);
        body.getStyleClass().add("dashboard-card-body");
        VBox.setVgrow(body, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(itemName);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setMinHeight(26);
        nameLabel.setMaxHeight(52);
        nameLabel.setMaxWidth(260);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label idLabel = new Label(auctionId);
        idLabel.getStyleClass().add("dashboard-card-id");
        idLabel.setWrapText(true);
        idLabel.setMaxWidth(260);
        idLabel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button viewBtn = new Button("Xem chi tiết");
        viewBtn.getStyleClass().add("card-view-btn");
        viewBtn.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        viewBtn.setOnAction(e -> handleViewAuction(auctionId));

        body.getChildren().addAll(nameLabel, idLabel, spacer, viewBtn);
        card.getChildren().addAll(imageContainer, body);

        return card;
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
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "";
    }

    // Hàm dịch trạng thái sang Tiếng Việt (nếu sếp cần hiển thị lên thẻ)
    private String mapStatus(String status) {
        if (status == null) return "Không rõ";
        return switch (status) {
            case "ACTIVE" -> "Đang diễn ra";
            case "COMPLETED" -> "Đã kết thúc";
            case "CANCELLED" -> "Đã hủy";
            case "SCHEDULED" -> "Sắp diễn ra";
            default -> status;
        };
    }


}          
           
           
           
           
           
           
           
           
           
