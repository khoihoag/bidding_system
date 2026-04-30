package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SceneAddItem implements Initializable {

    @FXML private Label lblPageTitle;
    @FXML private Label lblSubtitle;

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbCondition;

    @FXML private VBox panelArt;
    @FXML private TextField txtArtist;
    @FXML private TextField txtMedium;
    @FXML private TextField txtYearCreated;
    @FXML private TextField txtDimensions;

    @FXML private VBox panelElectronics;
    @FXML private TextField txtBrand;
    @FXML private TextField txtElecModel;
    @FXML private TextField txtWarrantyMonths;
    @FXML private TextField txtPowerWatts;

    @FXML private VBox panelVehicle;
    @FXML private TextField txtMake;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtYear;
    @FXML private TextField txtMileage;
    @FXML private TextField txtFuelType;

    @FXML private HBox imageContainer;
    @FXML private Label lblImageCount;
    @FXML private Label lblErrorMsg;
    @FXML private Button btnSubmit;

    private ProductShared.ProductData editingProduct;
    private final List<String> uploadedImagePaths = new ArrayList<>();

    private Consumer<String> onSaveSuccess;
    private Runnable onCancel;

    // Khoi tao form va listener cho reply add/update.
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.setValue("Art");
        cbType.setOnAction(event -> switchPanel());
        switchPanel();

        cbCondition.getItems().addAll("NEW", "USED", "LIKE_NEW", "DAMAGED");
        cbCondition.setValue("NEW");

        NetworkClient.addItemListener = this::onServerResponse;
    }

    // Cau hinh che do them moi.
    public void initAdd(Consumer<String> successCb, Runnable cancelCb) {
        editingProduct = null;
        onSaveSuccess = successCb;
        onCancel = cancelCb;
        lblPageTitle.setText("Them san pham moi");
        lblSubtitle.setText("Man nay chi gui JSON tao san pham. Viec len lich dau gia xu ly o man khac.");
        btnSubmit.setText("Dang san pham");
        clearForm();
    }

    // Cau hinh che do chinh sua.
    public void initEdit(ProductShared.ProductData data, Consumer<String> successCb, Runnable cancelCb) {
        editingProduct = data;
        onSaveSuccess = successCb;
        onCancel = cancelCb;
        lblPageTitle.setText("Chinh sua san pham");
        lblSubtitle.setText("Chi sua Ten va Mo ta. Khong doi loai san pham khi da tao.");
        btnSubmit.setText("Luu thay doi");
        fillForm(data);
    }

    // Doi panel thuoc tinh theo type dang chon.
    @FXML
    void handleTypeChange(ActionEvent event) {
        switchPanel();
    }

    // Chon anh tu may va luu danh sach path de gui len server.
    @FXML
    void handleUploadImages(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chon hinh san pham");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) imageContainer.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) {
            return;
        }

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        for (File file : files) {
            if (uploadedImagePaths.size() >= 5) {
                showMsg("Toi da 5 anh. Cac anh du bi bo qua.", false);
                break;
            }
            if (file.length() > 5 * 1024 * 1024L) {
                showMsg("Bo qua " + file.getName() + " vi vuot 5 MB.", false);
                continue;
            }
            uploadedImagePaths.add(file.getAbsolutePath());
            addImgPreview(file.getAbsolutePath());
        }
        refreshImgLabel();
    }

    // Chon luong add hoac update theo trang thai man hinh.
    @FXML
    void handleSubmit(ActionEvent event) {
        lblErrorMsg.setVisible(false);
        if (editingProduct != null) {
            submitUpdate();
        } else {
            submitAdd();
        }
    }

    // Dong man va tra quyen dieu huong cho caller.
    @FXML
    void handleCancel(ActionEvent event) {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // Gui UPDATE_ITEM voi itemId, name, description.
    private void submitUpdate() {
        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();
        if (!validateNameDesc(name, desc)) {
            return;
        }

        String json = String.format(
                "{\"action\":\"UPDATE_ITEM\",\"itemId\":\"%s\",\"name\":\"%s\",\"description\":\"%s\"}",
                esc(resolveItemId(editingProduct)), esc(name), esc(desc));
        NetworkClient.send(json);
        showMsg("Dang gui UPDATE_ITEM len server...", true);
    }

    // Gui duy nhat ADD_ITEM, khong tu dong gui SCHEDULE_AUCTION nua.
    private void submitAdd() {
        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();
        String price = txtStartingPrice.getText().trim();
        String type = cbType.getValue();
        String condition = cbCondition.getValue();

        if (!validateNameDesc(name, desc)) {
            return;
        }
        if (price.isEmpty() || !price.matches("\\d+(\\.\\d+)?") || Double.parseDouble(price) <= 0) {
            showMsg("Gia khoi diem phai la so duong.", false);
            return;
        }
        if (uploadedImagePaths.isEmpty()) {
            showMsg("Bat buoc phai co it nhat 1 hinh anh.", false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendStr(sb, "action", "ADD_ITEM");
        sb.append(",");
        appendStr(sb, "name", name);
        sb.append(",");
        appendStr(sb, "description", desc);
        sb.append(",");
        sb.append("\"startingPrice\":").append(price);
        sb.append(",");
        appendStr(sb, "type", type);
        sb.append(",");
        appendStr(sb, "condition", condition);
        sb.append(",");
        appendStringArray(sb, "images", uploadedImagePaths);

        try {
            switch (type) {
                case "Art" -> appendArtFields(sb);
                case "Electronics" -> appendElectronicsFields(sb);
                case "Vehicle" -> appendVehicleFields(sb);
                default -> {
                    showMsg("Loai san pham khong hop le theo server hien tai.", false);
                    return;
                }
            }
        } catch (IllegalArgumentException exception) {
            return;
        }

        sb.append("}");
        NetworkClient.send(sb.toString());
        showMsg("Dang gui ADD_ITEM len server...", true);
    }

    // Them field dac thu cho Art.
    private void appendArtFields(StringBuilder sb) {
        String artist = txtArtist.getText().trim();
        String medium = txtMedium.getText().trim();
        String yearStr = txtYearCreated.getText().trim();
        String dimensions = txtDimensions.getText().trim();

        if (artist.isEmpty()) {
            throwValidation("Vui long nhap Nghe si / Tac gia.");
        }
        if (medium.isEmpty()) {
            throwValidation("Vui long nhap Chat lieu.");
        }

        int yearCreated;
        try {
            yearCreated = Integer.parseInt(yearStr);
        } catch (Exception exception) {
            throwValidation("Nam sang tac phai la so nguyen.");
            return;
        }

        sb.append(",");
        appendStr(sb, "artist", artist);
        sb.append(",");
        appendStr(sb, "medium", medium);
        sb.append(",\"yearCreated\":").append(yearCreated);
        sb.append(",");
        appendStr(sb, "dimensions", dimensions);
    }

    // Them field dac thu cho Electronics.
    private void appendElectronicsFields(StringBuilder sb) {
        String brand = txtBrand.getText().trim();
        String model = txtElecModel.getText().trim();
        String warrantyStr = txtWarrantyMonths.getText().trim();
        String powerStr = txtPowerWatts.getText().trim();

        if (brand.isEmpty()) {
            throwValidation("Vui long nhap Hang san xuat.");
        }
        if (model.isEmpty()) {
            throwValidation("Vui long nhap Model.");
        }

        int warrantyMonths;
        int powerWatts = 0;
        try {
            warrantyMonths = Integer.parseInt(warrantyStr);
        } catch (Exception exception) {
            throwValidation("Bao hanh (thang) phai la so nguyen.");
            return;
        }
        try {
            if (!powerStr.isEmpty()) {
                powerWatts = Integer.parseInt(powerStr);
            }
        } catch (Exception exception) {
            throwValidation("Cong suat (Watts) phai la so nguyen.");
            return;
        }

        sb.append(",");
        appendStr(sb, "brand", brand);
        sb.append(",");
        appendStr(sb, "model", model);
        sb.append(",\"warrantyMonths\":").append(warrantyMonths);
        sb.append(",\"powerWatts\":").append(powerWatts);
    }

    // Them field dac thu cho Vehicle.
    private void appendVehicleFields(StringBuilder sb) {
        String make = txtMake.getText().trim();
        String model = txtVehModel.getText().trim();
        String yearStr = txtYear.getText().trim();
        String mileageStr = txtMileage.getText().trim();
        String fuelType = txtFuelType.getText().trim();

        if (make.isEmpty()) {
            throwValidation("Vui long nhap Hang xe.");
        }
        if (model.isEmpty()) {
            throwValidation("Vui long nhap Ten / Model xe.");
        }
        if (fuelType.isEmpty()) {
            throwValidation("Vui long nhap Loai nhien lieu.");
        }

        int yearValue;
        int mileage = 0;
        try {
            yearValue = Integer.parseInt(yearStr);
        } catch (Exception exception) {
            throwValidation("Nam san xuat phai la so nguyen.");
            return;
        }
        try {
            if (!mileageStr.isEmpty()) {
                mileage = Integer.parseInt(mileageStr);
            }
        } catch (Exception exception) {
            throwValidation("So km da chay phai la so nguyen.");
            return;
        }

        sb.append(",");
        appendStr(sb, "make", make);
        sb.append(",");
        appendStr(sb, "model", model);
        sb.append(",\"year\":").append(yearValue);
        sb.append(",\"mileage\":").append(mileage);
        sb.append(",");
        appendStr(sb, "fuelType", fuelType);
    }

    // Xu ly reply add/update tu server.
    private void onServerResponse(JsonObject resp) {
        String action = safeStr(resp, "action");
        String status = safeStr(resp, "status", "SUCCESS");

        switch (action) {
            case "ADD_ITEM_REPLY" -> {
                if (!"SUCCESS".equalsIgnoreCase(status)) {
                    showMsg("Dang san pham that bai: " + safeStr(resp, "message"), false);
                    return;
                }
                showMsg("Dang san pham thanh cong!", true);
                closeAfterDelay("ADD_ITEM");
            }
            case "UPDATE_ITEM_REPLY" -> {
                if (!"SUCCESS".equalsIgnoreCase(status)) {
                    showMsg("Cap nhat that bai: " + safeStr(resp, "message"), false);
                    return;
                }
                showMsg("Cap nhat thanh cong!", true);
                closeAfterDelay("UPDATE_ITEM");
            }
            case "ERROR" -> showMsg("Loi server: " + safeStr(resp, "message"), false);
            default -> {
            }
        }
    }

    // An hien panel field dac thu theo type.
    private void switchPanel() {
        String type = cbType.getValue();
        set(panelArt, "Art".equals(type));
        set(panelElectronics, "Electronics".equals(type));
        set(panelVehicle, "Vehicle".equals(type));
    }

    // Dong bo visible va managed cho panel.
    private void set(VBox panel, boolean show) {
        panel.setVisible(show);
        panel.setManaged(show);
    }

    // Validate ten va mo ta cho ca add va update.
    private boolean validateNameDesc(String name, String desc) {
        if (name.length() < 2 || name.length() > 200) {
            showMsg("Ten san pham phai tu 2 den 200 ky tu.", false);
            return false;
        }
        if (desc.length() < 10) {
            showMsg("Mo ta qua ngan, can it nhat 10 ky tu.", false);
            return false;
        }
        return true;
    }

    // Nap san du lieu san pham vao form khi sua.
    private void fillForm(ProductShared.ProductData data) {
        txtName.setText(data.name);
        txtDescription.setText(data.description);
        txtStartingPrice.setText(data.startPrice);
        cbType.setDisable(true);
        cbCondition.setDisable(true);

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        if (data.imagePaths != null) {
            uploadedImagePaths.addAll(data.imagePaths);
            data.imagePaths.forEach(this::addImgPreview);
        }
        refreshImgLabel();
        lblErrorMsg.setVisible(false);
    }

    // Reset form ve trang thai them moi.
    private void clearForm() {
        txtName.clear();
        txtDescription.clear();
        txtStartingPrice.clear();
        cbType.setValue("Art");
        cbType.setDisable(false);
        cbCondition.setValue("NEW");
        cbCondition.setDisable(false);
        clearCatFields();
        switchPanel();
        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        refreshImgLabel();
        lblErrorMsg.setVisible(false);
    }

    // Xoa cac field thuoc tinh dac thu.
    private void clearCatFields() {
        TextField[] fields = {
                txtArtist, txtMedium, txtYearCreated, txtDimensions,
                txtBrand, txtElecModel, txtWarrantyMonths, txtPowerWatts,
                txtMake, txtVehModel, txtYear, txtMileage, txtFuelType
        };
        for (TextField field : fields) {
            if (field != null) {
                field.clear();
            }
        }
    }

    // Tao preview anh tren form.
    private void addImgPreview(String path) {
        ImageView imageView = new ImageView();
        try {
            String source = path.startsWith("file:") || path.startsWith("http") ? path : "file:" + path;
            imageView.setImage(new Image(source, true));
        } catch (Exception ignored) {
        }
        imageView.setFitWidth(108);
        imageView.setFitHeight(108);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.12),5,0,0,0);");
        imageContainer.getChildren().add(imageView);
    }

    // Cap nhat label so luong anh.
    private void refreshImgLabel() {
        int count = uploadedImagePaths.size();
        if (count == 0) {
            lblImageCount.setText("Chua co anh nao.");
        } else {
            lblImageCount.setText(count + " anh da chon (toi da 5, moi anh < 5 MB).");
        }
    }

    // Hien thong bao thanh cong hoac loi tren form.
    private void showMsg(String msg, boolean ok) {
        lblErrorMsg.setText(msg);
        lblErrorMsg.setVisible(true);
        lblErrorMsg.setStyle(ok
                ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;-fx-font-size:13px;"
                : "-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:13px;");
    }

    // Dong form sau mot khoang nho de user kip thay ket qua.
    private void closeAfterDelay(String action) {
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(event -> {
            clearForm();
            if (onSaveSuccess != null) {
                onSaveSuccess.accept(action);
            }
        });
        pause.play();
    }

    // Append string field vao JSON builder.
    private void appendStr(StringBuilder sb, String key, String value) {
        sb.append("\"").append(key).append("\":\"").append(esc(value)).append("\"");
    }

    // Append mang string vao JSON builder.
    private void appendStringArray(StringBuilder sb, String key, List<String> values) {
        sb.append("\"").append(key).append("\":[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                sb.append(",");
            }
            sb.append("\"").append(esc(values.get(index))).append("\"");
        }
        sb.append("]");
    }

    // Escape chuoi de chen an toan vao JSON string.
    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // Lay itemId uu tien theo ref server neu co.
    private String resolveItemId(ProductShared.ProductData product) {
        if (product == null) {
            return "";
        }
        return (product.itemIdRef != null && !product.itemIdRef.isBlank())
                ? product.itemIdRef
                : String.valueOf(product.id);
    }

    // Doc string an toan tu JsonObject.
    private String safeStr(JsonObject object, String key) {
        return safeStr(object, key, "");
    }

    // Doc string an toan tu JsonObject voi fallback.
    private String safeStr(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    // Nem loi validation noi bo de dung build payload.
    private void throwValidation(String message) {
        showMsg(message, false);
        throw new IllegalArgumentException(message);
    }
}
