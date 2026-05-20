package com.bidding.controller;
import javafx.scene.layout.VBox;
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
    @FXML private VBox autoBidContainer;
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
        // ================= KIỂM TRA CHẾ ĐỘ ĐẤU GIÁ =================
        boolean isReverse = false;
        if (this.currentSelectedAuction != null && this.currentSelectedAuction.has("isReverse")) {
            isReverse = this.currentSelectedAuction.get("isReverse").getAsBoolean();
        }

        double amount = 0; // Giá trị mặc định gửi đi

        if (isReverse) {
            // ĐẤU GIÁ NGƯỢC: Vượt rào! Không cần đọc ô nhập giá.
            // Server đã được sếp code tự lấy giá hiện tại rồi, nên ở đây sếp truyền số 0 lên cũng được!
            amount = 0;
        } else {
            // ĐẤU GIÁ THƯỜNG: Bắt buộc phải check ô nhập giá
            String rawAmount = bidAmountField.getText().trim();

            if (rawAmount.isEmpty()) {
                showBidError("Vui lòng nhập mức giá trước khi đặt.");
                return;
            }

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
        }
        // ==========================================================

        setBidLoading(true);

        JsonObject request = new JsonObject();
        request.addProperty("action", "BID");
        request.addProperty("auctionId", currentAuctionId);

        // Truyền amount (Với đấu giá ngược thì gửi 0, Server sẽ tự lờ đi và chốt giá tụt quần)
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
                        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION,
                                "Thành công",
                                "Ting ting! Sếp đã nạp tiền thành công.\nSố dư hiện tại: " + String.format("%,.0f", newBalance) + " VNĐ");
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
                    // (Tùy chọn) Hiện một cái Popup chúc mừng cho nó máu
                    showAlert(javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Thông báo kết quả",
                            "Phiên đấu giá đã kết thúc!\nChúc mừng đại gia: " + winner);
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

        // 1. LẤY TÊN NGƯỜI DẪN ĐẦU MỚI
        String winnerId = json.has("winnerId") ? json.get("winnerId").getAsString() : "---";

        String timestamp = getStringSafe(json, "timestamp");
        String displayTime = formatTimestampForDisplay(timestamp);

        // ================= VŨ KHÍ ANTI-SNIPING =================
        String newEndTimeStr = json.has("newEndTime") ? json.get("newEndTime").getAsString() : null;
        // =======================================================

        Platform.runLater(() -> {
            updateCurrentPrice(newPrice);

            // 2. CẬP NHẬT NHÃN NGƯỜI DẪN ĐẦU
            currentWinnerLabel.setText(winnerId);

            priceSeries.getData().add(new XYChart.Data<>(displayTime, newPrice));

            totalBidCount++;
            totalBidsLabel.setText(String.valueOf(totalBidCount));

            lastUpdateLabel.setText("Cập nhật lúc: " + displayTime);
            resultLabel.setText("");

            // ================= XỬ LÝ NHẢY ĐỒNG HỒ (ANTI-SNIPING) =================
            if (newEndTimeStr != null) {
                try {
                    this.endTime = java.time.LocalDateTime.parse(newEndTimeStr);

                    if (timeRemainingLabel != null) {
                        timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-scale-x: 1.3; -fx-scale-y: 1.3; -fx-font-weight: bold;");

                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.5));
                        pause.setOnFinished(e -> timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-scale-x: 1; -fx-scale-y: 1; -fx-font-weight: bold;"));
                        pause.play();
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse giờ Anti-Sniping: " + e.getMessage());
                }
            }
            // =====================================================================

            // ================= HIỆU ỨNG TỤT GIÁ (ĐẤU GIÁ NGƯỢC) =================
            // Kiểm tra xem phiên này có mang cờ Đấu giá ngược không
            if (this.currentSelectedAuction != null
                    && this.currentSelectedAuction.has("isReverse")
                    && this.currentSelectedAuction.get("isReverse").getAsBoolean()) {

                if (bidButton != null) {
                    // Cập nhật số tiền đang rớt giá lên mặt nút
                    bidButton.setText("🔨 CHỐT ĐƠN: " + currencyFmt.format(newPrice) + " ₫");

                    // Hiệu ứng giật nảy cái nút để ép tim người chơi
                    // Hiệu ứng giật nảy cái nút: Chớp sáng lóa màu kim loại rồi trả về màu Vàng Gold
                    bidButton.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f9df9f, #d4af37); -fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 19px; -fx-background-radius: 8; -fx-scale-x: 1.05; -fx-scale-y: 1.05; -fx-effect: dropshadow(three-pass-box, rgba(255, 255, 255, 0.8), 20, 0, 0, 0);");

                    javafx.animation.PauseTransition pauseBtn = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.3));
                    pauseBtn.setOnFinished(e -> bidButton.setStyle("-fx-background-color: linear-gradient(to bottom right, #f9df9f, #d4af37, #9e7f1e); -fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 18px; -fx-background-radius: 8; -fx-scale-x: 1; -fx-scale-y: 1; -fx-effect: dropshadow(three-pass-box, rgba(212, 175, 55, 0.5), 15, 0, 0, 0);"));
                    pauseBtn.play();
                }
            }
            // =====================================================================
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

                // ================= TÁCH LÀN THỜI GIAN VÀ BẮT CỜ ĐẤU GIÁ NGƯỢC =================
                String startTimeStr = auction.has("startTime") ? auction.get("startTime").getAsString() : null;
                String endTimeStr   = auction.has("endTime") ? auction.get("endTime").getAsString() : null;
                String winnerId     = auction.has("winnerId") ? auction.get("winnerId").getAsString() : "---";

                // Lấy cờ Đấu giá ngược từ JSON gửi về
                boolean isReverse = auction.has("isReverse") && auction.get("isReverse").getAsBoolean();

                final String finalItemName = itemName;
                final double finalPrice    = currentPrice;
                final String finalStatus   = status;
                final String finalStartTime= startTimeStr;
                final String finalEndTime  = endTimeStr;
                final String finalWinner   = winnerId;
                final boolean finalIsReverse = isReverse; // Tạo final để ném vào luồng UI

                Platform.runLater(() -> {
                    itemNameLabel.setText(finalItemName);
                    headerItemName.setText(finalItemName);
                    updateCurrentPrice(finalPrice);
                    updateStatusBadge(finalStatus);

                    // --- BỔ SUNG 3: CẬP NHẬT GIAO DIỆN & PHÂN LOẠI ĐẾM NGƯỢC ---
                    if ("OPEN".equals(finalStatus)) {
                        // Đang chờ mở -> Đếm ngược tới startTime
                        if (finalStartTime != null && !finalStartTime.isEmpty()) {
                            startCountdown(finalStartTime, "OPEN"); // Truyền thêm status
                        }
                        // Khóa mõm 2 nút đặt giá để chặn đánh lén
                        if (bidButton != null) bidButton.setDisable(true);
                        if (autoBidButton != null) autoBidButton.setDisable(true);
                    } else {
                        // Đang chạy hoặc đã xong -> Đếm ngược tới endTime
                        if (finalEndTime != null && !finalEndTime.isEmpty()) {
                            startCountdown(finalEndTime, finalStatus); // Truyền thêm status
                        }
                        // Bật nút đặt giá nếu đang diễn ra
                        if ("RUNNING".equals(finalStatus) || "ACTIVE".equals(finalStatus)) {
                            if (bidButton != null) bidButton.setDisable(false);
                            if (autoBidButton != null) autoBidButton.setDisable(false);
                        }
                    }

                    currentWinnerLabel.setText(finalWinner);

                    // ================= BIẾN HÌNH GIAO DIỆN =================
                    if (finalIsReverse) {
                        // 1. Giấu toàn bộ khu vực nhập giá và Auto-bid đi
                        if (bidAmountField != null) { bidAmountField.setVisible(false); bidAmountField.setManaged(false); }
                        if (autoBidContainer != null) {
                            autoBidContainer.setVisible(false);
                            autoBidContainer.setManaged(false);
                        }
                        if (autoBidButton != null) { autoBidButton.setVisible(false); autoBidButton.setManaged(false); }
                        if (maxBidField != null) { maxBidField.setVisible(false); maxBidField.setManaged(false); }
                        if (incrementField != null) { incrementField.setVisible(false); incrementField.setManaged(false); }

                        // 2. Biến nút Đặt giá thành nút CHỐT ĐƠN bự chà bá
                        // 2. Biến nút Đặt giá thành nút CHỐT ĐƠN bự chà bá, dát vàng Gradient
                        if (bidButton != null) {
                            bidButton.setText("🔨 CHỐT ĐƠN: " + currencyFmt.format(finalPrice) + " ₫");
                            bidButton.setStyle("-fx-background-color: linear-gradient(to bottom right, #f9df9f, #d4af37, #9e7f1e); -fx-text-fill: #05070a; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(212, 175, 55, 0.5), 15, 0, 0, 0);");
                        }
                    } else {
                        // Khôi phục lại giao diện bình thường (nếu người dùng vào phiên thường)
                        if (bidAmountField != null) { bidAmountField.setVisible(true); bidAmountField.setManaged(true); }
                        if (autoBidButton != null) { autoBidButton.setVisible(true); autoBidButton.setManaged(true); }
                        if (maxBidField != null) { maxBidField.setVisible(true); maxBidField.setManaged(true); }
                        if (incrementField != null) { incrementField.setVisible(true); incrementField.setManaged(true); }

                        if (bidButton != null) {
                            bidButton.setText("🔨 Đặt Giá Ngay");
                            bidButton.setStyle(""); // Trả về style mặc định
                        }
                    }
                    // =======================================================
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

    // Trong AuctionDetailController.java
    private void updateStatusBadge(String status) {
        statusBadgeHeader.getStyleClass().removeAll("badge-running", "badge-scheduled", "badge-closed", "badge-default");
        switch (status) {
            case "RUNNING", "ACTIVE" -> {
                statusBadgeHeader.setText("🟢 Đang diễn ra");
                statusBadgeHeader.getStyleClass().add("badge-running");
            }
            case "FINISHED", "CLOSED" -> { // Chấp nhận cả FINISHED theo chuẩn mới
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

    private void startCountdown(String targetTimeStr, String status) {
        if (countdownTimer != null) countdownTimer.stop();

        // Parse thời gian đích (có thể là startTime hoặc endTime tùy vào status)
        this.endTime = java.time.LocalDateTime.parse(targetTimeStr);

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    java.time.Duration duration = java.time.Duration.between(java.time.LocalDateTime.now(), endTime);

                    if (duration.isNegative() || duration.isZero()) {
                        // Khi hết giờ, tùy theo trạng thái mà báo chữ khác nhau
                        if ("OPEN".equals(status)) {
                            timeRemainingLabel.setText("ĐANG MỞ SẠP...");
                            timeRemainingLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                            // Nó sẽ báo chữ này cho đến khi User bấm F5 (refresh) để cập nhật lại phiên
                        } else {
                            timeRemainingLabel.setText("ĐÃ KẾT THÚC");
                            timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            if (bidButton != null) bidButton.setDisable(true);
                            if (autoBidButton != null) autoBidButton.setDisable(true);
                        }
                        countdownTimer.stop();
                    } else {
                        // Đang đếm ngược
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

        // ================= ÉP CSS HOÀNG GIA CHO DIALOG =================
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {}
        // ===============================================================

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
        lblCondition.setStyle("-fx-font-style: italic; -fx-text-fill: #6b7a90;"); // Chuyển sang màu xám sang trọng

        javafx.scene.control.Label lblPrice = new javafx.scene.control.Label("Giá khởi điểm: 0 đ");
        lblPrice.getStyleClass().add("price-value"); // Gọi class mạ vàng
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        javafx.scene.control.Label lblDesc = new javafx.scene.control.Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imgView, lblType, lblCondition, lblPrice, new javafx.scene.control.Separator(), lblDesc);

        if (item.has("specifications")) {
            JsonObject specs = item.getAsJsonObject("specifications");
            javafx.scene.layout.VBox extraBox = new javafx.scene.layout.VBox(8);

            // Xóa màu cứng, gán class info-card để nó nổi bóng 3D
            extraBox.getStyleClass().add("info-card");
            extraBox.setStyle("");

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
    // HÀM HỖ TRỢ HIỆN THÔNG BÁO DƯỚI CÙNG FILE
    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            // ================= ÉP CSS HOÀNG GIA =================
            try {
                alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                alert.getDialogPane().setStyle("-fx-background-color: #05070a;");
            } catch (Exception e) {
                System.err.println("Lỗi load CSS cho Alert");
            }
            // ====================================================

            alert.showAndWait();
        });
    }
}
