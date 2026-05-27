package com.bidding.controller;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.bidding.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
public class InventoryController implements Initializable {
    private static final int INVENTORY_GRID_COLUMNS = 2;
    private static final double INVENTORY_GRID_GAP = 12;
    private static final double INVENTORY_CARD_INSET = 8;
    @FXML private javafx.scene.control.CheckBox chkReverseNow, chkReverseSchedule;
    @FXML private javafx.scene.control.TextField txtDropStepNow, txtDropStepSchedule;
    @FXML private GridPane wonItemsGrid;
    @FXML private ScrollPane inventoryScroll;
    @FXML private Button btnSubmitItem;
    @FXML private Button btnCancelEdit;
    @FXML private Button btnRemoveSelectedImage;
    @FXML private TabPane actionTabPane;
    @FXML private Tab itemFormTab;
    // Kho RAM lưu trọn bộ JSON của từng món đồ (để làm Popup)
    private final java.util.Map<String, JsonObject> itemDatabase = new java.util.HashMap<>();
    private final List<JsonObject> inventoryItems = new ArrayList<>();
    private String selectedItemId;
    private String editingItemId;
    @FXML private GridPane inventoryGrid;
    @FXML private TextField txtInventorySearch;
    @FXML private TextField txtItemIdSchedule, txtStartTime, txtEndTime;
    // --- Tab Bán Ngay ---
    @FXML private TextField txtItemIdNow;
    @FXML private TextField txtDuration;
    @FXML private ListView<String> listSelectedImages;
    private List<String> selectedImagePaths = new ArrayList<>();
    // --- Tab Nhập Đồ Mới (Trường dùng chung) ---
    @FXML private TextField txtNewName, txtNewDesc, txtNewPrice, txtBidStep;
    @FXML private ComboBox<String> comboType, comboCondition;

    // --- Container của các trường riêng biệt ---
    @FXML private VBox vboxArt, vboxVehicle, vboxElectronics;

    // Các trường của Art
    @FXML private TextField txtArtArtist, txtArtMedium, txtArtYear, txtArtDimensions;
    // Các trường của Vehicle
    @FXML private TextField txtVehMake, txtVehModel, txtVehYear, txtVehMileage, txtVehFuel;
    // Các trường của Electronics
    @FXML private TextField txtElecBrand, txtElecModel, txtElecWarranty, txtElecPower;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    @FXML
    private void handleSchedule() {
        String itemId = txtItemIdSchedule.getText().trim();
        String startTime = txtStartTime.getText().trim();
        String endTime = txtEndTime.getText().trim();

        if (itemId.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            showAlert(AlertType.WARNING, "Lỗi Nhập Liệu", "Vui lòng nhập đủ các trường cho lịch hẹn!");
            return;
        }

        // Tạo JSON theo đúng source: 47 trong Auction.txt
        JsonObject request = new JsonObject();
        // --- LOGIC ĐẤU GIÁ NGƯỢC ---
        boolean isReverse = chkReverseSchedule != null && chkReverseSchedule.isSelected();
        double dropStep = 0.0;
        if (isReverse) {
            try {
                dropStep = Double.parseDouble(txtDropStepSchedule.getText().replace(",", "").trim());
                if (dropStep <= 0) { showAlert(AlertType.WARNING, "Lỗi", "Bước giảm giá phải > 0!"); return; }
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Lỗi", "Bước giảm giá không hợp lệ!"); return;
            }
        }
        request.addProperty("isReverse", isReverse);
        request.addProperty("dropStep", dropStep);
        // ---------------------------
        request.addProperty("action", "SCHEDULE_AUCTION");
        request.addProperty("itemId", itemId);
        request.addProperty("startTime", startTime);
        request.addProperty("endTime", endTime);

        networkClient.sendJson(request);
    }
    @FXML
    private void handleSelectImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh vật phẩm (Có thể chọn nhiều)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(inventoryGrid.getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                String path = file.getAbsolutePath();
                if (!selectedImagePaths.contains(path)) {
                    selectedImagePaths.add(path);
                    // Hiển thị tên file vào list (Sếp có thể CSS cái list này cho đẹp hơn)
                    listSelectedImages.getItems().add(file.getName());
                }
            }
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        networkClient.setMessageHandler(this::handleServerMessage);

