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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.bidding.model.UserSession;
import com.bidding.controller.MainController;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class DashboardController {

    @FXML private FlowPane auctionContainer;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

    @FXML
    public void initialize() {
        // ÉP BUỘC NetworkClient phải gửi tin nhắn cho TÔI (Dashboard)
        networkClient.setMessageHandler(this::handleServerMessage);

        // Gửi lệnh đòi danh sách ngay
        requestAuctions();
    }

    @FXML
    private void handleRefresh() {
        // Trước khi refresh, phải đảm bảo mình đang cầm "cái tai"
        networkClient.setMessageHandler(this::handleServerMessage);
        requestAuctions();
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
            case "REALTIME_BID_UPDATE" -> handleRealtimeBidUpdate(json);

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
            auctionContainer.getChildren().clear();

            if (data.isEmpty()) {
                Label emptyLabel = new Label("Hiện chưa có phiên đấu giá nào đang diễn ra.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-padding: 20;");
                auctionContainer.getChildren().add(emptyLabel);
                return;
            }

            for (JsonElement element : data) {
                try {
                    JsonObject auction = element.getAsJsonObject();
                    String auctionId = getStringSafe(auction, "id");
                    double currentPrice = auction.has("currentPrice") ? auction.get("currentPrice").getAsDouble() : 0.0;
                    String status = getStringSafe(auction, "status");

                    String itemName = "Món hàng không xác định";
                    String imagePath = "";

                    if (auction.has("item") && auction.get("item").isJsonObject()) {
                        JsonObject item = auction.getAsJsonObject("item");
                        itemName = getStringSafe(item, "name");

                        // Lấy ảnh đầu tiên trong mảng images
                        if (item.has("images") && item.get("images").isJsonArray()) {
                            JsonArray imgs = item.getAsJsonArray("images");
                            if (!imgs.isEmpty()) imagePath = imgs.get(0).getAsString();
                        }
                    }

                    VBox card = buildAuctionCard(auctionId, itemName, currentPrice, status, imagePath);
                    auctionContainer.getChildren().add(card);
                } catch (Exception e) {
                    System.err.println("Lỗi vẽ thẻ đấu giá: " + e.getMessage());
                }
            }
        });
    }

    // ... (Giữ nguyên hàm handleRealtimeBidUpdate và mapStatus như cũ) ...

    private VBox buildAuctionCard(String auctionId, String itemName, double currentPrice, String status, String imagePath) {
        // 1. TẠO THẺ (CARD) - Kích thước chuẩn chỉnh để dàn đều
        VBox card = new VBox(10);
        card.getStyleClass().add("auction-card");
        card.setPrefSize(240, 320);
        card.setMinSize(240, 320);
        card.setMaxSize(240, 320);
        card.setUserData(auctionId);

        // 2. XỬ LÝ ẢNH (Bí quyết lột xác ở đây)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(210);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false); // Ép đầy khung, không sợ thừa viền trắng
        imageView.setSmooth(true);

        if (!imagePath.isEmpty()) {
            File file = new File(imagePath);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        }

        // Bọc ảnh để bo tròn góc siêu mượt
        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane(imageView);
        imageContainer.setPrefSize(210, 150);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(210, 150);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imageContainer.setClip(clip);

        // 3. TÊN SẢN PHẨM VÀ GIÁ
        Label nameLabel = new Label(itemName);
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true); // Xuống dòng nếu tên quá dài

        Label priceLabel = new Label("💰 " + currencyFmt.format(currentPrice) + " ₫");
        priceLabel.getStyleClass().add("card-price"); // Tự động ăn màu xanh lá từ style.css

        // 4. LÒ XO MA THUẬT (Đẩy nút bấm thẳng xuống đáy thẻ)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 5. NÚT VÀO XEM
        Button viewBtn = new Button("👁 Vào xem");
        viewBtn.getStyleClass().add("card-view-btn");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> handleViewAuction(auctionId));

        // 6. GOM TẤT CẢ LẠI
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, spacer, viewBtn);
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
            // Duyệt qua tất cả các thẻ đang hiển thị trên Sảnh
            for (javafx.scene.Node node : auctionContainer.getChildren()) {
                if (node instanceof VBox card && auctionId.equals(card.getUserData())) {
                    // Tìm đúng cái Label hiện giá tiền và cập nhật số mới
                    for (javafx.scene.Node child : card.getChildren()) {
                        if (child instanceof Label lbl && lbl.getStyleClass().contains("card-price")) {
                            lbl.setText("💰 " + currencyFmt.format(newPrice) + " ₫");
                            // Hiệu ứng nháy màu cam nhẹ để User biết giá vừa thay đổi
                            lbl.setStyle("-fx-text-fill: #e67e22; -fx-scale-x: 1.1; -fx-scale-y: 1.1;");

                            // 0.5s sau trả về màu xanh như cũ
                            new Thread(() -> {
                                try { Thread.sleep(500); } catch (Exception ignored) {}
                                Platform.runLater(() -> lbl.setStyle(""));
                            }).start();
                            break;
                        }
                    }
                    break;
                }
            }
        });
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
           
           
           
           
           
           
           
           
           