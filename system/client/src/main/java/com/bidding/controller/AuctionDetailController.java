package com.bidding.controller;

import com.bidding.controller.auctiondetail.*;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane; // ĐÃ THÊM IMPORT
import javafx.scene.control.SplitPane;  // ĐÃ THÊM IMPORT
import javafx.scene.layout.VBox;
import java.io.File;

public class AuctionDetailController {
    @FXML private Button btnFollow;
    // ================= BIẾN CỦA MÀN 1 (SOTHEBY'S SHOWCASE) =================
    @FXML private ScrollPane showcaseView;
    @FXML private SplitPane tradingRoomView;
    @FXML private javafx.scene.image.ImageView showcaseImageView;
    @FXML private Label showcaseDescLabel;
    @FXML private VBox showcaseSpecsBox;
    @FXML private Label showcaseNameLabel;
    @FXML private Label showcaseTimeLabel;
    @FXML private Label showcasePriceLabel;
    @FXML private Label showcaseWinnerLabel;

    // ================= BIẾN CỦA MÀN 2 (TRADING ROOM - PHÒNG GIAO DỊCH) =================
    @FXML private VBox autoBidContainer;
    @FXML private Label headerAuctionId;
    @FXML private Label statusBadgeHeader;
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private Label currentWinnerLabel;
    @FXML private Label totalBidsLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label bidErrorLabel;
    @FXML private Button bidButton;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label autoBidErrorLabel;
    @FXML private Button autoBidButton;
    @FXML private Label resultLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;

    private final AuctionDetailState state = new AuctionDetailState();
    private AuctionDetailNetworkGateway network;
    private AuctionDetailUiPresenter ui;
    private AuctionDetailChartBinder chartBinder;
    private AuctionDetailCountdown countdown;
    private AuctionDetailBidHandler bidHandler;
    private AuctionDetailResponseHandler responseHandler;

    @FXML
    public void initialize() {
        ui = new AuctionDetailUiPresenter(btnFollow,
                state, itemNameLabel, currentPriceLabel, statusBadgeHeader,
                currentWinnerLabel, totalBidsLabel, timeRemainingLabel, resultLabel, lastUpdateLabel,
                bidErrorLabel, autoBidErrorLabel, bidAmountField, maxBidField, incrementField,
                bidButton, autoBidButton, autoBidContainer);

        // Binding dữ liệu từ Màn 2 ra Màn 1 để giá nhảy Realtime
        showcaseNameLabel.textProperty().bind(itemNameLabel.textProperty());
        showcasePriceLabel.textProperty().bind(currentPriceLabel.textProperty());
        showcaseTimeLabel.textProperty().bind(timeRemainingLabel.textProperty());
        showcaseWinnerLabel.textProperty().bind(currentWinnerLabel.textProperty());

        network = new AuctionDetailNetworkGateway();
        chartBinder = new AuctionDetailChartBinder(state, priceChart, ui);
        countdown = new AuctionDetailCountdown(
                state, timeRemainingLabel, bidButton, autoBidButton);
        bidHandler = new AuctionDetailBidHandler(state, network, ui);
        responseHandler = new AuctionDetailResponseHandler(
                AuctionDetailController.class, state, ui, chartBinder, countdown, this);

        chartBinder.setupChart();
        ui.applyInitialStyles();
        network.registerMessageHandler(responseHandler::handle);

        String auctionId = (String) AppNavigator.getData();
        if (auctionId != null) {
            initData(auctionId);
        } else {
            System.err.println("Lỗi: Không nhận được ID phiên đấu giá!");
        }
    }

    public void initData(String auctionId) {
        state.currentAuctionId = auctionId;
        headerAuctionId.setText("ID: " + auctionId);
        network.requestAuctionHistory(auctionId);
        network.requestAuctions();
    }

    @FXML
    private void handleEnterTradingRoom() {
        showcaseView.setVisible(false);
        showcaseView.setManaged(false);

        tradingRoomView.setVisible(true);
        tradingRoomView.setManaged(true);
    }

