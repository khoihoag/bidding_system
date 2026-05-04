package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SceneAuctionManagement – màn hình Quản lý Phiên Đấu Giá (Seller)
 *
 * Tab 1 – Quản lý sản phẩm:
 *   - Hiển thị card sản phẩm, nút "Đấu giá" mở popup chọn:
 *       (A) Đấu giá ngay  → START_AUCTION  {"action":"START_AUCTION","itemId":"...","durationMinutes":N}
 *       (B) Lên lịch       → SCHEDULE_AUCTION {"action":"SCHEDULE_AUCTION","itemId":"...","startTime":"yyyy-MM-ddTHH:mm:ss","endTime":"yyyy-MM-ddTHH:mm:ss"}
 *
 * JSON chính xác theo Auction_(2).txt và Item.txt
 */
public class SceneAuctionManagement implements Initializable {

    // ─── Header ───────────────────────────────────────────────────────────────
    @FXML private Button btnAvatar;
    @FXML private Label  lblClock;
    @FXML private Label  lblBalance;

    // ─── Side menu ────────────────────────────────────────────────────────────
    @FXML private AnchorPane sideMenu;
    @FXML private AnchorPane dimOverlay;

    // ─── Tabs ─────────────────────────────────────────────────────────────────
    @FXML private VBox tabProducts;
    @FXML private VBox tabAuction;
    @FXML private Button btnTabProducts;
    @FXML private Button btnTabAuction;

    // ─── Product list ─────────────────────────────────────────────────────────
    @FXML private FlowPane productContainer;
    @FXML private Label    lblProductCount;

    // ─── Popup đấu giá ────────────────────────────────────────────────────────
    @FXML private AnchorPane popupAuctionAction;
    @FXML private Label      popupTitle;
    @FXML private HBox       popupImageContainer;
    @FXML private Label      popupLblName;
    @FXML private Label      popupLblStartPrice;
    @FXML private Label      popupLblCurrentPrice;
    @FXML private Label      popupLblStatus;
    @FXML private Label      popupLblDesc;
    @FXML private TextField  txtDurationMinutes;
    @FXML private TextField  txtStartTime;
    @FXML private TextField  txtEndTime;
    @FXML private Label      popupResultMsg;

    // ─── Account popup ────────────────────────────────────────────────────────
    @FXML private AnchorPane accountPopup;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean isMenuOpen = false;
    private String  currentFilter = "ALL";
    private final List<ProductShared.ProductData> allProducts = new ArrayList<>();
    private ProductShared.ProductData productBeingActioned = null;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Server expects ISO format: yyyy-MM-ddTHH:mm:ss
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ─── Initialize ───────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sideMenu.setVisible(false);
        sideMenu.setManaged(false);
        sideMenu.setTranslateX(-300);

        // FIX: Bọc toàn bộ xử lý response trong Platform.runLater()
        // vì NetworkClient gọi listener từ thread mạng, không phải JavaFX Thread
        NetworkClient.sellerListener = response ->
                Platform.runLater(() -> handleServerResponse(response));

