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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.layout.FlowPane;
public class InventoryController implements Initializable {
    @FXML private javafx.scene.control.CheckBox chkReverseNow, chkReverseSchedule;
    @FXML private javafx.scene.control.TextField txtDropStepNow, txtDropStepSchedule;
    @FXML private FlowPane wonItemsGrid;
    @FXML private Button btnViewSelectedItem;
    @FXML private Button btnEditSelectedItem;
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
    @FXML private FlowPane inventoryGrid;
    @FXML private TextField txtInventorySearch;
    @FXML private TextField txtItemIdSchedule, txtStartTime, txtEndTime;
    // --- Tab Bán Ngay ---
    @FXML private TextField txtItemIdNow;
    @FXML private TextField txtDuration;
    @FXML private ListView<String> listSelectedImages;
    private List<String> selectedImagePaths = new ArrayList<>();
    // --- Tab Nhập Đồ Mới (Trường dùng chung) ---
    @FXML private TextField txtNewName, txtNewDesc, txtNewPrice;
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
        forceNumericOnly(txtDuration);
        forceNumericOnly(txtDropStepNow);
        forceNumericOnly(txtDropStepSchedule);
        forceNumericOnly(txtNewPrice);
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
        String type = comboType.getValue();

        if (name.isEmpty() || priceStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Vui lòng điền Tên món đồ và Giá khởi điểm (*)");
            return;
        }

