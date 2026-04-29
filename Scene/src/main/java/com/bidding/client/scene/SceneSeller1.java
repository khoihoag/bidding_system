package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

public class SceneSeller1 implements Initializable {

    @FXML private AnchorPane sideMenu;
    @FXML private AnchorPane dimOverlay;
    @FXML private AnchorPane accountPopup;
    @FXML private AnchorPane viewProductPopup;
    @FXML private FlowPane productContainer;

    @FXML private ScrollPane viewImageScroll;
    @FXML private HBox viewImageContainer;
    @FXML private Label viewLblName;
    @FXML private Label viewLblPrice;
    @FXML private Label viewLblEndTime;
    @FXML private Label viewLblDesc;
    @FXML private Label viewLblStatus;
    @FXML private Label viewLblBidCount;
    @FXML private Label viewLblCurrentPrice;
    @FXML private Label viewLblMsg;
    @FXML private Button btnAvatar;

    @FXML private Label lblProductCount;
    @FXML private Label lblSellerNote;

    private boolean isMenuOpen = false;
    private ProductShared.ProductData productBeingViewed = null;
    private String currentFilter = "ALL";
    private final List<ProductShared.ProductData> allProducts = new ArrayList<>();
    private int dummyIdCounter = 1;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── Khởi tạo ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sideMenu.setVisible(false);
        sideMenu.setManaged(false);
        sideMenu.setTranslateX(-300);

        NetworkClient.sellerListener = this::handleSellerResponse;
        lblSellerNote.setText("Đang tải sản phẩm từ server qua GET_ITEMS.");
        requestSellerItems();
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

    @FXML
    void closePopups(MouseEvent event) {
        closePopupsInternal();
    }

    // ─── Mở SceneAddItem (tách riêng) ─────────────────────────────────────────

    /** Mở cửa sổ thêm sản phẩm mới */
    @FXML
    void showAddProductPopup(ActionEvent event) {
        if (isMenuOpen) toggleMenu(null);
        openAddItemWindow(null);
    }

    /** Mở cửa sổ sửa từ popup xem */
    @FXML
    void handleEditFromView(ActionEvent event) {
        if (productBeingViewed == null) return;
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        openAddItemWindow(productBeingViewed);
    }

    /**
     * Mở SceneAddItem trong một Stage riêng (modal).
     * @param data null = chế độ thêm mới, non-null = chỉnh sửa
     */
    private void openAddItemWindow(ProductShared.ProductData data) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/bidding/client/scene/SceneAddItem.fxml"));
            Parent root = loader.load();
            SceneAddItem controller = loader.getController();

            Stage addStage = new Stage();
            addStage.initModality(Modality.APPLICATION_MODAL);
            addStage.initOwner(productContainer.getScene().getWindow());
            addStage.setTitle(data == null ? "Thêm sản phẩm mới" : "Chỉnh sửa sản phẩm");
            addStage.setScene(new Scene(root));
            addStage.setResizable(false);

            if (data == null) {
                controller.initAdd(
                        action -> {
                            addStage.close();
                            requestSellerItems();
                        },
                        addStage::close
                );
            } else {
                controller.initEdit(
                        data,
                        action -> {
                            addStage.close();
                            requestSellerItems();
                        },
                        addStage::close
                );
            }

            addStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Server ───────────────────────────────────────────────────────────────

    private void requestSellerItems() {
        NetworkClient.send("{\"action\":\"GET_ITEMS\"}");
    }

    private void handleSellerResponse(JsonObject response) {
        String action = getString(response, "action");
        switch (action) {
            case "ITEMS_LIST"      -> replaceProducts(getArray(response, "items"));
            case "AUCTIONS_LIST"   -> replaceProducts(firstNonEmptyArray(
                                         getArray(response, "data"),
                                         getArray(response, "auctions"),
                                         getArray(response, "items")));
            case "DELETE_ITEM_REPLY" -> handleDeleteReply(response);
            case "START_AUCTION_REPLY" -> handleStartAuctionReply(response);
            case "GLOBAL_NOTIFY"   -> showInfoPopup(getString(response, "message", "Có thông báo mới."));
            case "ERROR"           -> showInfoPopup("Lỗi: " + getString(response, "message", "Server trả về lỗi."));
            default -> { /* ADD_ITEM_REPLY / UPDATE_ITEM_REPLY xử lý bởi SceneAddItem */ }
        }
    }

