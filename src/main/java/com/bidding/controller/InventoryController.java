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
            long startingPrice = Long.parseLong(priceStr);
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
            showAlert(AlertType.ERROR, "Lỗi định dạng", "Giá khởi điểm phải là số nguyên!");
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
            } catch (Exception e) {
                System.err.println("Lỗi parse JSON Inventory: " + e.getMessage());
            }
        });
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
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
        lblCondition.setStyle("-fx-font-style: italic; -fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Cho màu cam nổi bật
        // ===========================================================
        Label lblPrice = new Label("Giá khởi điểm: 0 đ");
        lblPrice.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
        if (item.has("startingPrice")) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN"));
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
            // Đóng khung màu xanh lá cây nhạt cho đẹp
            extraBox.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-background-color: #f1f8e9; -fx-padding: 10; -fx-background-radius: 5;");

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
}