        try {
            // ================= VŨ KHÍ CHỐNG TRÀN BỘ NHỚ =================
            // Chặn ngay từ vòng gửi xe nếu sếp nhập quá 15 chữ số
            // (15 số là 999 nghìn tỷ VNĐ rồi, đủ mua lại cả cty Google)
            if (priceStr.length() > 15) {
                showAlert(AlertType.WARNING, "Đại gia quá!", "Giá khởi điểm quá lớn! Hệ thống chỉ hỗ trợ tối đa 999 Nghìn Tỷ VNĐ.");
                return;
            }

            long startingPrice = parseRequiredLong(priceStr, "Giá khởi điểm");

            if (startingPrice < 0) {
                showAlert(AlertType.WARNING, "Lỗi logic", "Bạn định tặng kèm tiền cho người mua à? Giá không được âm!");
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
                        inventoryGrid.getChildren().add(new Label("Kho đồ trống. Hãy nhập thêm vật phẩm!"));
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
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";

                            // Cất vào kho RAM
                            itemDatabase.put(id, item);

                            // Tái sử dụng hàm vẽ Thẻ Ảnh chuẩn Sotheby's
                            javafx.scene.layout.VBox card = createItemCard(item, false);
                            wonItemsGrid.getChildren().add(card);
                        }
                    } else {
                        wonItemsGrid.getChildren().add(new Label("Chưa có chiến lợi phẩm nào. Hãy ra sảnh đấu giá thử vận may nhé!"));
                    }
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
            return "Da duyet";
        }
        return switch (status) {
            case "PENDING" -> "Cho duyet";
            case "REJECTED" -> "Bi tu choi";
            case "APPROVED" -> "Da duyet";
            default -> status;
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
    private void handleViewSelectedItem() {
        if (selectedItemId == null || selectedItemId.isBlank()) {
            showAlert(AlertType.INFORMATION, "Chưa chọn vật phẩm", "Vui lòng chọn một vật phẩm trong kho trước.");
            return;
        }
        handleViewDetailsById(selectedItemId);
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
        JsonObject item = itemDatabase.get(id);
        if (item == null) return;

        // Chuyển việc vẽ giao diện cho "cỗ máy" ItemDetail.fxml xử lý
        openXinyItemDetail(item);
    }


    // ================= CỖ MÁY CHUYỂN CẢNH SANG SHOWROOM XỊN =================
    // ================= CỖ MÁY MỞ CỬA SỔ SHOWROOM XỊN =================
    // ================= CỖ MÁY MỞ CỬA SỔ SHOWROOM XỊN =================
    private void openXinyItemDetail(JsonObject selectedItemJson) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ItemDetail.fxml"));
            javafx.scene.Parent detailRoot = loader.load();

            com.bidding.controller.ItemDetailController controller = loader.getController();
            controller.setItemData(selectedItemJson);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            controller.setDeleteCallback(itemId -> handleDeleteItemFromDetail(itemId, stage));
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

        for (JsonObject item : inventoryItems) {
            if (matchesInventorySearch(item)) {
                inventoryGrid.getChildren().add(createItemCard(item, true));
            }
        }

        if (inventoryGrid.getChildren().isEmpty()) {
            inventoryGrid.getChildren().add(new Label("Không có sản phẩm phù hợp."));
        }
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
        String price = item.has("startingPrice") ? formatPrice(item.get("startingPrice").getAsDouble()) : "";
        String type = item.has("type") ? item.get("type").getAsString() : "";
        String name = item.has("name") ? item.get("name").getAsString() : "Không tên";

        final double cardWidth = 258;
        final double contentWidth = 230;

        VBox card = new VBox(12);
        card.getStyleClass().add("inventory-item-card");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setMinHeight(336);
        card.setPrefHeight(336);
        card.setAlignment(Pos.TOP_CENTER);

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("inventory-card-image-box");
        imageBox.setPrefSize(cardWidth, 164);
        imageBox.setMinSize(cardWidth, 164);
        imageBox.setMaxSize(cardWidth, 164);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(contentWidth);
        imgView.setFitHeight(140);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) imgView.setImage(new Image(file.toURI().toString()));
            } catch (Exception e) {}
        }
        imageBox.getChildren().add(imgView);

        Label lblType = new Label(type);
        lblType.getStyleClass().add("inventory-card-badge");
        StackPane.setAlignment(lblType, Pos.TOP_LEFT);
        StackPane.setMargin(lblType, new javafx.geometry.Insets(0));
        imageBox.getChildren().add(lblType);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("card-name");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER_LEFT);
        lblName.setMaxWidth(contentWidth);

        Label priceCaption = new Label("Giá khởi điểm");
        priceCaption.getStyleClass().add("inventory-card-price-caption");

        Label lblPrice = new Label(price.isBlank() ? "—" : price);
        lblPrice.getStyleClass().add("inventory-card-price-value");
        lblPrice.setMaxWidth(contentWidth);

        VBox priceBlock = new VBox(2, priceCaption, lblPrice);
        priceBlock.setAlignment(Pos.TOP_LEFT);
        priceBlock.setMaxWidth(contentWidth);

        String approvalStatus = item.has("approvalStatus") ? item.get("approvalStatus").getAsString() : "APPROVED";
        Label lblApproval = new Label(approvalStatusLabel(approvalStatus));
        lblApproval.getStyleClass().add("card-id");
        lblApproval.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
        if ("REJECTED".equals(approvalStatus)) {
            lblApproval.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #b91c1c;");
        } else if ("PENDING".equals(approvalStatus)) {
            lblApproval.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #b45309;");
        }

        Button btnDetails = new Button("Xem chi tiết");
        btnDetails.getStyleClass().add("primary-button");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> {
            if (editable) {
                selectItemCard(card, id);
            }
            handleViewDetailsById(id);
        });

        Button btnEdit = new Button("Sửa");
        btnEdit.getStyleClass().add("secondary-button");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setOnAction(e -> {
            selectItemCard(card, id);
            handleEditSelectedItem();
        });

        VBox body = new VBox(6, lblName, priceBlock, lblApproval);
        body.getStyleClass().add("inventory-card-body");
        body.setAlignment(Pos.TOP_LEFT);
        body.setMaxWidth(contentWidth);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("inventory-card-actions");
        HBox.setHgrow(btnDetails, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(btnEdit, javafx.scene.layout.Priority.ALWAYS);
        if (editable) {
            actions.getChildren().addAll(btnDetails, btnEdit);
        } else {
            actions.getChildren().add(btnDetails);
        }

        card.getChildren().addAll(imageBox, body, actions);

        card.setOnMouseClicked(e -> {
            if (editable) {
                selectItemCard(card, id);
            }
        });

        return card;
    }

    private void selectItemCard(VBox card, String id) {
        clearCardSelection(inventoryGrid);
        clearCardSelection(wonItemsGrid);
        card.getStyleClass().add("inventory-item-card-selected");

        selectedItemId = id;
        if (txtItemIdNow != null) txtItemIdNow.setText(id);
        if (txtItemIdSchedule != null) txtItemIdSchedule.setText(id);
    }

    private void clearCardSelection(FlowPane grid) {
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