        // Khởi tạo ComboBox
        comboType.getItems().addAll("Art", "Vehicle", "Electronics");
        comboCondition.getItems().addAll("NEW", "USED");

        // --- LOGIC BIẾN HÌNH THEO LOẠI ---
        comboType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Ẩn/Hiện form tương ứng
                vboxArt.setVisible("Art".equals(newVal));
                vboxArt.setManaged("Art".equals(newVal));

                vboxVehicle.setVisible("Vehicle".equals(newVal));
                vboxVehicle.setManaged("Vehicle".equals(newVal));

                vboxElectronics.setVisible("Electronics".equals(newVal));
                vboxElectronics.setManaged("Electronics".equals(newVal));
            }
        });

        // Set giá trị mặc định để nó kích hoạt listener ở trên
        comboType.setValue("Art");
        comboCondition.setValue("NEW");

        txtInventorySearch.textProperty().addListener((obs, oldVal, newVal) -> renderInventoryGrid());
        if (inventoryGrid != null) {
            inventoryGrid.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.doubleValue() > 0 && !inventoryItems.isEmpty()) {
                    renderInventoryGrid();
                }
            });
        }
        forceNumericOnly(txtDuration);
        forceNumericOnly(txtDropStepNow);
        forceNumericOnly(txtDropStepSchedule);
        forceNumericOnly(txtNewPrice);
        forceNumericOnly(txtBidStep);
        forceNumericOnly(txtArtYear);
        forceNumericOnly(txtVehYear);
        forceNumericOnly(txtVehMileage);
        forceNumericOnly(txtElecWarranty);
        forceNumericOnly(txtElecPower);

        handleRefreshInventory();

        // Bắt sự kiện click vào list đồ để điền nhanh ID

        if (chkReverseNow != null) {
            chkReverseNow.setOnAction(e -> {
                txtDropStepNow.setDisable(!chkReverseNow.isSelected());
                if (!chkReverseNow.isSelected()) txtDropStepNow.clear();
            });
        }
        if (chkReverseSchedule != null) {
            chkReverseSchedule.setOnAction(e -> {
                txtDropStepSchedule.setDisable(!chkReverseSchedule.isSelected());
                if (!chkReverseSchedule.isSelected()) txtDropStepSchedule.clear();
            });
        }
    }

    @FXML
    private void handleRefreshInventory() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_ITEMS");
        networkClient.sendJson(request);
    }

    @FXML
    private void handleStartNow() {
        String itemId = txtItemIdNow.getText().trim();
        String durationStr = txtDuration.getText().trim();

        if (itemId.isEmpty() || durationStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đủ ID và Thời gian!");
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            JsonObject request = new JsonObject();
            // --- LOGIC ĐẤU GIÁ NGƯỢC ---
            boolean isReverse = chkReverseNow != null && chkReverseNow.isSelected();
            double dropStep = 0.0;
            if (isReverse) {
                try {
                    dropStep = Double.parseDouble(txtDropStepNow.getText().replace(",", "").trim());
                    if (dropStep <= 0) { showAlert(AlertType.WARNING, "Lỗi", "Bước giảm giá phải > 0!"); return; }
                } catch (NumberFormatException ex) {
                    showAlert(AlertType.ERROR, "Lỗi", "Bước giảm giá không hợp lệ!"); return;
                }
            }
            request.addProperty("isReverse", isReverse);
            request.addProperty("dropStep", dropStep);
            // ---------------------------
            request.addProperty("action", "START_AUCTION");
            request.addProperty("itemId", itemId);
            request.addProperty("durationMinutes", duration);
            networkClient.sendJson(request);
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Lỗi định dạng", "Thời gian phải là số!");
        }
    }

    @FXML
    private void handleAddNewItem() {
        String name = txtNewName.getText().trim();
        String priceStr = txtNewPrice.getText().trim();
        String bidStepStr = txtBidStep.getText().trim();
        String type = comboType.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || bidStepStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Vui lòng điền Tên món đồ, Giá khởi điểm và Bước giá (*).");
            return;
        }

        try {
            // ================= VŨ KHÍ CHỐNG TRÀN BỘ NHỚ =================
            // Chặn ngay từ vòng gửi xe nếu sếp nhập quá 15 chữ số
            // (15 số là 999 nghìn tỷ VNĐ rồi, đủ mua lại cả cty Google)
            if (priceStr.length() > 15 || bidStepStr.length() > 15) {
                showAlert(AlertType.WARNING, "Đại gia quá!", "Giá khởi điểm quá lớn! Hệ thống chỉ hỗ trợ tối đa 999 Nghìn Tỷ VNĐ.");
                return;
            }

            long startingPrice = parseRequiredLong(priceStr, "Giá khởi điểm");
            long bidStep = parseRequiredLong(bidStepStr, "Bước giá");

            if (startingPrice < 0) {
                showAlert(AlertType.WARNING, "Lỗi logic", "Bạn định tặng kèm tiền cho người mua à? Giá không được âm!");
                return;
            }
            if (bidStep <= 0) {
                showAlert(AlertType.WARNING, "Lỗi logic", "Bước giá phải lớn hơn 0.");
                return;
            }
            if (bidStep > startingPrice) {
                showAlert(AlertType.WARNING, "Lỗi logic", "Bước giá không được lớn hơn giá trị sản phẩm.");
                return;
            }
            // ============================================================

            JsonObject request = new JsonObject();

            // 1. Gói các thuộc tính chung
            request.addProperty("action", editingItemId == null ? "ADD_ITEM" : "UPDATE_ITEM");
            if (editingItemId != null) {
                request.addProperty("itemId", editingItemId);
            }
            request.addProperty("name", name);
            request.addProperty("description", txtNewDesc.getText().trim());
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("bidStep", bidStep);
            request.addProperty("type", type);
            request.addProperty("condition", comboCondition.getValue());
            JsonArray imagesArray = new JsonArray();
            for (String path : selectedImagePaths) {
                imagesArray.add(path);
            }
            request.add("images", imagesArray);

            // 2. Gói các thuộc tính riêng dựa theo Loại
            if ("Art".equals(type)) {
                request.addProperty("artist", txtArtArtist.getText().trim());
                request.addProperty("medium", txtArtMedium.getText().trim());
                request.addProperty("yearCreated", parseRequiredInt(txtArtYear.getText().trim(), "Năm sáng tác"));
                request.addProperty("dimensions", txtArtDimensions.getText().trim());
            }
            else if ("Vehicle".equals(type)) {
                request.addProperty("make", txtVehMake.getText().trim());
                request.addProperty("model", txtVehModel.getText().trim());
                request.addProperty("year", parseRequiredInt(txtVehYear.getText().trim(), "Năm sản xuất"));
                request.addProperty("mileage", parseRequiredInt(txtVehMileage.getText().trim(), "Số km đã đi"));
                request.addProperty("fuelType", txtVehFuel.getText().trim());
            }
            else if ("Electronics".equals(type)) {
                request.addProperty("brand", txtElecBrand.getText().trim());
                request.addProperty("model", txtElecModel.getText().trim());
                request.addProperty("warrantyMonths", parseRequiredInt(txtElecWarranty.getText().trim(), "Số tháng bảo hành"));
                request.addProperty("powerWatts", parseRequiredInt(txtElecPower.getText().trim(), "Công suất"));
            }

            networkClient.sendJson(request);

        } catch (NumberFormatException e) {
            String message = e.getMessage() != null && !e.getMessage().isEmpty()
                    ? e.getMessage()
                    : "Vui lòng chỉ nhập số ở các trường yêu cầu số.";
            showAlert(AlertType.ERROR, "Lỗi định dạng", message);
        }
    }

    private long parseRequiredLong(String text, String fieldName) {
        if (text == null || text.isEmpty()) {
            throw new NumberFormatException(fieldName + " không được để trống.");
        }

        if (!text.matches("\\d+")) {
            throw new NumberFormatException(fieldName + " phải là số nguyên không âm.");
        }

        return Long.parseLong(text);
    }

    private int parseRequiredInt(String text, String fieldName) {
        if (text == null || text.isEmpty()) {
            throw new NumberFormatException(fieldName + " không được để trống.");
        }

        if (!text.matches("\\d+")) {
            throw new NumberFormatException(fieldName + " phải là số nguyên không âm.");
        }

        return Integer.parseInt(text);
    }

    private void handleServerMessage(JsonObject response) {
        String action = response.has("action") ? response.get("action").getAsString() : "";

        Platform.runLater(() -> {
            try {
                // 1. Xử lý danh sách vật phẩm trong kho
                // 1. Xử lý danh sách vật phẩm trong kho
                // 1. Xử lý danh sách vật phẩm trong kho (GIAO DIỆN LƯỚI SOTHEBY'S)
                if ("ITEMS_LIST".equals(action)) {
                    inventoryGrid.getChildren().clear(); // Xóa sạch lưới cũ
                    itemDatabase.clear();
                    inventoryItems.clear();
                    selectedItemId = null;
                    if (txtItemIdNow != null) txtItemIdNow.clear();
                    if (txtItemIdSchedule != null) txtItemIdSchedule.clear();
                    exitEditMode();

                    if (response.has("items")) {
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";
                            itemDatabase.put(id, item);
                            inventoryItems.add(item);
                        }
                        renderInventoryGrid();
                    } else {
                        addGridMessage(inventoryGrid, "Kho đồ trống. Hãy nhập thêm vật phẩm!");
                    }
                }

                // 2. Phản hồi thêm vật phẩm mới
                else if ("ADD_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Da gui san pham va dang cho admin duyet.";
                        showAlert(AlertType.INFORMATION, "Thành công", msg);
                        handleRefreshInventory();
                        // Clear form nhập đồ mới
                        clearItemForm();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi thêm đồ";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }

                // 3. Phản hồi Mở phiên đấu giá NGAY
                else if ("UPDATE_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã cập nhật sản phẩm.");
                        clearItemForm();
                        exitEditMode();
                        handleRefreshInventory();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi cập nhật sản phẩm";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }

                else if ("DELETE_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã xóa sản phẩm khỏi kho đồ.");
                        clearItemForm();
                        exitEditMode();
                        handleRefreshInventory();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi xóa sản phẩm";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }

                else if ("START_AUCTION_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã đưa lên sàn đấu giá ngay lập tức!");
                        txtItemIdNow.clear(); txtDuration.clear();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi khởi tạo";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }

                // 4. MỚI: Phản hồi Đặt lịch hẹn giờ đấu giá
                else if ("SCHEDULE_AUCTION_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã đặt lịch hẹn giờ đấu giá thành công!");
                        // Xóa trắng các ô nhập liệu của Tab Hẹn giờ
                        txtItemIdSchedule.clear();
                        txtStartTime.clear();
                        txtEndTime.clear();
                    } else {
                        // Nếu lỗi định dạng yyyy-MM-ddTHH:mm:ss, Server sẽ báo ở đây
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi định dạng thời gian!";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }
                // --- NHẬN DANH SÁCH CHIẾN LỢI PHẨM ---
                // --- NHẬN DANH SÁCH CHIẾN LỢI PHẨM ---
                else if ("WON_ITEMS_LIST".equals(action)) {
                    wonItemsGrid.getChildren().clear(); // Dọn dẹp lưới cũ
                    if (response.has("items")) {
                        int index = 0;
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";

                            // Cất vào kho RAM
                            itemDatabase.put(id, item);

                            // Tái sử dụng hàm vẽ Thẻ Ảnh chuẩn Sotheby's
                            javafx.scene.layout.VBox card = createItemCard(item, false);
                            addCardToGrid(wonItemsGrid, card, index++);
                        }
                    } else {
                        addGridMessage(wonItemsGrid, "Chưa có chiến lợi phẩm nào. Hãy ra sảnh đấu giá thử vận may nhé!");
                    }
                }
                else if ("PAY_AUCTION_REPLY".equals(action)) {
                    showAlert(AlertType.INFORMATION, "Thành công", "Thanh toán thành công. Sản phẩm đã được chuyển vào kho của bạn.");
                    handleRefreshWonItems();
                    handleRefreshInventory();
                }
                else if ("ERROR".equals(action)) {
                    String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi hệ thống không xác định";
                    showAlert(AlertType.ERROR, "Server Từ Chối", msg);
                }
            } catch (Exception e) {
                System.err.println("Lỗi parse JSON Inventory: " + e.getMessage());
            }
        });
    }

    private String approvalStatusLabel(String status) {
        if (status == null || status.isEmpty()) {
            return "Đã duyệt";
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Đang chờ";
            case "REJECTED" -> "Từ chối";
            case "APPROVED" -> "Đã duyệt";
            default -> status;
        };
    }

    private String approvalStatusStyleClass(String status) {
        if (status == null || status.isEmpty()) {
            return "status-approved";
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> "status-pending";
            case "REJECTED" -> "status-rejected";
            case "APPROVED" -> "status-approved";
            default -> "status-pending";
        };
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // ================= ÉP CSS HOÀNG GIA CHO ALERT =================
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Không load được CSS cho Alert!");
        }
        // ==============================================================

        alert.showAndWait();
    }

    @FXML
    private void handleRefreshWonItems() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_WON_ITEMS");
        networkClient.sendJson(request);
    }

    // ================= VŨ KHÍ ÉP NHẬP SỐ =================
    private void handlePayAuction(String auctionId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "PAY_AUCTION");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);
    }

    private void forceNumericOnly(javafx.scene.control.TextField textField) {
        if (textField == null) return;
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Nếu phát hiện có bất kỳ ký tự nào KHÔNG PHẢI LÀ SỐ (\d)
            if (!newValue.matches("\\d*")) {
                // Tự động xóa sạch các ký tự chữ cái/kí tự đặc biệt
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
    @FXML
    private void handleEditSelectedItem() {
        if (selectedItemId == null || selectedItemId.isBlank()) {
            showAlert(AlertType.INFORMATION, "Chưa chọn vật phẩm", "Vui lòng chọn một vật phẩm trong kho trước.");
            return;
        }

        JsonObject item = itemDatabase.get(selectedItemId);
        if (item == null) {
            showAlert(AlertType.ERROR, "Lỗi dữ liệu", "Không tìm thấy dữ liệu vật phẩm đang chọn.");
            return;
        }

        editingItemId = selectedItemId;
        fillItemForm(item);
        if (btnSubmitItem != null) btnSubmitItem.setText("Lưu thay đổi sản phẩm");
        if (btnCancelEdit != null) {
            btnCancelEdit.setVisible(true);
            btnCancelEdit.setManaged(true);
        }
        if (actionTabPane != null && itemFormTab != null) {
            actionTabPane.getSelectionModel().select(itemFormTab);
        }
        comboType.setDisable(true);
    }

    @FXML
    private void handleCancelEdit() {
        clearItemForm();
        exitEditMode();
    }

    @FXML
    private void handleViewDetailsById(String id) {
        handleViewDetailsById(id, true);
    }

    private void handleViewDetailsById(String id, boolean editable) {
        JsonObject item = itemDatabase.get(id);
        if (item == null) return;

        // Chuyển việc vẽ giao diện cho "cỗ máy" ItemDetail.fxml xử lý
        openXinyItemDetail(item, editable);
    }


    // ================= CỖ MÁY CHUYỂN CẢNH SANG SHOWROOM XỊN =================
    // ================= CỖ MÁY MỞ CỬA SỔ SHOWROOM XỊN =================
    // ================= CỖ MÁY MỞ CỬA SỔ SHOWROOM XỊN =================
    private void openXinyItemDetail(JsonObject selectedItemJson, boolean editable) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ItemDetail.fxml"));
            javafx.scene.Parent detailRoot = loader.load();

            com.bidding.controller.ItemDetailController controller = loader.getController();
            controller.setItemData(selectedItemJson);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            controller.setDeleteCallback(itemId -> handleDeleteItemFromDetail(itemId, stage));
            if (editable) {
                controller.setEditCallback(itemId -> {
                    selectedItemId = itemId;
                    stage.close();
                    handleEditSelectedItem();
                });
            } else {
                controller.setEditCallback(null);
            }
            javafx.stage.Window ownerWindow = inventoryGrid.getScene().getWindow();
            javafx.scene.Parent ownerRoot = ownerWindow.getScene().getRoot();
            javafx.scene.effect.Effect previousEffect = ownerRoot.getEffect();
            double previousOpacity = ownerRoot.getOpacity();
            ownerRoot.setEffect(new GaussianBlur(5));
            ownerRoot.setOpacity(0.86);

            // ================= VŨ KHÍ XÓA VIỀN WINDOWS =================
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // <--- THÊM ĐÚNG DÒNG NÀY
            // ===========================================================

            stage.setTitle("Chi tiết vật phẩm");
            stage.setScene(new javafx.scene.Scene(detailRoot));
            stage.initOwner(ownerWindow);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> {
                ownerRoot.setEffect(previousEffect);
                ownerRoot.setOpacity(previousOpacity);
            });
            stage.centerOnScreen();
            stage.show();

            controller.setGoBackCallback(() -> {
                stage.close();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Lỗi Giao Diện", "Không thể tải màn hình chi tiết: " + e.getMessage());
        }
    }
    // =========================================================================
    // =====================================================
    // ================= VŨ KHÍ TẠO LƯỚI ẢNH CHUẨN SOTHEBY'S =================
    private void renderInventoryGrid() {
        if (inventoryGrid == null) return;
        inventoryGrid.getChildren().clear();

        int index = 0;
        for (JsonObject item : inventoryItems) {
            if (matchesInventorySearch(item)) {
                addCardToGrid(inventoryGrid, createItemCard(item, true), index++);
            }
        }

        if (inventoryGrid.getChildren().isEmpty()) {
            addGridMessage(inventoryGrid, "Không có sản phẩm phù hợp.");
        }
    }

    private void addCardToGrid(GridPane grid, javafx.scene.Node node, int index) {
        int col = index % INVENTORY_GRID_COLUMNS;
        int row = index / INVENTORY_GRID_COLUMNS;
        GridPane.setColumnIndex(node, col);
        GridPane.setRowIndex(node, row);
        if (node instanceof Region region) {
            GridPane.setHgrow(region, Priority.ALWAYS);
            region.setMaxWidth(Double.MAX_VALUE);
        }
        grid.getChildren().add(node);
    }

    private void addGridMessage(GridPane grid, String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        GridPane.setColumnSpan(label, INVENTORY_GRID_COLUMNS);
        grid.getChildren().add(label);
    }

    private double getGridCardWidth(GridPane grid) {
        double gridWidth = grid != null ? grid.getWidth() : 0;
        if (gridWidth <= 1 && grid != null && grid.getParent() instanceof ScrollPane scroll) {
            gridWidth = scroll.getViewportBounds().getWidth();
        }
        if (gridWidth <= 1) {
            return 197;
        }
        return Math.max(150, Math.floor((gridWidth - INVENTORY_GRID_GAP) / INVENTORY_GRID_COLUMNS));
    }

    private boolean matchesInventorySearch(JsonObject item) {
        String query = txtInventorySearch != null && txtInventorySearch.getText() != null
                ? normalizeSearchText(txtInventorySearch.getText())
                : "";
        if (query.isEmpty()) {
            return true;
        }

        String searchable = getString(item, "name") + " " +
                getString(item, "id") + " " +
                getString(item, "description") + " " +
                getString(item, "type") + " " +
                (item.has("startingPrice") ? item.get("startingPrice").getAsString() : "");
        return normalizeSearchText(searchable).contains(query);
    }

    private String normalizeSearchText(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        return normalized
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private VBox createItemCard(JsonObject item, boolean editable) {
        String id = item.has("id") ? item.get("id").getAsString() : "";
        String type = item.has("type") ? item.get("type").getAsString() : "";
        String name = item.has("name") ? item.get("name").getAsString() : "Không tên";

        GridPane sizeGrid = editable ? inventoryGrid : wonItemsGrid;
        final double cardWidth = getGridCardWidth(sizeGrid);
        final double imageWidth = Math.max(120, cardWidth - (INVENTORY_CARD_INSET * 2));
        final double imageHeight = 132;
        final double contentWidth = Math.max(120, cardWidth - (INVENTORY_CARD_INSET * 2) - 20);

        VBox card = new VBox(8);
        card.getStyleClass().add("inventory-item-card");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(10);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new javafx.geometry.Insets(INVENTORY_CARD_INSET));

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("inventory-card-image-box");
        imageBox.setPrefSize(imageWidth, imageHeight);
        imageBox.setMinSize(imageWidth, imageHeight);
        imageBox.setMaxSize(imageWidth, imageHeight);
        Rectangle imageClip = new Rectangle(imageWidth, imageHeight);
        imageClip.setArcWidth(16);
        imageClip.setArcHeight(16);
        imageBox.setClip(imageClip);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(imageWidth);
        imgView.setFitHeight(imageHeight);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imgView.setImage(image);
                    applyCoverViewport(imgView, image, imageWidth, imageHeight);
                }
            } catch (Exception e) {}
        }
        imageBox.getChildren().add(imgView);

        Label lblType = new Label(type);
        lblType.getStyleClass().add("inventory-card-badge");
        StackPane.setAlignment(lblType, Pos.TOP_LEFT);
        StackPane.setMargin(lblType, new javafx.geometry.Insets(6, 0, 0, 6));

        String approvalStatus = item.has("approvalStatus") ? item.get("approvalStatus").getAsString() : "APPROVED";
        Label lblApproval = new Label(approvalStatusLabel(approvalStatus));
        lblApproval.getStyleClass().addAll("inventory-status-badge", approvalStatusStyleClass(approvalStatus));
        StackPane.setAlignment(lblApproval, Pos.TOP_RIGHT);
        StackPane.setMargin(lblApproval, new javafx.geometry.Insets(6, 6, 0, 0));

        imageBox.getChildren().addAll(lblType, lblApproval);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("card-name");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER_LEFT);
        lblName.setMaxWidth(contentWidth);

        Button btnDetails = new Button("Xem chi tiết");
        btnDetails.getStyleClass().addAll("primary-button", "inventory-card-detail-btn");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> {
            if (editable) {
                selectItemCard(card, id);
            }
            handleViewDetailsById(id, editable);
        });

        VBox body = new VBox(6, lblName);
        body.getStyleClass().add("inventory-card-body");
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxWidth(contentWidth);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("inventory-card-actions");
        HBox.setHgrow(btnDetails, javafx.scene.layout.Priority.ALWAYS);
        actions.getChildren().add(btnDetails);
        String auctionStatus = item.has("auctionStatus") ? item.get("auctionStatus").getAsString() : "";
        if (!editable && "FINISHED".equals(auctionStatus) && item.has("auctionId")) {
            Button btnPay = new Button("Thanh toán");
            btnPay.getStyleClass().addAll("primary-button", "inventory-card-detail-btn");
            btnPay.setMaxWidth(Double.MAX_VALUE);
            btnPay.setOnAction(e -> handlePayAuction(item.get("auctionId").getAsString()));
            HBox.setHgrow(btnPay, javafx.scene.layout.Priority.ALWAYS);
            actions.getChildren().add(btnPay);
        }

        card.getChildren().addAll(imageBox, body, actions);

        card.setOnMouseClicked(e -> {
            if (editable) {
                selectItemCard(card, id);
            }
        });

        return card;
    }

    private void applyCoverViewport(ImageView imageView, Image image, double targetWidth, double targetHeight) {
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
            image.widthProperty().addListener((observable, oldValue, newValue) ->
                    applyCoverViewport(imageView, image, targetWidth, targetHeight));
            return;
        }

        double targetRatio = targetWidth / targetHeight;
        double imageRatio = imageWidth / imageHeight;

        double viewportWidth = imageWidth;
        double viewportHeight = imageHeight;
        double viewportX = 0;
        double viewportY = 0;

        if (imageRatio > targetRatio) {
            viewportWidth = imageHeight * targetRatio;
            viewportX = (imageWidth - viewportWidth) / 2;
        } else if (imageRatio < targetRatio) {
            viewportHeight = imageWidth / targetRatio;
            viewportY = (imageHeight - viewportHeight) / 2;
        }

        imageView.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
    }

    private void selectItemCard(VBox card, String id) {
        clearCardSelection(inventoryGrid);
        clearCardSelection(wonItemsGrid);
        card.getStyleClass().add("inventory-item-card-selected");

        selectedItemId = id;
        if (txtItemIdNow != null) txtItemIdNow.setText(id);
        if (txtItemIdSchedule != null) txtItemIdSchedule.setText(id);
    }

    private void clearCardSelection(Pane grid) {
        if (grid == null) return;
        for (javafx.scene.Node node : grid.getChildren()) {
            node.getStyleClass().remove("inventory-item-card-selected");
        }
    }

    @FXML
    private void handleRemoveSelectedImage() {
        int selectedIndex = listSelectedImages.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= selectedImagePaths.size()) {
            showAlert(AlertType.INFORMATION, "Chưa chọn ảnh", "Vui lòng chọn một ảnh trong danh sách để xóa.");
            return;
        }

        selectedImagePaths.remove(selectedIndex);
        listSelectedImages.getItems().remove(selectedIndex);
    }

    private void handleDeleteItemFromDetail(String itemId, javafx.stage.Stage detailStage) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Xóa sản phẩm");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn chắc chắn muốn xóa sản phẩm này? Sản phẩm đang hoặc đã đấu giá sẽ bị server từ chối.");
        try {
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        java.util.Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "DELETE_ITEM");
        request.addProperty("itemId", itemId);
        networkClient.sendJson(request);
        detailStage.close();
    }

    private void fillItemForm(JsonObject item) {
        txtNewName.setText(getString(item, "name"));
        txtNewDesc.setText(getString(item, "description"));
        txtNewPrice.setText(item.has("startingPrice") ? String.valueOf(Math.round(item.get("startingPrice").getAsDouble())) : "");
        txtBidStep.setText(item.has("bidStep") ? String.valueOf(Math.round(item.get("bidStep").getAsDouble())) : "");
        comboType.setValue(getString(item, "type").isBlank() ? "Art" : getString(item, "type"));
        comboCondition.setValue(getString(item, "condition").isBlank() ? "NEW" : getString(item, "condition"));

        selectedImagePaths.clear();
        listSelectedImages.getItems().clear();
        if (item.has("images") && item.get("images").isJsonArray()) {
            for (JsonElement elem : item.getAsJsonArray("images")) {
                String path = elem.getAsString();
                selectedImagePaths.add(path);
                listSelectedImages.getItems().add(new File(path).getName());
            }
        }

        clearSpecificFields();
        JsonObject specs = item.has("specifications") ? item.getAsJsonObject("specifications") : new JsonObject();
        String type = comboType.getValue();
        if ("Art".equals(type)) {
            txtArtArtist.setText(getString(specs, "Artist"));
            txtArtMedium.setText(getString(specs, "Medium"));
            txtArtYear.setText(getString(specs, "Year Created"));
            txtArtDimensions.setText(getString(specs, "Dimensions"));
        } else if ("Vehicle".equals(type)) {
            txtVehMake.setText(getString(specs, "Make"));
            txtVehModel.setText(getString(specs, "Model"));
            txtVehYear.setText(getString(specs, "Year"));
            txtVehMileage.setText(getString(specs, "Mileage"));
            txtVehFuel.setText(getString(specs, "Fuel Type"));
        } else if ("Electronics".equals(type)) {
            txtElecBrand.setText(getString(specs, "Brand"));
            txtElecModel.setText(getString(specs, "Model"));
            txtElecWarranty.setText(getString(specs, "Warranty Months"));
            txtElecPower.setText(getString(specs, "Power Watts"));
        }
    }

    private void clearItemForm() {
        txtNewName.clear();
        txtNewPrice.clear();
        txtBidStep.clear();
        txtNewDesc.clear();
        selectedImagePaths.clear();
        listSelectedImages.getItems().clear();
        clearSpecificFields();
    }

    private void clearSpecificFields() {
        txtArtArtist.clear(); txtArtMedium.clear(); txtArtYear.clear(); txtArtDimensions.clear();
        txtVehMake.clear(); txtVehModel.clear(); txtVehYear.clear(); txtVehMileage.clear(); txtVehFuel.clear();
        txtElecBrand.clear(); txtElecModel.clear(); txtElecWarranty.clear(); txtElecPower.clear();
    }

    private void exitEditMode() {
        editingItemId = null;
        if (btnSubmitItem != null) btnSubmitItem.setText("Nhập đồ mới");
        if (btnCancelEdit != null) {
            btnCancelEdit.setVisible(false);
            btnCancelEdit.setManaged(false);
        }
        if (comboType != null) comboType.setDisable(false);
    }

    private String getString(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private String formatPrice(double value) {
        return String.format("%,.0f ₫", value);
    }
}