    private void handleDeleteReply(JsonObject response) {
        // Tìm product đang pending delete qua trạng thái được lưu trước đó
        // (pendingDelete đã được di chuyển vào hàm confirmAndDelete)
        if (!"SUCCESS".equalsIgnoreCase(getString(response, "status", "SUCCESS"))) {
            showInfoPopup("Xóa sản phẩm thất bại: " + getString(response, "message", ""));
            return;
        }
        // Reload từ server để chắc chắn đồng bộ
        requestSellerItems();
        showInfoPopup("Xóa sản phẩm thành công!");
    }

    private void handleStartAuctionReply(JsonObject response) {
        if ("SUCCESS".equalsIgnoreCase(getString(response, "status", "SUCCESS"))) {
            String auctionId = getString(response, "auctionId", "");
            if (productBeingViewed != null && !auctionId.isBlank())
                productBeingViewed.auctionId = auctionId;
            viewLblMsg.setVisible(true);
            viewLblMsg.setStyle("-fx-text-fill:#15803d;-fx-font-weight:bold;");
            viewLblMsg.setText(auctionId.isBlank()
                    ? "Server đã ghi nhận phiên bắt đầu."
                    : "Server đã mở phiên đấu giá. Auction ID: " + auctionId);
        } else {
            viewLblMsg.setVisible(true);
            viewLblMsg.setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;");
            viewLblMsg.setText(getString(response, "message", "Không đồng bộ được phiên đấu giá."));
        }
    }

