package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho màn hình Thêm / Sửa sản phẩm (SceneAddItem).
 *
 * ┌──────────────────── PAYLOAD ADD_ITEM (theo json.docx) ────────────────────┐
 * │ Chung (Item cha):  action, name, description, startingPrice, type,        │
 * │                    condition, images                                       │
 * ├── Art:             artist, medium, yearCreated, dimensions                 │
 * ├── Electronics:     brand, model, warrantyMonths, powerWatts               │
 * └── Vehicle:         make, model, year, mileage, fuelType                  │
 *
 * UPDATE_ITEM:  action, itemId, name, description
 * SCHEDULE_AUCTION (tự gửi sau ADD_ITEM): action, itemId, startTime, endTime
 *   → format thời gian BẮT BUỘC có 'T' giữa (ISO_LOCAL_DATE_TIME)
 */
public class SceneAddItem implements Initializable {

    // ── Tiêu đề ──────────────────────────────────────────────────────────────
    @FXML private Label  lblPageTitle;
    @FXML private Label  lblSubtitle;

    // ── Thông tin chung (Item cha) ────────────────────────────────────────────
    @FXML private TextField        txtName;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtStartingPrice;
    @FXML private ComboBox<String> cbType;       // "Art" | "Electronics" | "Vehicle"
    @FXML private ComboBox<String> cbCondition;  // ItemCondition enum

    // ── Art ──────────────────────────────────────────────────────────────────
    @FXML private VBox      panelArt;
    @FXML private TextField txtArtist;
    @FXML private TextField txtMedium;
    @FXML private TextField txtYearCreated;
    @FXML private TextField txtDimensions;

    // ── Electronics ───────────────────────────────────────────────────────────
    @FXML private VBox      panelElectronics;
    @FXML private TextField txtBrand;
    @FXML private TextField txtElecModel;
    @FXML private TextField txtWarrantyMonths;
    @FXML private TextField txtPowerWatts;

    // ── Vehicle ───────────────────────────────────────────────────────────────
    @FXML private VBox      panelVehicle;

    @FXML private TextField txtMake;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtYear;
    @FXML private TextField txtMileage;
    @FXML private TextField txtFuelType;

    // ── Thời gian kết thúc ────────────────────────────────────────────────────
    @FXML private DatePicker       datePicker;
    @FXML private ComboBox<String> cbGio;
    @FXML private ComboBox<String> cbPhut;

    // ── Upload ảnh ────────────────────────────────────────────────────────────
    @FXML private HBox  imageContainer;
    @FXML private Label lblImageCount;

    // ── Trạng thái / nút ─────────────────────────────────────────────────────
    @FXML private Label  lblErrorMsg;
    @FXML private Button btnSubmit;

    // ── State ─────────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** null = thêm mới, non-null = chỉnh sửa */
    private ProductShared.ProductData editingProduct = null;

    private final List<String> uploadedImagePaths = new ArrayList<>();

    /** Lưu tạm start/endTime để gửi SCHEDULE_AUCTION sau ADD_ITEM_REPLY */
    private String pendingStartIso = "";
    private String pendingEndIso   = "";

    private Consumer<String> onSaveSuccess;
    private Runnable         onCancel;

    // ─────────────────────────────────────────────────────────────────────────
    // Khởi tạo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int i = 0; i < 24; i++) cbGio.getItems().add(String.format("%02d", i));
        for (int i = 0; i < 60; i++) cbPhut.getItems().add(String.format("%02d", i));
        cbGio.setValue("12");
        cbPhut.setValue("00");

        // Khớp chính xác với 3 class item hiện có ở server.
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.setValue("Art");
        cbType.setOnAction(e -> switchPanel());
        switchPanel();

        // Khớp với ItemCondition enum bên server
        cbCondition.getItems().addAll("NEW", "USED", "LIKE_NEW", "DAMAGED");
        cbCondition.setValue("NEW");

        // Đăng ký listener – chỉ SceneAddItem lắng nghe ADD/UPDATE reply
        NetworkClient.addItemListener = this::onServerResponse;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – gọi từ SceneSeller1
    // ─────────────────────────────────────────────────────────────────────────

