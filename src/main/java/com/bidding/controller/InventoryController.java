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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {
    @FXML private javafx.scene.control.CheckBox chkReverseNow, chkReverseSchedule;
    @FXML private javafx.scene.control.TextField txtDropStepNow, txtDropStepSchedule;
    @FXML private ListView<String> wonItemsListView;

    @FXML private ComboBox<String> comboFilter;
    private final List<String> allItemIds = new ArrayList<>();

    // Kho RAM lưu trọn bộ JSON của từng món đồ (để làm Popup)
    private final java.util.Map<String, JsonObject> itemDatabase = new java.util.HashMap<>();
    @FXML private ListView<String> inventoryListView;
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
        fileChooser.setTitle("Chọn hình ảnh vật phẩm");
        // Chỉ lọc các file ảnh cho đỡ rối
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Cho phép chọn nhiều ảnh cùng lúc
        List<File> files = fileChooser.showOpenMultipleDialog(inventoryListView.getScene().getWindow());

        if (files != null) {
            for (File file : files) {
                String path = file.getAbsolutePath();
                if (!selectedImagePaths.contains(path)) {
                    selectedImagePaths.add(path);
                    listSelectedImages.getItems().add(path);
                }
            }
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        networkClient.setMessageHandler(this::handleServerMessage);

        if (comboFilter != null) {
            comboFilter.getItems().addAll("Tất cả", "Đang chạy", "Đã chạy", "Chưa chạy");
            comboFilter.setValue("Tất cả");
            comboFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }

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

        handleRefreshInventory();

        // Cấu hình giao diện cực xịn cho danh sách vật phẩm
        setupInventoryListView();

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

    private void setupInventoryListView() {
        inventoryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Do chúng ta chỉ lưu ID vào ListView
                txtItemIdNow.setText(newVal);
            }
        });

        inventoryListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (empty || itemId == null) {
                    setText("");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #b2bec3; -fx-font-style: italic;");
                } else {

                    setStyle(null);

                    JsonObject item = itemDatabase.get(itemId);
                    if (item == null) {
                        setText("Phản hồi hệ thống  " + itemId);
                        setGraphic(null);
                        return;
                    }

                    String name = item.has("name") ? item.get("name").getAsString() : "Không tên";
                    String statusStr = item.has("status") ? item.get("status").getAsString() : "PENDING";

                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(15);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    javafx.scene.control.Label uidLabel = new javafx.scene.control.Label(itemId);
                    uidLabel.setStyle("-fx-text-fill: #74b9ff; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
                    uidLabel.setPrefWidth(260);

                    javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(name);
                    nameLabel.setStyle("-fx-text-fill: #dfe6e9; -fx-font-weight: bold;");
                    nameLabel.setPrefWidth(200);

                    javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();
                    statusLabel.setStyle("-fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                    
                    if ("RUNNING".equals(statusStr)) {
                        statusLabel.setText("Running");
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #00b894;"); // Xanh
                    } else if ("FINISHED".equals(statusStr) || "PAID".equals(statusStr)) {
                        statusLabel.setText("Thành công");
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #2ecc71;"); // Xanh lá cây tươi
                    } else if ("PENDING".equals(statusStr) || "OPEN".equals(statusStr)) {
                        statusLabel.setText("Đã lên lịch");
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #f39c12;"); // Cam
                    } else if ("FAILED".equals(statusStr) || "CANCELLED".equals(statusStr)) {
                        statusLabel.setText("Đã chạy");
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #d63031;"); // Đỏ
                    } else {
                        statusLabel.setText("Chưa");
                        statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #636e72;"); // Xám đậm
                    }

                    hbox.getChildren().addAll(uidLabel, nameLabel, statusLabel);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
    }

    private void applyFilter() {
        inventoryListView.getItems().clear();
        String filter = comboFilter.getValue();
        for (String id : allItemIds) {
            JsonObject item = itemDatabase.get(id);
            if (item == null) continue;
            String statusStr = item.has("status") ? item.get("status").getAsString() : "PENDING";
            boolean match = false;
            if ("Tất cả".equals(filter)) {
                match = true;
            } else if ("Đang chạy".equals(filter)) {
                match = "RUNNING".equals(statusStr) || "OPEN".equals(statusStr);
            } else if ("Đã chạy".equals(filter)) {
                match = "FINISHED".equals(statusStr) || "PAID".equals(statusStr) || "FAILED".equals(statusStr) || "CANCELLED".equals(statusStr);
            } else if ("Chưa chạy".equals(filter)) {
                match = "PENDING".equals(statusStr) || "NONE".equals(statusStr);
            }
            if (match) {
                inventoryListView.getItems().add(id);
            }
        }
        if (inventoryListView.getItems().isEmpty()) {
            inventoryListView.getItems().add("Không tìm thấy sản phẩm với trạng thái tương ứng ");
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

            long startingPrice = Long.parseLong(priceStr);

            if (startingPrice < 0) {
                showAlert(AlertType.WARNING, "Lỗi logic", "Sếp định tặng kèm tiền cho người mua à? Giá không được âm!");
                return;
            }
            // ============================================================

            JsonObject request = new JsonObject();

            // 1. Gói các thuộc tính chung
            request.addProperty("action", "ADD_ITEM");
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
                request.addProperty("yearCreated", parseIntSafe(txtArtYear.getText().trim(), 0));
                request.addProperty("dimensions", txtArtDimensions.getText().trim());
            }
            else if ("Vehicle".equals(type)) {
                request.addProperty("make", txtVehMake.getText().trim());
                request.addProperty("model", txtVehModel.getText().trim());
                request.addProperty("year", parseIntSafe(txtVehYear.getText().trim(), 0));
                request.addProperty("mileage", parseIntSafe(txtVehMileage.getText().trim(), 0));
                request.addProperty("fuelType", txtVehFuel.getText().trim());
            }
            else if ("Electronics".equals(type)) {
                request.addProperty("brand", txtElecBrand.getText().trim());
                request.addProperty("model", txtElecModel.getText().trim());
                request.addProperty("warrantyMonths", parseIntSafe(txtElecWarranty.getText().trim(), 0));
                request.addProperty("powerWatts", parseIntSafe(txtElecPower.getText().trim(), 0));
            }

            networkClient.sendJson(request);

        } catch (NumberFormatException e) {
            // Báo lỗi chuẩn xác hơn khi user nhập chữ hoặc ký tự lạ
            showAlert(AlertType.ERROR, "Lỗi định dạng", "Giá khởi điểm có chứa chữ hoặc ký tự lạ! Vui lòng chỉ nhập số.");
        }
    }
    // Hàm an toàn để chuyển String thành Số (nếu user nhập linh tinh thì gán về số mặc định)
    private int parseIntSafe(String text, int defaultValue) {
        if (text == null || text.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void handleServerMessage(JsonObject response) {
        String action = response.has("action") ? response.get("action").getAsString() : "";

        Platform.runLater(() -> {
            try {
                // 1. Xử lý danh sách vật phẩm trong kho
                if ("ITEMS_LIST".equals(action)) {
                    allItemIds.clear();
                    itemDatabase.clear(); // Xóa sạch kho tạm để nạp cái mới

                    if (response.has("items")) {
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";
                            
                            // Cất nguyên cục JSON vào kho RAM
                            itemDatabase.put(id, item);
                            allItemIds.add(id);
                        }
                    }
                    applyFilter();
                }

                // 2. Phản hồi thêm vật phẩm mới
                else if ("ADD_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã nhập đồ vào kho!");
                        handleRefreshInventory();
                        // Clear form nhập đồ mới
                        txtNewName.clear(); txtNewPrice.clear(); txtNewDesc.clear();
                        selectedImagePaths.clear(); listSelectedImages.getItems().clear();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi thêm đồ";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }

                // 3. Phản hồi Mở phiên đấu giá NGAY
                else if ("START_AUCTION_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành Công", "Đã đưa lên sàn đấu giá ngay lập tức!");
                        txtItemIdNow.clear(); txtDuration.clear();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi khởi tạo";
                        showAlert(AlertType.ERROR, "Thất Bại", msg);
                    }
                }

                // 4. MỚI: Phản hồi Đặt lịch hẹn giờ đấu giá
                else if ("SCHEDULE_AUCTION_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành Công", "Đã đặt lịch hẹn giờ đấu giá thành công!");
                        // Xóa trắng các ô nhập liệu của Tab Hẹn giờ
                        txtItemIdSchedule.clear();
                        txtStartTime.clear();
                        txtEndTime.clear();
                    } else {
                        // Nếu lỗi định dạng yyyy-MM-ddTHH:mm:ss, Server sẽ báo ở đây
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi định dạng thời gian!";
                        showAlert(AlertType.ERROR, "Thất Bại", msg);
                    }
                }
                else if ("DELETE_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã xóa vật phẩm!");
                        handleRefreshInventory();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi xóa vật phẩm";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }
                else if ("UPDATE_ITEM_REPLY".equals(action)) {
                    String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
                    if ("SUCCESS".equals(status)) {
                        showAlert(AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin vật phẩm!");
                        handleRefreshInventory();
                    } else {
                        String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi cập nhật vật phẩm";
                        showAlert(AlertType.ERROR, "Thất bại", msg);
                    }
                }
                // --- NHẬN DANH SÁCH CHIẾN LỢI PHẨM ---
                else if ("WON_ITEMS_LIST".equals(action)) {
                    wonItemsListView.getItems().clear();
                    if (response.has("items")) {
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";
                            String name = item.has("name") ? item.get("name").getAsString() : "Không tên";

                            // Cất nguyên cục JSON vào kho RAM để lát vẽ Popup
                            itemDatabase.put(id, item);
                            wonItemsListView.getItems().add(id + " - " + name);
                        }
                    } else {
                        wonItemsListView.getItems().add("Chưa có chiến lợi phẩm nào.");
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

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // ================= ÉP CSS HOÀNG GIA CHO ALERT =================
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            alert.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {
            System.err.println("Không load được CSS cho Alert!");
        }
        // ==============================================================

        alert.showAndWait();
    }
    
    @FXML
    private void handleDeleteItem() {
        String id = inventoryListView.getSelectionModel().getSelectedItem();
        if (id == null || id.equals("Kho đồ trống.")) {
            showAlert(AlertType.WARNING, "Chưa chọn đồ", "Vui lòng chọn một vật phẩm để xóa!");
            return;
        }

        JsonObject item = itemDatabase.get(id);
        if (item != null) {
            String status = item.has("status") ? item.get("status").getAsString() : "";
            if ("RUNNING".equals(status) || "OPEN".equals(status)) {
                showAlert(AlertType.WARNING, "Không thể xóa", "Vật phẩm đang được đấu giá, không thể xóa!");
                return;
            }
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION, "Bạn có chắc chắn muốn xóa vật phẩm này?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "DELETE_ITEM");
            request.addProperty("itemId", id);
            networkClient.sendJson(request);
        }
    }

    @FXML
    private void handleEditItem() {
        String id = inventoryListView.getSelectionModel().getSelectedItem();
        if (id == null || id.equals("Kho đồ trống.")) {
            showAlert(AlertType.WARNING, "Chưa chọn đồ", "Vui lòng chọn một vật phẩm để sửa!");
            return;
        }

        JsonObject item = itemDatabase.get(id);
        if (item != null) {
            String status = item.has("status") ? item.get("status").getAsString() : "";
            if ("RUNNING".equals(status) || "OPEN".equals(status) || "FINISHED".equals(status) || "PAID".equals(status)) {
                showAlert(AlertType.WARNING, "Không thể sửa", "Vật phẩm đang hoặc đã được đấu giá, không thể sửa!");
                return;
            }
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sửa Vật Phẩm");
        dialog.setHeaderText("Cập nhật toàn bộ thông tin vật phẩm");
        
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {}

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        javafx.stage.Window mainWindow = inventoryListView.getScene().getWindow();
        if (mainWindow != null) {
            scrollPane.setPrefViewportHeight(mainWindow.getHeight() * 0.6);
        } else {
            scrollPane.setPrefViewportHeight(400);
        }

        VBox vbox = new VBox(12);
        vbox.setStyle("-fx-padding: 20;");
        vbox.setPrefWidth(450);

        TextField txtName = new TextField(item.has("name") ? item.get("name").getAsString() : "");
        TextField txtDesc = new TextField(item.has("description") ? item.get("description").getAsString() : "");
        TextField txtPrice = new TextField(item.has("startingPrice") ? String.valueOf((long)item.get("startingPrice").getAsDouble()) : "");
        forceNumericOnly(txtPrice);

        ComboBox<String> cmbCondition = new ComboBox<>();
        cmbCondition.getItems().addAll("NEW", "USED");
        cmbCondition.setValue(item.has("condition") ? item.get("condition").getAsString() : "NEW");

        vbox.getChildren().addAll(
            new Label("Tên vật phẩm:"), txtName,
            new Label("Mô tả:"), txtDesc,
            new Label("Giá khởi điểm:"), txtPrice,
            new Label("Tình trạng:"), cmbCondition,
            new Separator()
        );

        String type = item.has("type") ? item.get("type").getAsString() : "";
        JsonObject specs = item.has("specifications") ? item.getAsJsonObject("specifications") : new JsonObject();
        
        // Dynamic Fields
        java.util.Map<String, TextField> dynamicFields = new java.util.HashMap<>();
        
        if ("Art".equals(type)) {
            TextField txtArtist = new TextField(specs.has("Artist") ? specs.get("Artist").getAsString() : "");
            TextField txtMedium = new TextField(specs.has("Medium") ? specs.get("Medium").getAsString() : "");
            TextField txtYear = new TextField(specs.has("Year Created") ? specs.get("Year Created").getAsString() : "");
            TextField txtDim = new TextField(specs.has("Dimensions") ? specs.get("Dimensions").getAsString() : "");
            forceNumericOnly(txtYear);
            dynamicFields.put("artist", txtArtist);
            dynamicFields.put("medium", txtMedium);
            dynamicFields.put("yearCreated", txtYear);
            dynamicFields.put("dimensions", txtDim);
            vbox.getChildren().addAll(
                new Label("Tác giả:"), txtArtist,
                new Label("Chất liệu:"), txtMedium,
                new Label("Năm sáng tác:"), txtYear,
                new Label("Kích thước:"), txtDim
            );
        } else if ("Vehicle".equals(type)) {
            TextField txtMake = new TextField(specs.has("Make") ? specs.get("Make").getAsString() : "");
            TextField txtModel = new TextField(specs.has("Model") ? specs.get("Model").getAsString() : "");
            TextField txtYear = new TextField(specs.has("Year") ? specs.get("Year").getAsString() : "");
            TextField txtMileage = new TextField(specs.has("Mileage") ? specs.get("Mileage").getAsString() : "");
            TextField txtFuel = new TextField(specs.has("Fuel Type") ? specs.get("Fuel Type").getAsString() : "");
            forceNumericOnly(txtYear); forceNumericOnly(txtMileage);
            dynamicFields.put("make", txtMake);
            dynamicFields.put("model", txtModel);
            dynamicFields.put("year", txtYear);
            dynamicFields.put("mileage", txtMileage);
            dynamicFields.put("fuelType", txtFuel);
            vbox.getChildren().addAll(
                new Label("Hãng xe:"), txtMake,
                new Label("Mẫu xe:"), txtModel,
                new Label("Năm sản xuất:"), txtYear,
                new Label("Số KM đã đi:"), txtMileage,
                new Label("Loại nhiên liệu:"), txtFuel
            );
        } else if ("Electronics".equals(type)) {
            TextField txtBrand = new TextField(specs.has("Brand") ? specs.get("Brand").getAsString() : "");
            TextField txtModel = new TextField(specs.has("Model") ? specs.get("Model").getAsString() : "");
            TextField txtWarranty = new TextField(specs.has("Warranty Months") ? specs.get("Warranty Months").getAsString() : "");
            TextField txtPower = new TextField(specs.has("Power Watts") ? specs.get("Power Watts").getAsString() : "");
            forceNumericOnly(txtWarranty); forceNumericOnly(txtPower);
            dynamicFields.put("brand", txtBrand);
            dynamicFields.put("model", txtModel);
            dynamicFields.put("warrantyMonths", txtWarranty);
            dynamicFields.put("powerWatts", txtPower);
            vbox.getChildren().addAll(
                new Label("Thương hiệu:"), txtBrand,
                new Label("Model:"), txtModel,
                new Label("Bảo hành (tháng):"), txtWarranty,
                new Label("Công suất (W):"), txtPower
            );
        }
        
        vbox.getChildren().add(new Separator());
        vbox.getChildren().add(new Label("Hình ảnh hiện tại:"));
        
        ListView<String> imgListView = new ListView<>();
        imgListView.setPrefHeight(100);
        if (item.has("images")) {
            for (JsonElement elem : item.getAsJsonArray("images")) {
                imgListView.getItems().add(elem.getAsString());
            }
        }
        
        javafx.scene.layout.HBox imgButtons = new javafx.scene.layout.HBox(10);
        Button btnAddImg = new Button("Thêm Ảnh");
        Button btnRemImg = new Button("Xóa Ảnh Chọn");
        imgButtons.getChildren().addAll(btnAddImg, btnRemImg);
        
        btnRemImg.setOnAction(e -> {
            String sel = imgListView.getSelectionModel().getSelectedItem();
            if (sel != null) imgListView.getItems().remove(sel);
        });
        
        btnAddImg.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn hình ảnh vật phẩm");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.util.List<File> files = fileChooser.showOpenMultipleDialog(inventoryListView.getScene().getWindow());
            if (files != null) {
                for (File file : files) {
                    String path = file.getAbsolutePath();
                    if (!imgListView.getItems().contains(path)) imgListView.getItems().add(path);
                }
            }
        });
        
        vbox.getChildren().addAll(imgListView, imgButtons);

        scrollPane.setContent(vbox);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty()) {
                    showAlert(AlertType.WARNING, "Lỗi", "Tên và Giá khởi điểm không được để trống!");
                    return null;
                }
                JsonObject request = new JsonObject();
                request.addProperty("action", "UPDATE_ITEM");
                request.addProperty("itemId", id);
                request.addProperty("name", txtName.getText().trim());
                request.addProperty("description", txtDesc.getText().trim());
                request.addProperty("startingPrice", Double.parseDouble(txtPrice.getText().trim()));
                request.addProperty("condition", cmbCondition.getValue());
                
                JsonArray arr = new JsonArray();
                for (String path : imgListView.getItems()) arr.add(path);
                request.add("images", arr);
                
                // Add specific properties
                for (java.util.Map.Entry<String, TextField> entry : dynamicFields.entrySet()) {
                    String val = entry.getValue().getText().trim();
                    if (!val.isEmpty()) {
                        if (entry.getKey().equals("yearCreated") || entry.getKey().equals("year") || entry.getKey().equals("mileage") || entry.getKey().equals("warrantyMonths") || entry.getKey().equals("powerWatts")) {
                            request.addProperty(entry.getKey(), parseIntSafe(val, 0));
                        } else {
                            request.addProperty(entry.getKey(), val);
                        }
                    }
                }
                
                networkClient.sendJson(request);
            }
            return dialogButton;
        });
        dialog.showAndWait();
    }

    @FXML
    private void handleViewDetails() {
        String id = inventoryListView.getSelectionModel().getSelectedItem();
        if (id == null || id.equals("Kho đồ trống.")) {
            showAlert(AlertType.WARNING, "Chưa chọn đồ", "Vui lòng chọn một vật phẩm trong danh sách bên trái!");
            return;
        }

        JsonObject item = itemDatabase.get(id);
        if (item == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi Tiết Vật Phẩm");
        dialog.setHeaderText(item.has("name") ? item.get("name").getAsString() : "Không tên");

        // ÉP CSS HOÀNG GIA CHO DIALOG
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {}
        // 1. TĂNG KÍCH THƯỚC KHUNG CHỨA CHO DỄ NHÌN
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-padding: 20; -fx-font-size: 15px;");
        vbox.setPrefWidth(500); // Ép chiều rộng to ra 500px

        // 2. TẠO SLIDER ẢNH (CAROUSEL)
        com.google.gson.JsonArray imagesArray = item.has("images") ? item.getAsJsonArray("images") : new com.google.gson.JsonArray();
        javafx.scene.layout.StackPane imageSlider = createImageSlider(imagesArray, 460, 300);

        Label lblType = new Label("Loại: " + (item.has("type") ? item.get("type").getAsString() : "---"));
        // ================= THÊM TÌNH TRẠNG VÀO ĐÂY =================
        String conditionText = item.has("condition") ? item.get("condition").getAsString() : "---";
        // Dịch qua tiếng Việt cho thân thiện luôn
        if ("NEW".equals(conditionText)) conditionText = "Mới 100%";
        else if ("USED".equals(conditionText)) conditionText = "Đã sử dụng";

        Label lblCondition = new Label("Tình trạng: " + conditionText);
        lblCondition.setStyle("-fx-font-style: italic; -fx-text-fill: #6b7a90;"); // Xóa màu cam cứng, đổi sang xám sang trọng

        Label lblPrice = new Label("Giá khởi điểm: 0 đ");
        lblPrice.getStyleClass().add("price-value"); // Dùng class mạ vàng của CSS
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        Label lblDesc = new Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imageSlider, lblType, lblCondition, lblPrice, new Separator(), lblDesc);
        // 3. TỰ ĐỘNG QUÉT VÀ VẼ THÔNG SỐ CHI TIẾT (Tác giả, Hãng xe, Kích thước...)
        if (item.has("specifications")) {
            JsonObject specs = item.getAsJsonObject("specifications");
            VBox extraBox = new VBox(8);
            // Xóa màu cứng, gán class info-card để nó ăn bóng đổ xịn xò
            extraBox.getStyleClass().add("info-card");
            extraBox.setStyle("");

            extraBox.getChildren().add(new Label("📋 Thông số chi tiết:"));
            for (String key : specs.keySet()) {
                String value = specs.get(key).getAsString();
                extraBox.getChildren().add(new Label("  • " + key + ": " + value));
            }
            vbox.getChildren().add(extraBox);
        }

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Ép Popup căn giữa màn hình app
        dialog.initOwner(inventoryListView.getScene().getWindow());
        dialog.showAndWait();
    }
    @FXML
    private void handleRefreshWonItems() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_WON_ITEMS");
        networkClient.sendJson(request);
    }
    @FXML
    private void handleViewWonItemDetails() {
        String selected = wonItemsListView.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.contains(" - ")) {
            showAlert(AlertType.WARNING, "Chưa chọn đồ", "Vui lòng chọn một vật phẩm trong danh sách chiến lợi phẩm!");
            return;
        }

        String id = selected.split(" - ")[0];
        JsonObject item = itemDatabase.get(id);
        if (item == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi Tiết Chiến Lợi Phẩm");
        dialog.setHeaderText(item.has("name") ? item.get("name").getAsString() : "Không tên");
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {}
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-padding: 20; -fx-font-size: 15px;");
        vbox.setPrefWidth(500);

        com.google.gson.JsonArray imagesArray = item.has("images") ? item.getAsJsonArray("images") : new com.google.gson.JsonArray();
        javafx.scene.layout.StackPane imageSlider = createImageSlider(imagesArray, 460, 300);

        Label lblType = new Label("Loại: " + (item.has("type") ? item.get("type").getAsString() : "---"));

        Label lblPrice = new Label("Giá khởi điểm: 0 đ");
        lblPrice.getStyleClass().add("price-value"); // Dùng class mạ vàng
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        Label lblDesc = new Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imageSlider, lblType, lblPrice, new Separator(), lblDesc);

        if (item.has("specifications")) {
            JsonObject specs = item.getAsJsonObject("specifications");
            VBox extraBox = new VBox(8);

            // Xóa màu cứng, gán class info-card
            extraBox.getStyleClass().add("info-card");
            extraBox.setStyle("");

            extraBox.getChildren().add(new Label("📋 Thông số chi tiết:"));
            for (String key : specs.keySet()) {
                extraBox.getChildren().add(new Label("  • " + key + ": " + specs.get(key).getAsString()));
            }
            vbox.getChildren().add(extraBox);
        }

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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

    private javafx.scene.layout.StackPane createImageSlider(com.google.gson.JsonArray imagesArray, double width, double height) {
        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
        stack.setPrefSize(width, height);
        stack.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 12; -fx-border-color: rgba(212, 175, 55, 0.2); -fx-border-radius: 12; -fx-border-width: 1;");
        
        if (imagesArray == null || imagesArray.size() == 0) {
            Label lbl = new Label("(Chưa có hình ảnh)");
            lbl.setStyle("-fx-text-fill: #7f8c8d;");
            stack.getChildren().add(lbl);
            return stack;
        }
        
        java.util.List<String> images = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement e : imagesArray) images.add(e.getAsString());
        
        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
        imgView.setFitWidth(width - 40);
        imgView.setFitHeight(height - 20);
        imgView.setPreserveRatio(true);
        
        int[] currentIndex = {0};
        Runnable updateImage = () -> {
            try {
                File file = new File(images.get(currentIndex[0]));
                if (file.exists()) {
                    imgView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                }
            } catch (Exception ex) {}
        };
        updateImage.run();
        stack.getChildren().add(imgView);
        
        if (images.size() > 1) {
            Button btnPrev = new Button("<");
            btnPrev.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 20;");
            Button btnNext = new Button(">");
            btnNext.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 20;");
            
            btnPrev.setOnAction(e -> {
                currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                updateImage.run();
            });
            btnNext.setOnAction(e -> {
                currentIndex[0] = (currentIndex[0] + 1) % images.size();
                updateImage.run();
            });
            
            javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox();
            hbox.setAlignment(javafx.geometry.Pos.CENTER);
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            hbox.getChildren().addAll(btnPrev, spacer, btnNext);
            hbox.setPickOnBounds(false);
            hbox.setPadding(new javafx.geometry.Insets(0, 10, 0, 10));
            stack.getChildren().add(hbox);
        }
        return stack;
    }
}
    // =====================================================