    // ─── Hiển thị sản phẩm ────────────────────────────────────────────────────

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
            if (shouldShow(p)) productContainer.getChildren().add(createSellerCard(p));
        updateCountLabel();
    }

    private VBox createSellerCard(ProductShared.ProductData data) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:white;-fx-padding:0;-fx-background-radius:10;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.08),10,0,0,2);");
        card.setPrefWidth(270);

        ImageView imgView = new ImageView();
        if (!data.imagePaths.isEmpty()) loadImg(imgView, data.imagePaths.get(0));
        imgView.setFitHeight(170);
        imgView.setFitWidth(270);
        imgView.setPreserveRatio(false);

        VBox content = new VBox(6);
        content.setStyle("-fx-padding:12 12 10 12;");

        Label lblName = new Label(data.name);
        lblName.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:#2c3e50;-fx-wrap-text:true;");
        lblName.setWrapText(true);
        lblName.setMaxWidth(235);

        Label lblPrice = new Label("Khởi điểm: " + formatPrice(data.startPrice) + " đ");
        lblPrice.setStyle("-fx-font-size:13px;-fx-text-fill:#f39c12;-fx-font-weight:bold;");

        Label lblCurrent = new Label("Giá hiện tại: " + formatPrice(coalesce(data.currentPrice, data.startPrice)) + " đ");
        lblCurrent.setStyle("-fx-font-size:13px;-fx-text-fill:#1d5fa7;-fx-font-weight:bold;");

        String status = calcStatus(data.endTimeStr);
        Label lblStatus = new Label(status);
        lblStatus.setStyle("Đã kết thúc".equals(status)
                ? "-fx-text-fill:#e74c3c;-fx-font-size:13px;-fx-font-weight:bold;"
                : "-fx-text-fill:#27ae60;-fx-font-size:13px;-fx-font-weight:bold;");

        HBox btnBox = new HBox(6);
        btnBox.setStyle("-fx-padding:5 0 0 0;");

        Button btnView = new Button("Xem");
        btnView.setStyle("-fx-background-color:#12263a;-fx-text-fill:white;-fx-font-size:13px;"
                + "-fx-padding:8 14;-fx-background-radius:8;-fx-cursor:hand;");
        btnView.setOnAction(e -> openViewPopup(data));

        Button btnEdit = new Button("Sửa");
        btnEdit.setStyle("-fx-background-color:#1d5fa7;-fx-text-fill:white;-fx-font-size:13px;"
                + "-fx-padding:8 14;-fx-background-radius:8;-fx-cursor:hand;");
        btnEdit.setOnAction(e -> openAddItemWindow(data));

        Button btnDelete = new Button("Xóa");
        btnDelete.setStyle("-fx-background-color:#b93832;-fx-text-fill:white;-fx-font-size:13px;"
                + "-fx-padding:8 12;-fx-background-radius:8;-fx-cursor:hand;");
        btnDelete.setOnAction(e -> confirmAndDelete(data));

        btnBox.getChildren().addAll(btnView, btnEdit, btnDelete);
        content.getChildren().addAll(lblName, lblPrice, lblCurrent, lblStatus, btnBox);
        card.getChildren().addAll(imgView, content);
        return card;
    }

    // ─── View popup ───────────────────────────────────────────────────────────

    private void openViewPopup(ProductShared.ProductData data) {
        productBeingViewed = data;
        viewLblMsg.setVisible(false);
        viewLblName.setText(data.name);
        viewLblPrice.setText(formatPrice(data.startPrice) + " đ");
        viewLblDesc.setText(data.description);
        viewLblEndTime.setText(formatDisplayTime(data.endTimeStr));
        viewLblStatus.setText(calcStatus(data.endTimeStr));
        viewLblBidCount.setText(data.bidCount + " lượt");
        viewLblCurrentPrice.setText(formatPrice(coalesce(data.currentPrice, data.startPrice)) + " đ");

        viewImageContainer.getChildren().clear();
        if (!data.imagePaths.isEmpty()) {
            for (String path : data.imagePaths) {
                ImageView iv = new ImageView();
                loadImg(iv, path);
                iv.setFitHeight(230);
                iv.setFitWidth(270);
                iv.setPreserveRatio(true);
                viewImageContainer.getChildren().add(iv);
            }
        } else {
            Label noImg = new Label("Không có ảnh");
            noImg.setStyle("-fx-text-fill:#bdc3c7;-fx-font-size:14px;-fx-padding:80 60;");
            viewImageContainer.getChildren().add(noImg);
        }

        dimOverlay.setVisible(true);
        dimOverlay.toFront();
        viewProductPopup.setVisible(true);
        viewProductPopup.toFront();
    }

    @FXML
    void handleStartAuctionNow(ActionEvent event) {
        if (productBeingViewed == null) return;
        String itemId = resolveItemId(productBeingViewed);
        if (itemId.isBlank()) {
            viewLblMsg.setVisible(true);
            viewLblMsg.setStyle("-fx-text-fill:#b93832;-fx-font-weight:bold;");
            viewLblMsg.setText("Chưa có itemId thật từ server, không gửi được START_AUCTION.");
            return;
        }
        int durationMinutes = Math.max(1, (int) ChronoUnit.MINUTES.between(
                LocalDateTime.now(), parseSafe(productBeingViewed.endTimeStr)));
        NetworkClient.send(String.format(
                "{\"action\":\"START_AUCTION\",\"itemId\":\"%s\",\"durationMinutes\":%d}",
                escapeJson(itemId), durationMinutes));
        viewLblMsg.setVisible(true);
        viewLblMsg.setStyle("-fx-text-fill:#1d5fa7;-fx-font-weight:bold;");
        viewLblMsg.setText("Đã gửi START_AUCTION lên server.");
    }

    @FXML
    void closeViewPopup(ActionEvent event) {
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingViewed = null;
    }

    @FXML
    void handleDeleteFromView(ActionEvent event) {
        if (productBeingViewed == null) return;
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        confirmAndDelete(productBeingViewed);
    }

    private void confirmAndDelete(ProductShared.ProductData data) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm \"" + data.name + "\"?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                // Xóa lạc quan trong UI trước, server sẽ xác nhận qua DELETE_ITEM_REPLY
                allProducts.removeIf(p -> sameProduct(p, data));
                renderProducts();
                NetworkClient.send(String.format(
                        "{\"action\":\"DELETE_ITEM\",\"itemId\":\"%s\"}",
                        escapeJson(resolveItemId(data))));
            }
        });
    }

    // ─── Filters ──────────────────────────────────────────────────────────────

    @FXML void filterAll(ActionEvent event)    { currentFilter = "ALL";    renderProducts(); }
    @FXML void filterActive(ActionEvent event) { currentFilter = "ACTIVE"; renderProducts(); }
    @FXML void filterEnded(ActionEvent event)  { currentFilter = "ENDED";  renderProducts(); }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    void switchToBidder(ActionEvent event) {
        try {
            Stage stage = (Stage) sideMenu.getScene().getWindow();
            AppNavigator.openPrimary(stage,
                    "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Người Mua");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) accountPopup.getScene().getWindow();
            AppNavigator.openEntry(stage,
                    "/com/bidding/client/scene/Scene1.fxml", "Đăng nhập hệ thống");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleManageAuctions(ActionEvent event) {
        NetworkClient.send("{\"action\":\"GET_AUCTIONS\"}");
    }

    @FXML
    void handleRevenueStats(ActionEvent event) {
        showInfoPopup("Doanh thu hiện tại chưa có action riêng trong JSON doc.");
    }

    // ─── Account popup ────────────────────────────────────────────────────────

    @FXML
    void toggleAccountPopup(ActionEvent event) {
        if (accountPopup.isVisible()) {
            accountPopup.setVisible(false);
            dimOverlay.setVisible(false);
        } else {
            if (isMenuOpen) toggleMenu(null);
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

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private void closePopupsInternal() {
        if (isMenuOpen) { toggleMenu(null); return; }
        accountPopup.setVisible(false);
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingViewed = null;
    }

    private void showInfoPopup(String msg) {
        // Dùng Alert thay vì lblErrorMsg (đã bị cắt sang SceneAddItem)
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Thông báo");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.show();
    }

    private void updateCountLabel() {
        long showing = productContainer.getChildren().size();
        lblProductCount.setText(showing + " sản phẩm đang hiển thị");
        lblSellerNote.setText(showing == 0
                ? "Chưa có dữ liệu từ server."
                : "Danh sách đang hiển thị từ dữ liệu thật của server.");
    }

    private boolean shouldShow(ProductShared.ProductData data) {
        boolean ended = "Đã kết thúc".equals(calcStatus(data.endTimeStr));
        return switch (currentFilter) {
            case "ACTIVE" -> !ended;
            case "ENDED"  -> ended;
            default       -> true;
        };
    }

    private ProductShared.ProductData parseProduct(JsonObject o) {
        int id = getInt(o, "itemId", getInt(o, "id", dummyIdCounter++));
        String itemIdRef = coalesce(getString(o, "itemId"), getString(o, "id"), String.valueOf(id));
        String auctionId = coalesce(getString(o, "auctionId"), "");
        String name      = coalesce(getString(o, "itemName"), getNestedStr(o, "item", "name"), getString(o, "name"), "Chưa đặt tên");
        String startP    = String.valueOf(getLong(o, "startingPrice", getLong(o, "startPrice", getLong(o, "price", 0L))));
        String curP      = String.valueOf(getLong(o, "currentPrice", getLong(o, "newPrice", Long.parseLong(startP))));
        String desc      = coalesce(getString(o, "description"), getString(o, "desc"), "Chưa có mô tả");
        String startT    = coalesce(getString(o, "startTime"), LocalDateTime.now().minusMinutes(5).format(DT_FORMATTER));
        String endT      = coalesce(getString(o, "endTime"), LocalDateTime.now().plusDays(1).format(DT_FORMATTER));
        List<String> imgs = new ArrayList<>();
        for (JsonElement el : firstNonEmptyArray(getArray(o, "imagePaths"), getArray(o, "images")))
            try { imgs.add(el.getAsString()); } catch (Exception ignored) {}
        int bidCount = getInt(o, "bidCount", 0);
        return new ProductShared.ProductData(id, itemIdRef, auctionId, name, startP, curP, desc, startT, endT, imgs, bidCount);
    }

    private boolean sameProduct(ProductShared.ProductData a, ProductShared.ProductData b) {
        if (a == null || b == null) return false;
        if (!resolveItemId(a).isBlank() && !resolveItemId(b).isBlank())
            return resolveItemId(a).equals(resolveItemId(b));
        if (a.auctionId != null && !a.auctionId.isBlank() && b.auctionId != null && !b.auctionId.isBlank())
            return a.auctionId.equals(b.auctionId);
        return a.id == b.id;
    }

    private String resolveItemId(ProductShared.ProductData p) {
        return p == null ? "" : coalesce(p.itemIdRef, String.valueOf(p.id));
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

    private String formatPrice(String raw) {
        try { return String.format("%,d", Long.parseLong(normalizeNum(raw))).replace(',', '.'); }
        catch (Exception e) { return raw; }
    }

    private String formatDisplayTime(String v) {
        try { return parseSafe(v).format(DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy")); }
        catch (Exception e) { return v; }
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

    // ─── JSON utils ───────────────────────────────────────────────────────────

    private String normalizeNum(String v) {
        return v == null || v.isBlank() ? "0" : v.replace(".", "").replace(",", "").trim();
    }

    private String escapeJson(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
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
        catch (Exception e) { try { return Integer.parseInt(o.get(key).getAsString().replaceAll("[^0-9-]","")); } catch (Exception ignored) { return fb; } }
    }
    private long getLong(JsonObject o, String key, long fb) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fb;
        try { return o.get(key).getAsLong(); }
        catch (Exception e) { try { return Long.parseLong(o.get(key).getAsString().replace(".","").replace(",","").trim()); } catch (Exception ignored) { return fb; } }
    }
    private JsonArray getArray(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonArray()) return new JsonArray();
        return o.getAsJsonArray(key);
    }
    private JsonArray firstNonEmptyArray(JsonArray... arrays) {
        for (JsonArray a : arrays) if (a != null && !a.isEmpty()) return a;
        return new JsonArray();
    }
    private String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
}