    /** Cấu hình chế độ THÊM MỚI */
    public void initAdd(Consumer<String> successCb, Runnable cancelCb) {
        editingProduct = null;
        onSaveSuccess  = successCb;
        onCancel       = cancelCb;
        lblPageTitle.setText("Thêm sản phẩm mới");
        lblSubtitle.setText("Chọn đúng loại sản phẩm – server hiện chỉ nhận Art, Electronics hoặc Vehicle.");
        btnSubmit.setText("Đăng sản phẩm");
        clearForm();
    }

    /** Cấu hình chế độ CHỈNH SỬA */
    public void initEdit(ProductShared.ProductData data,
                         Consumer<String> successCb,
                         Runnable cancelCb) {
        editingProduct = data;
        onSaveSuccess  = successCb;
        onCancel       = cancelCb;
        lblPageTitle.setText("Chỉnh sửa sản phẩm");
        lblSubtitle.setText("Chỉ sửa được Tên và Mô tả – không đổi được loại khi đã tạo.");
        btnSubmit.setText("Lưu thay đổi");
        fillForm(data);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML handlers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleTypeChange(ActionEvent event) {
        switchPanel();
    }

    @FXML
    void handleUploadImages(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm (tối đa 5, mỗi ảnh < 5 MB)");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) imageContainer.getScene().getWindow();
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        for (File f : files) {
            if (uploadedImagePaths.size() >= 5) { showMsg("Tối đa 5 ảnh – một số bị bỏ qua.", false); break; }
            if (f.length() > 5 * 1024 * 1024L)  { showMsg("Bỏ qua " + f.getName() + " (vượt 5 MB).", false); continue; }
            uploadedImagePaths.add(f.getAbsolutePath());
            addImgPreview(f.getAbsolutePath());
        }
        refreshImgLabel();
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        lblErrorMsg.setVisible(false);
        if (editingProduct != null) {
            submitUpdate();
        } else {
            submitAdd();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        if (onCancel != null) onCancel.run();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Submit logic
    // ─────────────────────────────────────────────────────────────────────────

    /** Gửi UPDATE_ITEM: action, itemId, name, description */
    private void submitUpdate() {
        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();
        if (!validateNameDesc(name, desc)) return;

        String json = String.format(
                "{\"action\":\"UPDATE_ITEM\",\"itemId\":\"%s\",\"name\":\"%s\",\"description\":\"%s\"}",
                esc(resolveItemId(editingProduct)), esc(name), esc(desc));
        NetworkClient.send(json);
        showMsg("Đang gửi UPDATE_ITEM lên server…", true);
    }

    /**
     * Xây dựng và gửi ADD_ITEM.
     * Payload: trường chung + images + trường đặc thù theo type (Art/Electronics/Vehicle).
     */
    private void submitAdd() {
        // ── Validate & đọc trường chung ──────────────────────────────────────
        String name  = txtName.getText().trim();
        String desc  = txtDescription.getText().trim();
        String price = txtStartingPrice.getText().trim();
        String type  = cbType.getValue();
        String cond  = cbCondition.getValue();

        if (!validateNameDesc(name, desc)) return;
        if (price.isEmpty() || !price.matches("\\d+(\\.\\d+)?") || Double.parseDouble(price) <= 0) {
            showMsg("Giá khởi điểm phải là số dương!", false); return;
        }
        if (uploadedImagePaths.isEmpty()) {
            showMsg("Bắt buộc phải có ít nhất 1 hình ảnh!", false); return;
        }
        if (datePicker.getValue() == null) {
            showMsg("Vui lòng chọn ngày kết thúc!", false); return;
        }
        LocalDateTime endDt = LocalDateTime.of(
                datePicker.getValue(),
                LocalTime.of(Integer.parseInt(cbGio.getValue()),
                             Integer.parseInt(cbPhut.getValue())));
        if (!endDt.isAfter(LocalDateTime.now())) {
            showMsg("Thời gian kết thúc phải sau thời điểm hiện tại.", false); return;
        }

        // Lưu để dùng cho SCHEDULE_AUCTION sau khi ADD thành công
        pendingStartIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        pendingEndIso   = endDt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // ── Build JSON payload ────────────────────────────────────────────────
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendStr(sb, "action", "ADD_ITEM");           sb.append(",");
        appendStr(sb, "name", name);                   sb.append(",");
        appendStr(sb, "description", desc);            sb.append(",");
        sb.append("\"startingPrice\":").append(price); sb.append(",");
        appendStr(sb, "type", type);                   sb.append(",");
        appendStr(sb, "condition", cond);              sb.append(",");
        appendStringArray(sb, "images", uploadedImagePaths);

        // ── Trường đặc thù theo từng class con ───────────────────────────────
        switch (type) {
            case "Art" -> {
                // Art: artist, medium, yearCreated, dimensions
                String artist  = txtArtist.getText().trim();
                String medium  = txtMedium.getText().trim();
                String yearStr = txtYearCreated.getText().trim();
                String dims    = txtDimensions.getText().trim();

                if (artist.isEmpty()) { showMsg("Vui lòng nhập Nghệ sĩ / Tác giả.", false); return; }
                if (medium.isEmpty()) { showMsg("Vui lòng nhập Chất liệu.", false); return; }
                int yearCreated;
                try { yearCreated = Integer.parseInt(yearStr); }
                catch (Exception e) { showMsg("Năm sáng tác phải là số nguyên.", false); return; }

                sb.append(","); appendStr(sb, "artist", artist);
                sb.append(","); appendStr(sb, "medium", medium);
                sb.append(",\"yearCreated\":").append(yearCreated);
                sb.append(","); appendStr(sb, "dimensions", dims);
            }
            case "Electronics" -> {
                // Electronics: brand, model, warrantyMonths, powerWatts
                String brand    = txtBrand.getText().trim();
                String elecMdl  = txtElecModel.getText().trim();
                String warnStr  = txtWarrantyMonths.getText().trim();
                String pwrStr   = txtPowerWatts.getText().trim();

                if (brand.isEmpty())   { showMsg("Vui lòng nhập Hãng sản xuất.", false); return; }
                if (elecMdl.isEmpty()) { showMsg("Vui lòng nhập Model.", false); return; }
                int warranty, power = 0;
                try { warranty = Integer.parseInt(warnStr); }
                catch (Exception e) { showMsg("Bảo hành (tháng) phải là số nguyên.", false); return; }
                try { if (!pwrStr.isEmpty()) power = Integer.parseInt(pwrStr); }
                catch (Exception e) { showMsg("Công suất (Watts) phải là số nguyên.", false); return; }

                sb.append(","); appendStr(sb, "brand", brand);
                sb.append(","); appendStr(sb, "model", elecMdl);
                sb.append(",\"warrantyMonths\":").append(warranty);
                sb.append(",\"powerWatts\":").append(power);
            }
            case "Vehicle" -> {
                // Vehicle: make, model, year, mileage, fuelType
                String make    = txtMake.getText().trim();
                String vehMdl  = txtVehModel.getText().trim();
                String yearStr = txtYear.getText().trim();
                String mileStr = txtMileage.getText().trim();
                String fuel    = txtFuelType.getText().trim();

                if (make.isEmpty())   { showMsg("Vui lòng nhập Hãng xe.", false); return; }
                if (vehMdl.isEmpty()) { showMsg("Vui lòng nhập Tên / Model xe.", false); return; }
                if (fuel.isEmpty())   { showMsg("Vui lòng nhập Loại nhiên liệu.", false); return; }
                int year, mileage = 0;
                try { year = Integer.parseInt(yearStr); }
                catch (Exception e) { showMsg("Năm sản xuất phải là số nguyên.", false); return; }
                try { if (!mileStr.isEmpty()) mileage = Integer.parseInt(mileStr); }
                catch (Exception e) { showMsg("Số km đã chạy phải là số nguyên.", false); return; }

                sb.append(","); appendStr(sb, "make", make);
                sb.append(","); appendStr(sb, "model", vehMdl);
                sb.append(",\"year\":").append(year);
                sb.append(",\"mileage\":").append(mileage);
                sb.append(","); appendStr(sb, "fuelType", fuel);
            }
            default -> {
                showMsg("Loại sản phẩm không hợp lệ theo server hiện tại.", false);
                return;
            }
        }

        sb.append("}");
        NetworkClient.send(sb.toString());
        showMsg("Đang gửi ADD_ITEM lên server…", true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xử lý phản hồi server
    // ─────────────────────────────────────────────────────────────────────────

    private void onServerResponse(JsonObject resp) {
        String action = safeStr(resp, "action");
        String status = safeStr(resp, "status", "SUCCESS");

        switch (action) {
            case "ADD_ITEM_REPLY" -> {
                if (!"SUCCESS".equalsIgnoreCase(status)) {
                    showMsg("Đăng sản phẩm thất bại: " + safeStr(resp, "message"), false);
                    return;
                }
                showMsg("✅  Đăng sản phẩm thành công!", true);
                autoScheduleAuction(resp);   // Tự gửi SCHEDULE_AUCTION nếu có itemId
                closeAfterDelay("ADD_ITEM");
            }
            case "UPDATE_ITEM_REPLY" -> {
                if (!"SUCCESS".equalsIgnoreCase(status)) {
                    showMsg("Cập nhật thất bại: " + safeStr(resp, "message"), false);
                    return;
                }
                showMsg("✅  Cập nhật thành công!", true);
                closeAfterDelay("UPDATE_ITEM");
            }
            case "SCHEDULE_AUCTION_REPLY" ->
                showMsg("✅  Đã lên lịch đấu giá thành công!", true);
            case "ERROR" ->
                showMsg("❌  Lỗi server: " + safeStr(resp, "message"), false);
        }
    }

    /**
     * Sau ADD_ITEM_REPLY thành công, lấy itemId từ reply rồi gửi SCHEDULE_AUCTION.
     * Format thời gian BẮT BUỘC phải có 'T' (ISO) theo doc.
     */
    private void autoScheduleAuction(JsonObject reply) {
        String itemId = extractItemId(reply);
        if (itemId.isBlank()) {
            showMsg("ADD_ITEM_REPLY không trả itemId nên client chưa thể tự gửi SCHEDULE_AUCTION. Đây là giới hạn từ doc/reply hiện tại.", false);
            return;
        }
        if (pendingEndIso.isBlank()) return;

        String json = String.format(
                "{\"action\":\"SCHEDULE_AUCTION\",\"itemId\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                esc(itemId), pendingStartIso, pendingEndIso);
        NetworkClient.send(json);
    }

    /** Tìm itemId trong các vị trí khác nhau server có thể trả về */
    private String extractItemId(JsonObject reply) {
        if (reply.has("itemId") && !reply.get("itemId").isJsonNull())
            return reply.get("itemId").getAsString();
        if (reply.has("data") && reply.get("data").isJsonObject()) {
            JsonObject data = reply.getAsJsonObject("data");
            if (data.has("itemId") && !data.get("itemId").isJsonNull())
                return data.get("itemId").getAsString();
        }
        if (reply.has("item") && reply.get("item").isJsonObject()) {
            JsonObject item = reply.getAsJsonObject("item");
            if (item.has("itemId") && !item.get("itemId").isJsonNull())
                return item.get("itemId").getAsString();
        }
        return "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Ẩn/hiện panel theo loại được chọn */
    private void switchPanel() {
        String t = cbType.getValue();
        set(panelArt,          "Art".equals(t));
        set(panelElectronics,  "Electronics".equals(t));
        set(panelVehicle,      "Vehicle".equals(t));
    }

    private void set(VBox panel, boolean show) {
        panel.setVisible(show);
        panel.setManaged(show);
    }

    private boolean validateNameDesc(String name, String desc) {
        if (name.length() < 2 || name.length() > 200) {
            showMsg("Tên sản phẩm phải từ 2 đến 200 ký tự!", false); return false;
        }
        if (desc.length() < 10) {
            showMsg("Mô tả quá ngắn – cần ít nhất 10 ký tự.", false); return false;
        }
        return true;
    }

    private void fillForm(ProductShared.ProductData d) {
        txtName.setText(d.name);
        txtDescription.setText(d.description);
        txtStartingPrice.setText(d.startPrice);
        // Khi sửa không cho đổi type/condition (đã tạo rồi)
        cbType.setDisable(true);
        cbCondition.setDisable(true);

        try {
            LocalDateTime end = parseSafe(d.endTimeStr);
            datePicker.setValue(end.toLocalDate());
            cbGio.setValue(String.format("%02d", end.getHour()));
            cbPhut.setValue(String.format("%02d", end.getMinute()));
        } catch (Exception ignored) {
            datePicker.setValue(LocalDate.now().plusDays(1));
        }

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        if (d.imagePaths != null) {
            uploadedImagePaths.addAll(d.imagePaths);
            d.imagePaths.forEach(this::addImgPreview);
        }
        refreshImgLabel();
        lblErrorMsg.setVisible(false);
    }

    private void clearForm() {
        txtName.clear(); txtDescription.clear(); txtStartingPrice.clear();
        cbType.setValue("Art"); cbType.setDisable(false);
        cbCondition.setValue("NEW"); cbCondition.setDisable(false);
        clearCatFields();
        switchPanel();
        datePicker.setValue(null); cbGio.setValue("12"); cbPhut.setValue("00");
        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        refreshImgLabel();
        lblErrorMsg.setVisible(false);
        pendingStartIso = ""; pendingEndIso = "";
    }

    private void clearCatFields() {
        TextField[] fields = {
            txtArtist, txtMedium, txtYearCreated, txtDimensions,
            txtBrand, txtElecModel, txtWarrantyMonths, txtPowerWatts,
            txtMake, txtVehModel, txtYear, txtMileage, txtFuelType
        };
        for (TextField f : fields) if (f != null) f.clear();
    }

    private void addImgPreview(String path) {
        ImageView iv = new ImageView();
        try {
            String src = path.startsWith("file:") || path.startsWith("http") ? path : "file:" + path;
            iv.setImage(new Image(src, true));
        } catch (Exception ignored) {}
        iv.setFitWidth(108); iv.setFitHeight(108); iv.setPreserveRatio(true);
        iv.setStyle("-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.12),5,0,0,0);");
        imageContainer.getChildren().add(iv);
    }

    private void refreshImgLabel() {
        int n = uploadedImagePaths.size();
        lblImageCount.setText(n == 0 ? "Chưa có ảnh nào." : n + " ảnh đã chọn (tối đa 5, mỗi ảnh < 5 MB).");
    }

    private void showMsg(String msg, boolean ok) {
        lblErrorMsg.setText(msg);
        lblErrorMsg.setVisible(true);
        lblErrorMsg.setStyle(ok
                ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;-fx-font-size:13px;"
                : "-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:13px;");
    }

    private void closeAfterDelay(String action) {
        PauseTransition p = new PauseTransition(Duration.seconds(1.2));
        p.setOnFinished(e -> {
            clearForm();
            if (onSaveSuccess != null) onSaveSuccess.accept(action);
        });
        p.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON / String utils
    // ─────────────────────────────────────────────────────────────────────────

    /** Append cặp key-value string vào StringBuilder JSON */
    private void appendStr(StringBuilder sb, String key, String value) {
        sb.append("\"").append(key).append("\":\"").append(esc(value)).append("\"");
    }

    /** Append mảng string vào StringBuilder JSON */
    private void appendStringArray(StringBuilder sb, String key, List<String> values) {
        sb.append("\"").append(key).append("\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(esc(values.get(i))).append("\"");
        }
        sb.append("]");
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String resolveItemId(ProductShared.ProductData p) {
        if (p == null) return "";
        return (p.itemIdRef != null && !p.itemIdRef.isBlank())
                ? p.itemIdRef : String.valueOf(p.id);
    }

    private LocalDateTime parseSafe(String v) {
        try {
            if (v != null && v.contains("T"))
                return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return LocalDateTime.parse(v, DT_FMT);
        } catch (Exception e) {
            return LocalDateTime.now().plusDays(1);
        }
    }

    private String safeStr(JsonObject o, String key) { return safeStr(o, key, ""); }
    private String safeStr(JsonObject o, String key, String fb) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fb;
        return o.get(key).getAsString();
    }
}
