package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AuctionDetailController {

    // ─── FXML nodes — Top Bar ──────────────────────────────────────────────────
    @FXML private Label    headerItemName;
    @FXML private Label    headerAuctionId;
    @FXML private Label    statusBadgeHeader;

    // ─── FXML nodes — Info ────────────────────────────────────────────────────
    @FXML private Label    itemNameLabel;
    @FXML private Label    currentPriceLabel;
    @FXML private Label    timeRemainingLabel;
    @FXML private Label    currentWinnerLabel;
    @FXML private Label    totalBidsLabel;

    // ─── FXML nodes — Manual Bid ──────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Label     bidErrorLabel;
    @FXML private Button    bidButton;

    // ─── FXML nodes — Auto-Bid ────────────────────────────────────────────────
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label     autoBidErrorLabel;
    @FXML private Button    autoBidButton;
    // ─── State ────────────────────────────────────────────────────────────────
    private String currentAuctionId;
    private int    totalBidCount = 0;

    // THÊM DÒNG NÀY VÀO:
    private JsonObject currentSelectedAuction;
    // ─── FXML nodes — Result & Chart ──────────────────────────────────────────
    @FXML private Label                        resultLabel;
    @FXML private Label                        lastUpdateLabel;
    @FXML private LineChart<String, Number>    priceChart;
    @FXML private CategoryAxis                 timeAxis;
    @FXML private NumberAxis                   priceAxis;

    // ─── State ────────────────────────────────────────────────────────────────


    private XYChart.Series<String, Number> priceSeries;
    private javafx.animation.Timeline countdownTimer;
    private java.time.LocalDateTime endTime;
    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final UserSession   session       = UserSession.getInstance();
    private final NumberFormat  currencyFmt   =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private static final DateTimeFormatter DISPLAY_TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupChart();
        // 1. Tút lại nhan sắc cho Tiêu đề & Giá tiền
        headerItemName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        currentPriceLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Màu xanh lá uy tín
        itemNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        // 2. Mặc áo mới cho mấy cái nút bấm (Dùng lại đồ xịn sếp đã có sẵn)
        bidButton.getStyleClass().add("action-button");
        autoBidButton.getStyleClass().add("action-button");

        // 3. Bo góc, tạo viền cho mấy ô nhập số tiền
        bidAmountField.getStyleClass().add("detail-input");
        maxBidField.getStyleClass().add("detail-input");
        incrementField.getStyleClass().add("detail-input");

        // 4. Xóa phông nền trắng dại dại của biểu đồ
        priceChart.getStyleClass().add("custom-chart");
        // 1. Giành lại quyền nghe thông báo từ Server cho màn hình Chi Tiết
        networkClient.setMessageHandler(this::handleServerMessage);

        // 2. Mở "túi hành lý" từ AppNavigator ra để lấy cái ID sếp vừa bấm
        String auctionId = (String) AppNavigator.getData();

        if (auctionId != null) {
            System.out.println("Đã nhảy sang màn Chi Tiết. ID nhận được: " + auctionId);

            // 3. Truyền ID này vào cái hàm initData sếp đã viết sẵn ở dưới
            // để nó tự động load Lịch sử và vẽ thông tin món đồ lên màn hình!
            initData(auctionId);
        } else {
            System.err.println("Toang! Không nhận được ID phiên đấu giá nào cả.");
        }
    }

    /**
     * Được gọi từ MainController ngay sau khi load FXML,
     * để truyền auctionId và khởi tạo dữ liệu ban đầu.
     */
    public void initData(String auctionId) {
        this.currentAuctionId = auctionId;
        headerAuctionId.setText("ID: " + auctionId);
        requestAuctionHistory(auctionId);

        // Bắn lệnh lên server yêu cầu lấy thông tin và CHỜ...
        requestAuctions();
    }

    // Thêm hàm helper này vào (vì Claude quên viết hàm này trong file Detail)
    private void requestAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        networkClient.sendJson(request);
    }
    // ─── Chart Setup ──────────────────────────────────────────────────────────

    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu giá");
        priceChart.getData().add(priceSeries);
        priceChart.setTitle(null);

        // Tắt animation để tránh hiệu ứng lag khi update realtime
        priceChart.setAnimated(false);
    }

    // ─── Network — Outgoing ───────────────────────────────────────────────────

    private void requestAuctionHistory(String auctionId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTION_HISTORY");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);
    }

    @FXML
    private void handleBid() {
        String rawAmount = bidAmountField.getText().trim();

        if (rawAmount.isEmpty()) {
            showBidError("Vui lòng nhập mức giá trước khi đặt.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(rawAmount.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showBidError("Mức giá không hợp lệ. Vui lòng nhập số.");
            return;
        }

        if (amount <= 0) {
            showBidError("Mức giá phải lớn hơn 0.");
            return;
        }

        setBidLoading(true);

        // Lưu ý: Server đang dùng tên biến "itemId" nhưng truyền vào auctionId
        // (theo đúng đặc tả API đã cung cấp)
        JsonObject request = new JsonObject();
        request.addProperty("action", "BID");
        request.addProperty("auctionId", currentAuctionId);
        request.addProperty("amount", amount);
        networkClient.sendJson(request);
    }

    @FXML
    private void handleAutoBid() {
        String rawMaxBid    = maxBidField.getText().trim();
        String rawIncrement = incrementField.getText().trim();

        if (rawMaxBid.isEmpty() || rawIncrement.isEmpty()) {
            showAutoBidError("Vui lòng điền đầy đủ Giá tối đa và Bước giá.");
            return;
        }

        double maxBid;
        double increment;
        try {
            maxBid    = Double.parseDouble(rawMaxBid.replace(",", "").replace(".", ""));
            increment = Double.parseDouble(rawIncrement.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showAutoBidError("Giá trị không hợp lệ. Vui lòng nhập số.");
            return;
        }

        if (maxBid <= 0 || increment <= 0) {
            showAutoBidError("Giá tối đa và bước giá phải lớn hơn 0.");
            return;
        }

        setAutoBidLoading(true);

        JsonObject request = new JsonObject();
        request.addProperty("action", "REGISTER_AUTO_BID");
        request.addProperty("auctionId", currentAuctionId);
        request.addProperty("maxBid", maxBid);
        request.addProperty("increment", increment);
        networkClient.sendJson(request);
    }

    // ─── Network — Incoming ───────────────────────────────────────────────────

    private void handleServerMessage(JsonObject json) {

        if (!json.has("action")) return;
        String action = json.get("action").getAsString();

        switch (action) {
            // Thêm case này vào switch (action) trong handleServerMessage[cite: 14]
            case "DEPOSIT_REPLY" -> {
                String status = json.get("status").getAsString();
                if ("SUCCESS".equals(status)) {
                    double newBalance = json.get("newBalance").getAsDouble();

                    Platform.runLater(() -> {
                        // Hiện thông báo thành công
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("Ting ting! Sếp đã nạp tiền thành công.\nSố dư hiện tại: "
                                + String.format("%,.0f", newBalance) + " VNĐ");
                        alert.showAndWait();
                    });
                }
            }
            // Thêm vào switch (action) trong handleServerMessage của AuctionDetailController
            case "AUCTION_FINISHED" -> {
                String winner = json.get("winnerId").getAsString();

                Platform.runLater(() -> {
                    // 1. Dừng đồng hồ đếm ngược ngay lập tức
                    if (countdownTimer != null) countdownTimer.stop();

                    // 2. Hiện thông báo người thắng cuộc cực lớn
                    resultLabel.setText("🏆 PHIÊN ĐÃ KẾT THÚC! NGƯỜI THẮNG: " + winner);
                    resultLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-font-weight: bold;");

                    // 3. Cập nhật nhãn trạng thái sang màu đỏ "Kết thúc"
                    statusBadgeHeader.setText("🔴 Kết thúc");
                    statusBadgeHeader.getStyleClass().setAll("status-badge", "badge-closed");
                    // 4. Vô hiệu hóa toàn bộ nút đặt giá để không ai "phá bĩnh" được nữa
                    bidButton.setDisable(true);
                    autoBidButton.setDisable(true);

                    // (Tùy chọn) Hiện một cái Popup chúc mừng cho nó máu
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo kết quả");
                    alert.setHeaderText("Phiên đấu giá đã kết thúc!");
                    alert.setContentText("Chúc mừng đại gia: " + winner);
                    alert.show();
                });
            }
            case "AUCTION_HISTORY_REPLY" -> handleAuctionHistoryReply(json);
            case "BID_REPLY"             -> handleBidReply(json);
            case "REGISTER_AUTO_BID_REPLY" -> handleAutoBidReply(json);
            case "NEW_BID"   -> handleRealtimeBidUpdate(json);
            case "AUCTIONS_LIST"         -> handleAuctionsListForInfo(json);
            case "ERROR"                 -> handleError(json);
            default                      -> {}
        }
    }

    /**
     * Nhận lịch sử toàn bộ phiên — vẽ lại toàn bộ biểu đồ từ đầu.
     */
    private void handleAuctionHistoryReply(JsonObject json) {
        if (!json.has("data")) return;
        JsonArray data = json.getAsJsonArray("data");

        Platform.runLater(() -> {
            priceSeries.getData().clear();
            totalBidCount = 0;

            for (JsonElement element : data) {
                JsonObject point  = element.getAsJsonObject();
                String    rawTime = getStringSafe(point, "timestamp");
                double    price   = point.has("price")
                        ? point.get("price").getAsDouble() : 0.0;

                String displayTime = formatTimestampForDisplay(rawTime);
                priceSeries.getData().add(
                        new XYChart.Data<>(displayTime, price)
                );
                totalBidCount++;
            }

            totalBidsLabel.setText(String.valueOf(totalBidCount));

            // Cập nhật giá hiện tại từ điểm cuối cùng trong lịch sử
            if (!priceSeries.getData().isEmpty()) {
                XYChart.Data<String, Number> lastPoint =
                        priceSeries.getData().get(priceSeries.getData().size() - 1);
                double lastPrice = lastPoint.getYValue().doubleValue();
                updateCurrentPrice(lastPrice);
            }
        });
    }

    /**
     * Nhận reply sau khi đặt giá thủ công.
     */
    private void handleBidReply(JsonObject json) {
        String status = getStringSafe(json, "status");

        Platform.runLater(() -> {
            setBidLoading(false);
            if ("SUCCESS".equals(status)) {
                bidAmountField.clear();
                clearBidError();
                showResult("✅ Đặt giá thành công! Đang chờ cập nhật...");
            } else {
                showBidError("Đặt giá thất bại. Vui lòng thử lại.");
            }
        });
    }

    /**
     * Nhận reply sau khi đăng ký Auto-Bid.
     */
    private void handleAutoBidReply(JsonObject json) {
        String status = getStringSafe(json, "status");

        Platform.runLater(() -> {
            setAutoBidLoading(false);
            if ("SUCCESS".equals(status)) {
                maxBidField.clear();
                incrementField.clear();
                clearAutoBidError();
                showResult("🤖 Auto-Bid đã được kích hoạt thành công!");
                autoBidButton.setText("✅ Auto-Bid đang chạy");
                autoBidButton.setDisable(true);
            } else {
                showAutoBidError("Không thể kích hoạt Auto-Bid. Vui lòng thử lại.");
            }
        });
    }

    /**
     * Server chủ động broadcast khi có bid mới thành công.
     * Chỉ xử lý nếu auctionId khớp với phiên hiện tại.
     */
    private void handleRealtimeBidUpdate(JsonObject json) {
        String auctionId = getStringSafe(json, "auctionId");

        if (!auctionId.equals(currentAuctionId)) return;

        double newPrice  = json.has("newPrice") ? json.get("newPrice").getAsDouble() : 0.0;

        // 1. LẤY TÊN NGƯỜI DẪN ĐẦU MỚI (MỚI)
        String winnerId = json.has("winnerId") ? json.get("winnerId").getAsString() : "---";

        String timestamp = getStringSafe(json, "timestamp");
        String displayTime = formatTimestampForDisplay(timestamp);

        Platform.runLater(() -> {
            updateCurrentPrice(newPrice);

            // 2. CẬP NHẬT NHÃN NGƯỜI DẪN ĐẦU (MỚI)
            currentWinnerLabel.setText(winnerId);

            priceSeries.getData().add(new XYChart.Data<>(displayTime, newPrice));

            totalBidCount++;
            totalBidsLabel.setText(String.valueOf(totalBidCount));

            lastUpdateLabel.setText("Cập nhật lúc: " + displayTime);
            resultLabel.setText("");
        });
    }

    /**
     * Khi nhận AUCTIONS_LIST (do MainController gọi hoặc refresh),
     * tìm auction tương ứng để cập nhật tên sản phẩm và trạng thái.
     */
    private void handleAuctionsListForInfo(JsonObject json) {
        if (!json.has("data")) return;
        JsonArray data = json.getAsJsonArray("data");

        for (JsonElement element : data) {
            JsonObject auction = element.getAsJsonObject();
            String id = getStringSafe(auction, "id");

            if (id.equals(currentAuctionId)) {
                this.currentSelectedAuction = auction;
                String itemName = "Không rõ tên";
                if (auction.has("item") && auction.get("item").isJsonObject()) {
                    JsonObject item = auction.getAsJsonObject("item");
                    itemName = getStringSafe(item, "name");
                }

                double currentPrice = auction.has("currentPrice")
                        ? auction.get("currentPrice").getAsDouble() : 0.0;
                String status       = getStringSafe(auction, "status");

                // --- BỔ SUNG 1: LẤY THÔNG TIN TỪ JSON ---
                String endTimeStr = auction.has("endTime") ? auction.get("endTime").getAsString() : null;
                String winnerId   = auction.has("winnerId") ? auction.get("winnerId").getAsString() : "---";

                final String finalItemName = itemName;
                final double finalPrice    = currentPrice;
                final String finalStatus   = status;

                // --- BỔ SUNG 2: TẠO BIẾN FINAL ĐỂ ĐƯA VÀO LUỒNG UI ---
                final String finalEndTime  = endTimeStr;
                final String finalWinner   = winnerId;

                Platform.runLater(() -> {
                    itemNameLabel.setText(finalItemName);
                    headerItemName.setText(finalItemName);
                    updateCurrentPrice(finalPrice);
                    updateStatusBadge(finalStatus);

                    // --- BỔ SUNG 3: CẬP NHẬT GIAO DIỆN ---
                    if (finalEndTime != null && !finalEndTime.isEmpty()) {
                        startCountdown(finalEndTime); // Bật đồng hồ tạch tạch
                    }
                    currentWinnerLabel.setText(finalWinner); // Vinh danh đại gia
                });
                break;
            }
        }
    }

    private void handleError(JsonObject json) {
        String message = getStringSafe(json, "message");

        Platform.runLater(() -> {
            setBidLoading(false);
            setAutoBidLoading(false);

            // Phân loại lỗi để hiển thị đúng chỗ
            if (message.contains("đặt giá") || message.contains("BID")
                    || message.contains("giá")) {
                showBidError(message);
            } else if (message.contains("Auto") || message.contains("auto")) {
                showAutoBidError(message);
            } else {
                showResult("❌ " + message);
            }
        });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        // Dùng cái thuyền Navigator anh em mình đã đóng
        AppNavigator.navigate("Main.fxml");
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void updateCurrentPrice(double price) {
        currentPriceLabel.setText(currencyFmt.format(price) + " ₫");
    }

    private void updateStatusBadge(String status) {
        statusBadgeHeader.getStyleClass().removeAll(
                "badge-running", "badge-scheduled", "badge-closed", "badge-default"
        );
        switch (status) {
            case "RUNNING" -> {
                statusBadgeHeader.setText("🟢 Đang diễn ra");
                statusBadgeHeader.getStyleClass().add("badge-running");
            }
            case "SCHEDULED" -> {
                statusBadgeHeader.setText("🕐 Sắp diễn ra");
                statusBadgeHeader.getStyleClass().add("badge-scheduled");
            }
            case "CLOSED" -> {
                statusBadgeHeader.setText("🔴 Đã kết thúc");
                statusBadgeHeader.getStyleClass().add("badge-closed");
            }
            default -> {
                statusBadgeHeader.setText("⚪ " + status);
                statusBadgeHeader.getStyleClass().add("badge-default");
            }
        }
    }

    private void showBidError(String message) {
        bidErrorLabel.setText(message);
        bidErrorLabel.setVisible(true);
    }

    private void clearBidError() {
        bidErrorLabel.setText("");
        bidErrorLabel.setVisible(false);
    }

    private void showAutoBidError(String message) {
        autoBidErrorLabel.setText(message);
        autoBidErrorLabel.setVisible(true);
    }

    private void clearAutoBidError() {
        autoBidErrorLabel.setText("");
        autoBidErrorLabel.setVisible(false);
    }

    private void showResult(String message) {
        resultLabel.setText(message);
        resultLabel.setVisible(true);
    }

    private void setBidLoading(boolean loading) {
        bidButton.setDisable(loading);
        bidButton.setText(loading ? "Đang gửi..." : "🔨 Đặt Giá Ngay");
        if (!loading) clearBidError();
    }

    private void setAutoBidLoading(boolean loading) {
        autoBidButton.setDisable(loading);
        autoBidButton.setText(loading ? "Đang kích hoạt..." : "🤖 Kích Hoạt Auto-Bid");
        if (!loading) clearAutoBidError();
    }

    private String formatTimestampForDisplay(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return LocalDateTime.now().format(DISPLAY_TIME_FMT);
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTimestamp, ISO_FMT);
            return dt.format(DISPLAY_TIME_FMT);
        } catch (Exception e) {
            // Nếu parse lỗi, trả về nguyên chuỗi gốc
            return isoTimestamp.length() > 8
                    ? isoTimestamp.substring(11, 19)
                    : isoTimestamp;
        }
    }

    private String getStringSafe(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : "";
    }
    // Trong file AuctionDetailController.java

    private void startCountdown(String endTimeStr) {
        if (countdownTimer != null) countdownTimer.stop();

        // Parse thời gian kết thúc từ Server gửi về
        this.endTime = java.time.LocalDateTime.parse(endTimeStr);

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    java.time.Duration duration = java.time.Duration.between(java.time.LocalDateTime.now(), endTime);

                    if (duration.isNegative() || duration.isZero()) {
                        // Khi hết giờ[cite: 13]
                        timeRemainingLabel.setText("ĐÃ KẾT THÚC");
                        timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

                        if (bidButton != null) bidButton.setDisable(true);
                        countdownTimer.stop();
                    } else {
                        // Đang đếm ngược[cite: 13]
                        long h = duration.toHours();
                        long m = duration.toMinutesPart();
                        long s = duration.toSecondsPart();
                        timeRemainingLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
                    }
                })
        );
        countdownTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        countdownTimer.play();
    }
    @FXML
    private void handleViewAuctionItemDetails() {
        // Lấy cục JSON của phiên đấu giá đang được chọn trên màn hình
        // (Sếp nhớ thay tên biến `currentSelectedAuction` thành biến sếp đang dùng nhé)
        if (currentSelectedAuction == null || !currentSelectedAuction.has("item")) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Lỗi", "Không tìm thấy thông tin vật phẩm!");
            return;
        }

        // Móc cục thông tin món đồ ra
        JsonObject item = currentSelectedAuction.getAsJsonObject("item");

        // ================= ĐOẠN DƯỚI NÀY Y HỆT BÊN INVENTORY =================
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Chi Tiết Vật Phẩm Đấu Giá");
        dialog.setHeaderText(item.has("name") ? item.get("name").getAsString() : "Không tên");

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setStyle("-fx-padding: 20; -fx-font-size: 15px;");
        vbox.setPrefWidth(500);

        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
        imgView.setFitWidth(460);
        imgView.setFitHeight(300);
        imgView.setPreserveRatio(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                java.io.File file = new java.io.File(imgPath);
                if (file.exists()) {
                    imgView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                }
            } catch (Exception e) { System.err.println("Lỗi load ảnh!"); }
        } else {
            vbox.getChildren().add(new javafx.scene.control.Label("(Chưa có hình ảnh)"));
        }

        javafx.scene.control.Label lblType = new javafx.scene.control.Label("Loại: " + (item.has("type") ? item.get("type").getAsString() : "---"));

        String conditionText = item.has("condition") ? item.get("condition").getAsString() : "---";
        if ("NEW".equals(conditionText)) conditionText = "Mới 100%";
        else if ("USED".equals(conditionText)) conditionText = "Đã sử dụng";
        javafx.scene.control.Label lblCondition = new javafx.scene.control.Label("Tình trạng: " + conditionText);
        lblCondition.setStyle("-fx-font-style: italic; -fx-text-fill: #e67e22; -fx-font-weight: bold;");

        javafx.scene.control.Label lblPrice = new javafx.scene.control.Label("Giá khởi điểm: 0 đ");
        lblPrice.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        javafx.scene.control.Label lblDesc = new javafx.scene.control.Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imgView, lblType, lblCondition, lblPrice, new javafx.scene.control.Separator(), lblDesc);

        if (item.has("specifications")) {
            JsonObject specs = item.getAsJsonObject("specifications");
            javafx.scene.layout.VBox extraBox = new javafx.scene.layout.VBox(8);
            extraBox.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-color: #f1f8e9; -fx-padding: 10; -fx-background-radius: 5;");
            extraBox.getChildren().add(new javafx.scene.control.Label("📋 Thông số chi tiết:"));
            for (String key : specs.keySet()) {
                String value = specs.get(key).getAsString();
                extraBox.getChildren().add(new javafx.scene.control.Label("  • " + key + ": " + value));
            }
            vbox.getChildren().add(extraBox);
        }

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }
    // HÀM HỖ TRỢ HIỆN THÔNG BÁO DƯỚI CÙNG FILE
    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