        startClock();
        requestItems();
    }

    // ─── Clock ────────────────────────────────────────────────────────────────

    private void startClock() {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblClock.setText(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    // ─── Network ──────────────────────────────────────────────────────────────

    /**
     * FIX: Đúng action theo Item.txt — GET_ITEMS
     * Response sẽ là ITEMS_LIST với key "items"
     */
    private void requestItems() {
        NetworkClient.send("{\"action\":\"GET_ITEMS\"}");
    }

    /**
     * Gửi START_AUCTION lên server.
     * FIX: JSON chính xác theo Auction_(2).txt:
     * {"action":"START_AUCTION","itemId":"...","durationMinutes":N}
     */
    private void sendStartAuction(String itemId, int durationMinutes) {
        String json = String.format(
                "{\"action\":\"START_AUCTION\",\"itemId\":\"%s\",\"durationMinutes\":%d}",
                escapeJson(itemId), durationMinutes);
        NetworkClient.send(json);
    }

    /**
     * Gửi SCHEDULE_AUCTION lên server.
     * FIX: JSON chính xác theo Auction_(2).txt:
     * {"action":"SCHEDULE_AUCTION","itemId":"...","startTime":"yyyy-MM-ddTHH:mm:ss","endTime":"yyyy-MM-ddTHH:mm:ss"}
     */
    private void sendScheduleAuction(String itemId, String startTime, String endTime) {
        // Chuyển "yyyy-MM-dd HH:mm:ss" → "yyyy-MM-ddTHH:mm:ss" (ISO) theo yêu cầu server
        String isoStart = toIso(startTime);
        String isoEnd   = toIso(endTime);
        String json = String.format(
                "{\"action\":\"SCHEDULE_AUCTION\",\"itemId\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                escapeJson(itemId), isoStart, isoEnd);
        NetworkClient.send(json);
    }

    // ─── Server Response Handler ───────────────────────────────────────────────

    /**
     * FIX: Toàn bộ hàm này đã được đảm bảo chạy trên JavaFX Thread
     * nhờ Platform.runLater() trong initialize().
     * Handler phân loại action theo đúng Item.txt và Auction_(2).txt.
     */
    private void handleServerResponse(JsonObject response) {
        String action = getString(response, "action");
        switch (action) {
            // FIX: "ITEMS_LIST" với key "items" — đúng theo Item.txt GET_ITEMS response
            case "ITEMS_LIST"              -> replaceProducts(getArray(response, "items"));
            // "AUCTIONS_LIST" với key "data" — đúng theo Auction_(2).txt GET_AUCTIONS response
            case "AUCTIONS_LIST"           -> replaceProducts(firstNonEmpty(
                                                 getArray(response, "data"),
                                                 getArray(response, "auctions"),
                                                 getArray(response, "items")));
            case "START_AUCTION_REPLY"     -> handleStartAuctionReply(response);
            case "SCHEDULE_AUCTION_REPLY"  -> handleScheduleAuctionReply(response);
            case "DELETE_ITEM_REPLY"       -> requestItems();
            case "GLOBAL_NOTIFY"           -> showPopupResult(
                                                 getString(response, "message", "Có thông báo mới."), true);
            case "ERROR"                   -> showPopupResult(
                                                 "Lỗi: " + getString(response, "message", "Không rõ lỗi."), false);
        }
    }

    private void handleStartAuctionReply(JsonObject response) {
        String status = getString(response, "status", "");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            String auctionId = getString(response, "auctionId", "");
            String msg = auctionId.isBlank()
                    ? "Server đã ghi nhận phiên bắt đầu."
                    : "✅ Mở phiên đấu giá thành công! Auction ID: " + auctionId;
            showPopupResult(msg, true);
            if (productBeingActioned != null && !auctionId.isBlank())
                productBeingActioned.auctionId = auctionId;
        } else {
            showPopupResult("❌ " + getString(response, "message", "Không thể mở phiên đấu giá."), false);
        }
    }

    private void handleScheduleAuctionReply(JsonObject response) {
        String status = getString(response, "status", "");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            String auctionId = getString(response, "auctionId", "");
            String msg = auctionId.isBlank()
                    ? "✅ Đã lên lịch đấu giá thành công!"
                    : "✅ Đã lên lịch thành công! Auction ID: " + auctionId;
            showPopupResult(msg, true);
        } else {
            showPopupResult("❌ " + getString(response, "message", "Không thể lên lịch."), false);
        }
    }

    // ─── FXML Handlers: Đấu giá ngay ─────────────────────────────────────────

    @FXML
    void handleStartAuctionNow(ActionEvent event) {
        if (productBeingActioned == null) return;

        String itemId = resolveItemId(productBeingActioned);
        if (itemId.isBlank()) {
            showPopupResult("❌ Sản phẩm chưa có ID thật từ server!", false);
            return;
        }

        String durationStr = txtDurationMinutes.getText().trim();
        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showPopupResult("❌ Vui lòng nhập số phút hợp lệ (lớn hơn 0).", false);
            return;
        }

        showPopupResult("⏳ Đang gửi yêu cầu START_AUCTION lên server...", true);
        sendStartAuction(itemId, duration);
    }

    // ─── FXML Handlers: Lên lịch đấu giá ────────────────────────────────────

    @FXML
    void handleScheduleAuction(ActionEvent event) {
        if (productBeingActioned == null) return;

        String itemId = resolveItemId(productBeingActioned);
        if (itemId.isBlank()) {
            showPopupResult("❌ Sản phẩm chưa có ID thật từ server!", false);
            return;
        }

        String startTime = txtStartTime.getText().trim();
        String endTime   = txtEndTime.getText().trim();

        if (startTime.isBlank() || endTime.isBlank()) {
            showPopupResult("❌ Vui lòng điền đầy đủ thời gian bắt đầu và kết thúc.", false);
            return;
        }

        // Validate format trước khi gửi
        try {
            LocalDateTime.parse(startTime, DT_FORMATTER);
            LocalDateTime.parse(endTime, DT_FORMATTER);
        } catch (Exception e) {
            showPopupResult("❌ Lỗi định dạng thời gian. Vui lòng dùng chuẩn: yyyy-MM-dd HH:mm:ss", false);
            return;
        }

        LocalDateTime dtStart = parseSafe(startTime);
        LocalDateTime dtEnd   = parseSafe(endTime);
        if (!dtEnd.isAfter(dtStart)) {
            showPopupResult("❌ Thời gian kết thúc phải sau thời gian bắt đầu.", false);
            return;
        }

        showPopupResult("⏳ Đang gửi yêu cầu SCHEDULE_AUCTION lên server...", true);
        sendScheduleAuction(itemId, startTime, endTime);
    }

    // ─── Product display ──────────────────────────────────────────────────────

    private void replaceProducts(JsonArray items) {
        List<ProductShared.ProductData> parsed = new ArrayList<>();
        for (JsonElement el : items)
            if (el != null && el.isJsonObject())
                parsed.add(parseProduct(el.getAsJsonObject()));
        allProducts.clear();
        allProducts.addAll(parsed);
        renderProducts();
    }

    private void renderProducts() {
        productContainer.getChildren().clear();
        for (ProductShared.ProductData p : allProducts)
            if (shouldShow(p))
                productContainer.getChildren().add(createProductCard(p));
        updateCountLabel();
    }

    private VBox createProductCard(ProductShared.ProductData data) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white;-fx-padding:0;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.10),14,0,0,3);");
        card.setPrefWidth(310);

        ImageView imgView = new ImageView();
        if (!data.imagePaths.isEmpty()) loadImg(imgView, data.imagePaths.get(0));
        imgView.setFitHeight(200);
        imgView.setFitWidth(310);
        imgView.setPreserveRatio(false);
        imgView.setStyle("-fx-background-radius:12 12 0 0;");

        VBox content = new VBox(8);
        content.setStyle("-fx-padding:14 14 12 14;");

        Label lblName = new Label(data.name);
        lblName.setStyle("-fx-font-weight:bold;-fx-font-size:16px;-fx-text-fill:#2c3e50;-fx-wrap-text:true;");
        lblName.setWrapText(true);
        lblName.setMaxWidth(270);

        Label lblStart = new Label("Giá khởi điểm: " + formatPrice(data.startPrice) + " đ");
        lblStart.setStyle("-fx-font-size:13px;-fx-text-fill:#f39c12;-fx-font-weight:bold;");

        Label lblCurrent = new Label("Giá hiện tại: " + formatPrice(coalesce(data.currentPrice, data.startPrice)) + " đ");
        lblCurrent.setStyle("-fx-font-size:13px;-fx-text-fill:#1d5fa7;-fx-font-weight:bold;");

        String status = calcStatus(data.endTimeStr);
        Label lblStatus = new Label(status);
        String statusColor = "Đã kết thúc".equals(status) ? "#e74c3c" : "#27ae60";
        lblStatus.setStyle("-fx-text-fill:" + statusColor + ";-fx-font-size:12px;-fx-font-weight:bold;");

        HBox btnBox = new HBox(8);
        btnBox.setStyle("-fx-padding:6 0 0 0;");

        Button btnView = new Button("👁 Xem");
        btnView.setStyle("-fx-background-color:#f0f2f5;-fx-text-fill:#2c3e50;-fx-font-size:12px;"
                + "-fx-padding:8 12;-fx-background-radius:7;-fx-cursor:hand;");
        btnView.setOnAction(e -> openAuctionActionPopup(data));

        Button btnAuction = new Button("🔨 Đấu giá");
        btnAuction.setStyle("-fx-background-color:#12263a;-fx-text-fill:white;-fx-font-size:12px;"
                + "-fx-font-weight:bold;-fx-padding:8 14;-fx-background-radius:7;-fx-cursor:hand;");
        btnAuction.setOnAction(e -> openAuctionActionPopup(data));

        btnBox.getChildren().addAll(btnView, btnAuction);
        content.getChildren().addAll(lblName, lblStart, lblCurrent, lblStatus, btnBox);
        card.getChildren().addAll(imgView, content);
        return card;
    }

    // ─── Popup ────────────────────────────────────────────────────────────────

    private void openAuctionActionPopup(ProductShared.ProductData data) {
        productBeingActioned = data;

        popupTitle.setText("Chi tiết: " + data.name);
        popupLblName.setText(data.name);
        popupLblStartPrice.setText(formatPrice(data.startPrice) + " đ");
        popupLblCurrentPrice.setText(formatPrice(coalesce(data.currentPrice, data.startPrice)) + " đ");
        popupLblStatus.setText(calcStatus(data.endTimeStr));
        popupLblDesc.setText(data.description);

        LocalDateTime now = LocalDateTime.now();
        txtStartTime.setText(now.plusMinutes(5).format(DT_FORMATTER));
        txtEndTime.setText(now.plusHours(2).format(DT_FORMATTER));
        txtDurationMinutes.setText("60");

        popupImageContainer.getChildren().clear();
        if (data.imagePaths.isEmpty()) {
            Label noImg = new Label("Không có ảnh");
            noImg.setStyle("-fx-text-fill:#bdc3c7;-fx-font-size:13px;-fx-padding:60 40;");
            popupImageContainer.getChildren().add(noImg);
        } else {
            for (String path : data.imagePaths) {
                ImageView iv = new ImageView();
                loadImg(iv, path);
                iv.setFitHeight(180);
                iv.setFitWidth(220);
                iv.setPreserveRatio(true);
                popupImageContainer.getChildren().add(iv);
            }
        }

        popupResultMsg.setVisible(false);
        popupResultMsg.setText("");

        double w = 580, h = 640;
        AnchorPane root = (AnchorPane) productContainer.getScene().getRoot();
        double cx = root.getWidth() / 2 - w / 2;
        double cy = root.getHeight() / 2 - h / 2 + 32;
        AnchorPane.setLeftAnchor(popupAuctionAction, cx);
        AnchorPane.setTopAnchor(popupAuctionAction, cy);

        dimOverlay.setVisible(true);
        dimOverlay.toFront();
        popupAuctionAction.setVisible(true);
        popupAuctionAction.toFront();
    }

    @FXML
    void closeAuctionActionPopup(ActionEvent event) {
        popupAuctionAction.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingActioned = null;
    }

    @FXML
    void closePopups(javafx.scene.input.MouseEvent event) {
        if (isMenuOpen) { toggleMenu(null); return; }
        accountPopup.setVisible(false);
        popupAuctionAction.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingActioned = null;
    }

    private void showPopupResult(String message, boolean isSuccess) {
        popupResultMsg.setVisible(true);
        popupResultMsg.setText(message);
        popupResultMsg.setStyle(isSuccess
                ? "-fx-text-fill:#15803d;-fx-font-weight:bold;-fx-font-size:13px;"
                : "-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:13px;");
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

    @FXML
    void switchTabProducts(ActionEvent event) {
        tabProducts.setVisible(true);
        tabProducts.setManaged(true);
        tabAuction.setVisible(false);
        tabAuction.setManaged(false);
        btnTabProducts.setStyle("-fx-background-color:#12263a;-fx-text-fill:white;-fx-font-size:13px;"
                + "-fx-font-weight:bold;-fx-padding:14 22;-fx-background-radius:0;-fx-cursor:hand;"
                + "-fx-border-color:transparent transparent #f39c12 transparent;-fx-border-width:0 0 3 0;");
        btnTabAuction.setStyle("-fx-background-color:transparent;-fx-text-fill:#aec6e8;-fx-font-size:13px;"
                + "-fx-padding:14 22;-fx-background-radius:0;-fx-cursor:hand;");
    }

    @FXML
    void switchTabAuction(ActionEvent event) {
        tabProducts.setVisible(false);
        tabProducts.setManaged(false);
        tabAuction.setVisible(true);
        tabAuction.setManaged(true);
        btnTabAuction.setStyle("-fx-background-color:#12263a;-fx-text-fill:white;-fx-font-size:13px;"
                + "-fx-font-weight:bold;-fx-padding:14 22;-fx-background-radius:0;-fx-cursor:hand;"
                + "-fx-border-color:transparent transparent #f39c12 transparent;-fx-border-width:0 0 3 0;");
        btnTabProducts.setStyle("-fx-background-color:transparent;-fx-text-fill:#aec6e8;-fx-font-size:13px;"
                + "-fx-padding:14 22;-fx-background-radius:0;-fx-cursor:hand;");
    }

    // ─── Filters ──────────────────────────────────────────────────────────────

    @FXML void filterAll(ActionEvent e)    { currentFilter = "ALL";    renderProducts(); }
    @FXML void filterActive(ActionEvent e) { currentFilter = "ACTIVE"; renderProducts(); }
    @FXML void filterEnded(ActionEvent e)  { currentFilter = "ENDED";  renderProducts(); }

    @FXML
    void refreshItems(ActionEvent event) {
        requestItems();
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    @FXML
    void toggleMenu(ActionEvent event) {
        if (isMenuOpen) {
            TranslateTransition t = new TranslateTransition(Duration.millis(280), sideMenu);
            t.setToX(-300);
            t.setInterpolator(Interpolator.EASE_OUT);
            t.setOnFinished(e -> {
                sideMenu.setVisible(false);
                sideMenu.setManaged(false);
                dimOverlay.setVisible(false);
            });
            t.play();
            isMenuOpen = false;
        } else {
            sideMenu.setVisible(true);
            sideMenu.setManaged(true);
            sideMenu.setTranslateX(-300);
            dimOverlay.setVisible(true);
            dimOverlay.toFront();
            sideMenu.toFront();
            TranslateTransition t = new TranslateTransition(Duration.millis(280), sideMenu);
            t.setToX(0);
            t.setInterpolator(Interpolator.EASE_OUT);
            t.play();
            isMenuOpen = true;
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    void goToSellerHome(ActionEvent event) {
        try {
            Stage stage = (Stage) productContainer.getScene().getWindow();
            AppNavigator.openPrimary(stage,
                    "/com/bidding/client/scene/SceneSeller1.fxml", "BidViet - Người Bán");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void goToItemManagement(ActionEvent event) {
        if (isMenuOpen) toggleMenu(null);
    }

    @FXML
    void goToAuctionManagement(ActionEvent event) {
        if (isMenuOpen) toggleMenu(null);
    }

    @FXML
    void switchToBidder(ActionEvent event) {
        try {
            Stage stage = (Stage) productContainer.getScene().getWindow();
            AppNavigator.openPrimary(stage,
                    "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Người Mua");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) productContainer.getScene().getWindow();
            AppNavigator.openEntry(stage,
                    "/com/bidding/client/scene/Scene1.fxml", "Đăng nhập hệ thống");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─── Account popup ────────────────────────────────────────────────────────

    @FXML
    void toggleAccountPopup(ActionEvent event) {
        if (accountPopup.isVisible()) {
            accountPopup.setVisible(false);
            dimOverlay.setVisible(false);
        } else {
            if (isMenuOpen) toggleMenu(null);
            AnchorPane.setRightAnchor(accountPopup, 16.0);
            AnchorPane.setTopAnchor(accountPopup, 72.0);
            dimOverlay.setVisible(true);
            dimOverlay.toFront();
            accountPopup.setVisible(true);
            accountPopup.toFront();
        }
    }

    @FXML
    public void handleUploadAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh đại diện");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tập tin ảnh", "*.png", "*.jpg", "*.jpeg"));
        java.io.File f = fc.showOpenDialog(null);
        if (f != null) setAvatarImage(f.toURI().toString());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void updateCountLabel() {
        long showing = productContainer.getChildren().size();
        lblProductCount.setText("(" + showing + " sản phẩm)");
    }

    private boolean shouldShow(ProductShared.ProductData data) {
        boolean ended = "Đã kết thúc".equals(calcStatus(data.endTimeStr));
        return switch (currentFilter) {
            case "ACTIVE" -> !ended;
            case "ENDED"  -> ended;
            default       -> true;
        };
    }

    private String calcStatus(String endTimeStr) {
        try {
            LocalDateTime end = parseSafe(endTimeStr);
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(end)) return "Đã kết thúc";
            long mins = ChronoUnit.MINUTES.between(now, end);
            if (mins > 24 * 60) return "Còn " + (mins / 60 / 24) + " ngày nữa";
            return "Còn " + (mins / 60) + " giờ " + (mins % 60) + " phút";
        } catch (Exception e) { return "Không xác định"; }
    }

    /**
     * FIX CHÍNH: Parse product theo đúng JSON trả về từ GET_ITEMS (Item.txt):
     *   "id"           → String ID của item (VD: "item123")
     *   "name"         → Tên sản phẩm
     *   "startingPrice"→ Giá khởi điểm (long)
     *   "images"       → Mảng ảnh (key là "images", không phải "imagePaths")
     *
     * Khi nhận AUCTIONS_LIST, item nằm trong "item": {...} với key "name",
     * currentPrice nằm ngoài object item.
     */
    private ProductShared.ProductData parseProduct(JsonObject o) {
        // FIX: Server trả "id" dạng String ("item123"), không phải int
        String rawId     = coalesce(getString(o, "id"), getString(o, "itemId"), "0");
        int    numericId = parseIdSafe(rawId);

        // itemIdRef dùng để gửi lên server
        // FIX: Ưu tiên "id" đúng theo server GET_ITEMS, sau đó "itemId"
        String itemIdRef = coalesce(getString(o, "id"), getString(o, "itemId"), rawId);

        String auctionId = coalesce(getString(o, "auctionId"), "");

        // FIX: Server trả "name" (GET_ITEMS), hoặc item.name (AUCTIONS_LIST)
        String name = coalesce(
                getString(o, "name"),
                getString(o, "itemName"),
                getNestedStr(o, "item", "name"),
                "Chưa đặt tên");

        // FIX: Server trả "startingPrice" (đúng key theo Item.txt)
        String startP = String.valueOf(getLong(o, "startingPrice",
                            getLong(o, "startPrice",
                            getLong(o, "price", 0L))));

        // GET_ITEMS không trả currentPrice → mặc định bằng startingPrice
        // AUCTIONS_LIST trả "currentPrice" ở ngoài object item
        String curP = String.valueOf(getLong(o, "currentPrice",
                          getLong(o, "newPrice",
                          Long.parseLong(startP))));

        String desc = coalesce(getString(o, "description"), getString(o, "desc"), "Chưa có mô tả");

        String startT = coalesce(getString(o, "startTime"),
                LocalDateTime.now().minusMinutes(5).format(DT_FORMATTER));
        String endT   = coalesce(getString(o, "endTime"),
                LocalDateTime.now().plusDays(1).format(DT_FORMATTER));

        // FIX: Server trả key "images" (Item.txt), không phải "imagePaths"
        List<String> imgs = new ArrayList<>();
        for (JsonElement el : firstNonEmpty(
                getArray(o, "images"),       // đúng theo Item.txt
                getArray(o, "imagePaths")))  // fallback cho các response khác
            try { imgs.add(el.getAsString()); } catch (Exception ignored) {}

        int bidCount = getInt(o, "bidCount", 0);

        return new ProductShared.ProductData(
                numericId, itemIdRef, auctionId, name,
                startP, curP, desc, startT, endT, imgs, bidCount);
    }

    private String resolveItemId(ProductShared.ProductData p) {
        if (p == null) return "";
        // FIX: itemIdRef đã được set đúng từ parseProduct (ưu tiên "id" của server)
        return coalesce(p.itemIdRef, String.valueOf(p.id));
    }

    private String formatPrice(String raw) {
        try { return String.format("%,d", Long.parseLong(normalizeNum(raw))).replace(',', '.'); }
        catch (Exception e) { return raw; }
    }

    private void setAvatarImage(String path) {
        try {
            Image img = new Image(path);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(42); iv.setFitHeight(42);
            iv.setPreserveRatio(false);
            iv.setClip(new Circle(21, 21, 21));
            btnAvatar.setGraphic(iv);
            btnAvatar.setText("");
        } catch (Exception e) { System.out.println("Lỗi load avatar: " + e.getMessage()); }
    }

    private void loadImg(ImageView iv, String path) {
        if (path == null || path.isBlank()) return;
        try {
            String src = path.startsWith("file:") || path.startsWith("http") ? path : "file:" + path;
            iv.setImage(new Image(src, true));
        } catch (Exception ignored) {}
    }

    /**
     * Chuyển "yyyy-MM-dd HH:mm:ss" → "yyyy-MM-ddTHH:mm:ss" (ISO) để gửi server.
     * Nếu đã là ISO thì giữ nguyên.
     */
    private String toIso(String input) {
        if (input == null) return "";
        if (input.contains("T")) return input;
        return input.replace(" ", "T");
    }

    // ─── JSON utils ───────────────────────────────────────────────────────────

    private String normalizeNum(String v) {
        return v == null || v.isBlank() ? "0" : v.replace(".", "").replace(",", "").trim();
    }

    private String escapeJson(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * FIX: Parse String ID sang int an toàn — server có thể trả "item123" (String)
     */
    private int parseIdSafe(String rawId) {
        if (rawId == null || rawId.isBlank()) return 0;
        try { return Integer.parseInt(rawId); } catch (Exception ignored) {}
        String numOnly = rawId.replaceAll("[^0-9]", "");
        try { return numOnly.isBlank() ? rawId.hashCode() : Integer.parseInt(numOnly); }
        catch (Exception e) { return rawId.hashCode(); }
    }

    private LocalDateTime parseSafe(String v) {
        try {
            if (v != null && v.contains("T"))
                return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return LocalDateTime.parse(v, DT_FORMATTER);
        } catch (Exception e) { return LocalDateTime.now().plusHours(1); }
    }

    private String getString(JsonObject o, String key) { return getString(o, key, ""); }
    private String getString(JsonObject o, String key, String fb) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fb;
        return o.get(key).getAsString();
    }
    private String getNestedStr(JsonObject o, String objKey, String valKey) {
        if (o == null || !o.has(objKey) || !o.get(objKey).isJsonObject()) return "";
        return getString(o.getAsJsonObject(objKey), valKey, "");
    }
    private int getInt(JsonObject o, String key, int fb) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fb;
        try { return o.get(key).getAsInt(); }
        catch (Exception e) {
            try { return Integer.parseInt(o.get(key).getAsString().replaceAll("[^0-9-]", "")); }
            catch (Exception ignored2) { return fb; }
        }
    }
    private long getLong(JsonObject o, String key, long fb) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fb;
        try { return o.get(key).getAsLong(); }
        catch (Exception e) {
            try { return Long.parseLong(o.get(key).getAsString().replace(".", "").replace(",", "").trim()); }
            catch (Exception ignored2) { return fb; }
        }
    }
    private JsonArray getArray(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonArray()) return new JsonArray();
        return o.getAsJsonArray(key);
    }
    @SafeVarargs
    private JsonArray firstNonEmpty(JsonArray... arrays) {
        for (JsonArray a : arrays) if (a != null && !a.isEmpty()) return a;
        return new JsonArray();
    }
    private String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
}