    // ================= TỰ ĐỘNG ĐỔ ẢNH VÀ MÔ TẢ RA MÀN HÌNH CHÍNH =================
    // ================= TỰ ĐỘNG ĐỔ ẢNH VÀ MÔ TẢ RA MÀN HÌNH CHÍNH =================
    public void renderSothebysItemDetails(JsonObject item) {
        if (item == null) return;

        // 1. Đổ Mô Tả
        showcaseDescLabel.setText(item.has("description") && !item.get("description").isJsonNull()
                ? item.get("description").getAsString() : "Chưa có mô tả chi tiết.");

        // 2. Load Ảnh & Xử lý khi "Không có ảnh"
        boolean hasImage = false;
        showcaseImageView.setImage(null); // Reset ảnh cũ

        // Dùng tà thuật lấy cái khung StackPane bọc bên ngoài bức ảnh
        javafx.scene.layout.StackPane container = (javafx.scene.layout.StackPane) showcaseImageView.getParent();
        // Dọn dẹp mấy cái chữ "Không có ảnh" (nếu bị dính từ phiên xem trước đó)
        container.getChildren().removeIf(node -> node instanceof Label);

        if (item.has("images") && item.get("images").isJsonArray() && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                java.io.File file = new java.io.File(imgPath);
                if (file.exists()) {
                    showcaseImageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                    hasImage = true;
                }
            } catch (Exception e) { System.err.println("Lỗi load ảnh: " + e.getMessage()); }
        }

        // Đóng dấu chữ "Không có ảnh" nếu load thất bại
        if (!hasImage) {
            Label noImgLabel = new Label("Không có hình ảnh");
            noImgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 16px;");
            container.getChildren().add(noImgLabel);
        }

        // 3. Khôi phục toàn bộ thông số chi tiết (Loại, Tình trạng, Thuộc tính riêng)
        showcaseSpecsBox.getChildren().clear();

        // Tiêu đề nhỏ phân khu
        Label specTitle = new Label("THÔNG SỐ CHI TIẾT");
        specTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111111; -fx-padding: 0 0 10 0;");
        showcaseSpecsBox.getChildren().add(specTitle);

        // -- Thêm Loại --
        String type = item.has("type") && !item.get("type").isJsonNull() ? item.get("type").getAsString() : "Khác";
        addSpecRow("Phân loại", type);

        // -- Thêm Tình trạng --
        if (item.has("condition") && !item.get("condition").isJsonNull()) {
            String conditionText = item.get("condition").getAsString();
            if ("NEW".equals(conditionText)) conditionText = "Mới 100%";
            else if ("USED".equals(conditionText)) conditionText = "Đã sử dụng";
            addSpecRow("Tình trạng", conditionText);
        }

        // -- Thêm Thông số chuyên sâu (Tác giả, Hãng xe, Kích thước, Cân nặng...) --
        if (item.has("specifications") && item.get("specifications").isJsonObject()) {
            JsonObject specs = item.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                addSpecRow(key, specs.get(key).getAsString());
            }
        }
    }

    // ================= HÀM PHỤ TRỢ: VẼ TỪNG DÒNG THÔNG SỐ SIÊU ĐẸP =================
    private void addSpecRow(String label, String value) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setSpacing(15);
        // Gạch chân mỏng dưới mỗi thông số
        row.setStyle("-fx-padding: 10 0; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        Label lblKey = new Label(label);
        lblKey.setPrefWidth(150); // Cố định chiều rộng cột tên thông số cho thẳng hàng
        lblKey.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblValue.setWrapText(true);
        javafx.scene.layout.HBox.setHgrow(lblValue, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(lblKey, lblValue);
        showcaseSpecsBox.getChildren().add(row);
    }

    @FXML
    private void handleBid() {
        bidHandler.placeBid(bidAmountField.getText());
    }

    @FXML
    private void handleAutoBid() {
        bidHandler.registerAutoBid(maxBidField.getText().trim(), incrementField.getText().trim());
    }

    @FXML
    private void handleBack() {
        AppNavigator.navigate("Main.fxml");
    }
    // ================= XỬ LÝ THEO DÕI PHIÊN ĐẤU GIÁ =================
    @FXML
    private void handleFollowAuction() {
        if (state.currentAuctionId == null) return;

        // Bắn gói tin Yêu cầu Theo Dõi lên Server
        JsonObject request = new JsonObject();
        request.addProperty("action", "FOLLOW_AUCTION"); // Sếp nhớ code BE đón lệnh này nhé
        request.addProperty("auctionId", state.currentAuctionId);

        // Gửi đi (Dùng ké NetworkClient)
        com.bidding.network.NetworkClient.getInstance().sendJson(request);

        // Đổi giao diện nút thành "Đã Theo Dõi" và khóa lại cho ngầu
        btnFollow.setText("♥ ĐÃ THEO DÕI");
        btnFollow.setStyle("-fx-background-color: #F9FAFB; -fx-text-fill: #111111; -fx-border-color: #D1D5DB; -fx-border-width: 1; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 14;");
        btnFollow.setDisable(true);
    }
}