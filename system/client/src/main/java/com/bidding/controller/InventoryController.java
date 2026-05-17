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

        // Bắt sự kiện click vào list đồ để điền nhanh ID
        inventoryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(" - ")) {
                txtItemIdNow.setText(newVal.split(" - ")[0]);
            }
        });
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
                if ("ITEMS_LIST".equals(action)) {
                    inventoryListView.getItems().clear();
                    itemDatabase.clear(); // Xóa sạch kho tạm để nạp cái mới

                    if (response.has("items")) {
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";
                            String name = item.has("name") ? item.get("name").getAsString() : "Không tên";

                            // Cất nguyên cục JSON vào kho RAM
                            itemDatabase.put(id, item);
                            inventoryListView.getItems().add(id + " - " + name);
                        }
                    } else {
                        inventoryListView.getItems().add("Kho đồ trống.");
                    }
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
    private void handleViewDetails() {
        String selected = inventoryListView.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.contains(" - ")) {
            showAlert(AlertType.WARNING, "Chưa chọn đồ", "Vui lòng chọn một vật phẩm trong danh sách bên trái!");
            return;
        }

        String id = selected.split(" - ")[0];
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

        // 2. TĂNG KÍCH THƯỚC ẢNH
        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
        imgView.setFitWidth(460); // Ảnh bự ra
        imgView.setFitHeight(300);
        imgView.setPreserveRatio(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) {
                    imgView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                }
            } catch (Exception e) { System.err.println("Lỗi load ảnh!"); }
        } else {
            vbox.getChildren().add(new Label("(Chưa có hình ảnh)"));
        }

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
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        Label lblDesc = new Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imgView, lblType, lblCondition, lblPrice, new Separator(), lblDesc);
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

        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
        imgView.setFitWidth(460);
        imgView.setFitHeight(300);
        imgView.setPreserveRatio(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) imgView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            } catch (Exception e) { System.err.println("Lỗi load ảnh!"); }
        } else {
            vbox.getChildren().add(new Label("(Chưa có hình ảnh)"));
        }

        Label lblType = new Label("Loại: " + (item.has("type") ? item.get("type").getAsString() : "---"));

        Label lblPrice = new Label("Giá khởi điểm: 0 đ");
        lblPrice.getStyleClass().add("price-value"); // Dùng class mạ vàng
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        Label lblDesc = new Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imgView, lblType, lblPrice, new Separator(), lblDesc);

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
    // =====================================================
}
