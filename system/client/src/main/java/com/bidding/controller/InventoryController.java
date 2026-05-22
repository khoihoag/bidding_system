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
import javafx.scene.layout.FlowPane;
public class InventoryController implements Initializable {
    @FXML private javafx.scene.control.CheckBox chkReverseNow, chkReverseSchedule;
    @FXML private javafx.scene.control.TextField txtDropStepNow, txtDropStepSchedule;
    @FXML private FlowPane wonItemsGrid;
    // Kho RAM lưu trọn bộ JSON của từng món đồ (để làm Popup)
    private final java.util.Map<String, JsonObject> itemDatabase = new java.util.HashMap<>();
    @FXML private FlowPane inventoryGrid;
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
                // 1. Xử lý danh sách vật phẩm trong kho (GIAO DIỆN LƯỚI SOTHEBY'S)
                if ("ITEMS_LIST".equals(action)) {
                    inventoryGrid.getChildren().clear(); // Xóa sạch lưới cũ
                    itemDatabase.clear();

                    if (response.has("items")) {
                        for (JsonElement elem : response.getAsJsonArray("items")) {
                            JsonObject item = elem.getAsJsonObject();
                            String id = item.has("id") ? item.get("id").getAsString() : "UNKNOWN";
                            itemDatabase.put(id, item);

                            // Gọi tà thuật tự vẽ thẻ Hình Ảnh
                            VBox card = createItemCard(item);
                            inventoryGrid.getChildren().add(card);
                        }
                    } else {
                        inventoryGrid.getChildren().add(new Label("Kho đồ trống. Hãy nhập thêm vật phẩm!"));
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
                            javafx.scene.layout.VBox card = createItemCard(item);
                            wonItemsGrid.getChildren().add(card);
                        }
                    } else {
                        wonItemsGrid.getChildren().add(new Label("Chưa có chiến lợi phẩm nào. Hãy ra sảnh khô máu đi sếp!"));
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

            // ================= VŨ KHÍ XÓA VIỀN WINDOWS =================
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // <--- THÊM ĐÚNG DÒNG NÀY
            // ===========================================================

            stage.setTitle("Chi Tiết Vật Phẩm");
            stage.setScene(new javafx.scene.Scene(detailRoot));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
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
    private VBox createItemCard(JsonObject item) {
        String id = item.has("id") ? item.get("id").getAsString() : "";
        String name = item.has("name") ? item.get("name").getAsString() : "Không tên";

        VBox card = new VBox(15);
        card.setPrefWidth(320); // Chiều rộng thẻ
        card.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-padding: 15; -fx-cursor: hand; -fx-alignment: center;");

        // 1. Xử lý Ảnh
        javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
        imgView.setFitWidth(280);
        imgView.setFitHeight(350);
        imgView.setPreserveRatio(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) imgView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            } catch (Exception e) {}
        }

        // 2. Tên tác phẩm và ID
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 16px; -fx-text-fill: #111111;");

        Label lblId = new Label("ID: " + id);
        lblId.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        // 3. Nút Xem chi tiết gọn gàng
        Button btnDetails = new Button("Xem chi tiết");
        btnDetails.getStyleClass().add("secondary-button");
        btnDetails.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        btnDetails.setOnAction(e -> handleViewDetailsById(id)); // Mở popup

        card.getChildren().addAll(imgView, lblName, lblId, btnDetails);

        // 4. BẮT SỰ KIỆN CLICK VÀO ẢNH ĐỂ ĐIỀN ID VÀO FORM MỞ BÁN
        card.setOnMouseClicked(e -> {
            // Tẩy trắng toàn bộ thẻ khác
            for (javafx.scene.Node node : inventoryGrid.getChildren()) {
                node.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-padding: 15; -fx-cursor: hand; -fx-alignment: center;");
            }
            // Tô viền Đen Đậm cho thẻ đang được chọn (Chuẩn Sothebys)
            card.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #111111; -fx-border-width: 2; -fx-padding: 15; -fx-cursor: hand; -fx-alignment: center;");

            // Tự động bắn ID sang 2 ô txtItemId
            if (txtItemIdNow != null) txtItemIdNow.setText(id);
            if (txtItemIdSchedule != null) txtItemIdSchedule.setText(id);
        });

        return card;
    }
}
