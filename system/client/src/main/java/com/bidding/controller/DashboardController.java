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

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private JsonArray allAuctions = new JsonArray();

    @FXML
    public void initialize() {
        networkClient.setMessageHandler(this::handleServerMessage);
        setupSearch();

        // Chỉ giữ lại 2 dòng này thôi, bỏ hết mấy dòng .bind() đi
        setupCarousel();
        requestAuctions();
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

        Label runningHeader = buildSectionHeader("PHIÊN ĐANG ĐẤU GIÁ", "#d4af37", 10);
        javafx.scene.layout.HBox runningPane = buildAuctionRow();

        Label upcomingHeader = buildSectionHeader("PHIÊN CHUẨN BỊ ĐẤU GIÁ", "#d4af37", 30);
        javafx.scene.layout.HBox upcomingPane = buildAuctionRow();

        Label endedHeader = buildSectionHeader("PHIÊN ĐÃ KẾT THÚC", "#95a5a6", 30);
        javafx.scene.layout.HBox endedPane = buildAuctionRow();

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
                    endedPane.getChildren().add(card);
                } else if (isUpcoming) {
                    upcomingPane.getChildren().add(card);
                } else {
                    runningPane.getChildren().add(card);
                }
            } catch (Exception e) {
                System.err.println("Lỗi vẽ thẻ đấu giá: " + e.getMessage());
            }
        }

        if (!runningPane.getChildren().isEmpty()) {
            auctionContainer.getChildren().addAll(runningHeader, buildHorizontalAuctionScroll(runningPane));
        }
        if (!upcomingPane.getChildren().isEmpty()) {
            auctionContainer.getChildren().addAll(upcomingHeader, buildHorizontalAuctionScroll(upcomingPane));
        }
        if (!endedPane.getChildren().isEmpty()) {
            auctionContainer.getChildren().addAll(endedHeader, buildHorizontalAuctionScroll(endedPane));
        }

        if (runningPane.getChildren().isEmpty() && upcomingPane.getChildren().isEmpty() && endedPane.getChildren().isEmpty()) {
            showEmptyAuctionsMessage();
        }
    }

    private Label buildSectionHeader(String text, String color, int topPadding) {
        Label header = new Label(text);
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-padding: " + topPadding + " 0 10 5;");
        return header;
    }

    private javafx.scene.layout.HBox buildAuctionRow() {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(20);
        row.setFillHeight(false);
        row.setMinHeight(380);
        return row;
    }

    private javafx.scene.control.ScrollPane buildHorizontalAuctionScroll(javafx.scene.layout.HBox row) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(row);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0 0 8 0;");
        scrollPane.setMinHeight(390);
        scrollPane.setPrefHeight(600);
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
        // 1. TẠO THẺ (CARD) - Tối giản, không ép cứng chiều cao
        VBox card = new VBox(15);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(320); // Mở rộng thẻ
        // Xóa sạch minSize, maxSize để thẻ tự co giãn theo độ dài của tên tác phẩm
        card.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 15;");
        card.setUserData(auctionId);

        // 2. KHU VỰC ẢNH SẢN PHẨM - Ép dáng dọc khổng lồ 280x350
        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane();
        imageContainer.setPrefSize(280, 350);
        imageContainer.setMinSize(280, 350);
        // Nền xám cực kỳ nhạt, tàng hình làm không gian tranh
        imageContainer.setStyle("-fx-background-color: #F9FAFB;");

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(350);
        imageView.setPreserveRatio(true); // KHÔNG CHO PHÉP MÉO ẢNH
        imageView.setSmooth(true);

        if (imagePath != null && !imagePath.isEmpty()) {
            java.io.File file = new java.io.File(imagePath);
            if (file.exists()) {
                imageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                imageContainer.getChildren().add(imageView);
            } else {
                Label noImg = new Label("Lỗi hiển thị ảnh");
                noImg.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic;");
                imageContainer.getChildren().add(noImg);
            }
        } else {
            Label noImg = new Label("Không có ảnh");
            noImg.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 14px;");
            imageContainer.getChildren().add(noImg);
        }

        // Bỏ luôn cái Clip bo góc tròn đi, Sotheby's dùng góc vuông hoặc vuông nhẹ cho tranh

        // 3. THÔNG TIN SẢN PHẨM (Xóa sạch Emoji)
        Label nameLabel = new Label(itemName);
        nameLabel.setWrapText(true);
        // Ép font có chân cực kỳ quý tộc
        nameLabel.setStyle("-fx-font-family: 'Georgia', 'Times New Roman', serif; -fx-font-size: 20px; -fx-text-fill: #111111;");
        nameLabel.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Giá - Font hiện đại nét căng
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
        Label priceLabel = new Label(fmt.format(currentPrice) + " ₫");
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #111111;");

        // Lược bớt chữ T trong ngày tháng
        String displayTime = targetTimeStr.replace("T", " ");
        int dotIdx = displayTime.indexOf(".");
        if(dotIdx > 0) displayTime = displayTime.substring(0, dotIdx);

        // Thời gian - Chữ xám thanh lịch
        Label timeLabel = new Label(timeLabelText + displayTime);
        timeLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        // 4. LÒ XO MA THUẬT (Giữ nguyên)
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // 5. NÚT VÀO XEM (Lõi trắng viền đen)
        Button viewBtn = new Button("VÀO XEM CHI TIẾT");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #111111; -fx-border-color: #D1D5DB; -fx-border-width: 1; -fx-font-size: 12px; -fx-padding: 10; -fx-cursor: hand; -fx-border-radius: 0; -fx-background-radius: 0;");
        viewBtn.setOnAction(e -> handleViewAuction(auctionId));

        // Hover chuột vào nút: Viền đen đậm lên
        viewBtn.setOnMouseEntered(e -> viewBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #111111; -fx-border-color: #111111; -fx-border-width: 1; -fx-font-size: 12px; -fx-padding: 10; -fx-cursor: hand; -fx-border-radius: 0; -fx-background-radius: 0;"));
        viewBtn.setOnMouseExited(e -> viewBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #111111; -fx-border-color: #D1D5DB; -fx-border-width: 1; -fx-font-size: 12px; -fx-padding: 10; -fx-cursor: hand; -fx-border-radius: 0; -fx-background-radius: 0;"));

        // 6. GẮN TẤT CẢ VÀO THẺ
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, timeLabel, spacer, viewBtn);

        // Hover chuột vào cả thẻ: Bo viền đen nhám
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #111111; -fx-border-width: 1; -fx-padding: 14; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #FFFFFF; -fx-border-width: 0; -fx-padding: 15;"));

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
                    lbl.setText("💰 " + currencyFmt.format(newPrice) + " ₫");
                    // Hiệu ứng nháy màu cam nhẹ để User biết giá vừa thay đổi
                    lbl.setStyle("-fx-text-fill: #e67e22; -fx-scale-x: 1.1; -fx-scale-y: 1.1;");

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
           
           
           
           
           
           
           
           
           
