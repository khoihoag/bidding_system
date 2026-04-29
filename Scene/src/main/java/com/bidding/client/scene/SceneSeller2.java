package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SceneSeller2 implements Initializable {

    @FXML private AnchorPane sideMenu;
    @FXML private AnchorPane dimOverlay;
    @FXML private AnchorPane addProductPopup;
    @FXML private AnchorPane accountPopup;
    @FXML private AnchorPane viewProductPopup;
    @FXML private FlowPane productContainer;

    @FXML private TextField txtTenSP;
    @FXML private TextField txtGiaKhoiDiem;
    @FXML private TextArea txtMoTa;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbGio;
    @FXML private ComboBox<String> cbPhut;
    @FXML private ScrollPane imageScrollPane;
    @FXML private HBox imageContainer;
    @FXML private Label lblErrorMsg;
    @FXML private Label lblPopupTitle;
    @FXML private Button btnSubmitProduct;
    @FXML private Label lblProductCount;
    @FXML private Label lblSellerNote;

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

    private boolean isMenuOpen = false;
    private final List<String> uploadedImagePaths = new ArrayList<>();
    private int dummyIdCounter = 1;
    private ProductData productBeingEdited;
    private ProductData productBeingViewed;
    private String currentFilter = "ALL";
    private final List<ProductData> allProducts = new ArrayList<>();
    private ProductData pendingDraft;
    private ProductData pendingDelete;
    private String pendingAction = "";

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static class ProductData {
        int id;
        String itemIdRef;
        String auctionId;
        String name;
        String startPrice;
        String currentPrice;
        String description;
        String startTimeStr;
        String endTimeStr;
        List<String> imagePaths;
        int bidCount;

        // Tao model seller de bind UI va payload.
        ProductData(int id, String itemIdRef, String auctionId, String name, String startPrice, String currentPrice,
                    String description, String startTimeStr, String endTimeStr, List<String> imagePaths, int bidCount) {
            this.id = id;
            this.itemIdRef = itemIdRef;
            this.auctionId = auctionId;
            this.name = name;
            this.startPrice = startPrice;
            this.currentPrice = currentPrice;
            this.description = description;
            this.startTimeStr = startTimeStr;
            this.endTimeStr = endTimeStr;
            this.imagePaths = imagePaths == null ? new ArrayList<>() : new ArrayList<>(imagePaths);
            this.bidCount = bidCount;
        }
    }

    // Khoi tao scene seller va listener server.
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sideMenu.setVisible(false);
        sideMenu.setManaged(false);
        sideMenu.setTranslateX(-300);

        for (int i = 0; i < 24; i++) {
            cbGio.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i < 60; i++) {
            cbPhut.getItems().add(String.format("%02d", i));
        }
        cbGio.setValue("12");
        cbPhut.setValue("00");

        NetworkClient.sellerListener = this::handleSellerResponse;
        lblSellerNote.setText("Dang tai san pham that tu server qua GET_ITEMS.");
        requestSellerItems();
    }

    // Bat tat menu trai.
    @FXML
    void toggleMenu(ActionEvent event) {
        if (isMenuOpen) {
            TranslateTransition transition = new TranslateTransition(Duration.millis(280), sideMenu);
            transition.setToX(-300);
            transition.setInterpolator(Interpolator.EASE_OUT);
            transition.setOnFinished(e -> {
                sideMenu.setVisible(false);
                sideMenu.setManaged(false);
                dimOverlay.setVisible(false);
            });
            transition.play();
            isMenuOpen = false;
        } else {
            sideMenu.setVisible(true);
            sideMenu.setManaged(true);
            sideMenu.setTranslateX(-300);
            dimOverlay.setVisible(true);
            dimOverlay.toFront();
            sideMenu.toFront();

            TranslateTransition transition = new TranslateTransition(Duration.millis(280), sideMenu);
            transition.setToX(0);
            transition.setInterpolator(Interpolator.EASE_OUT);
            transition.play();
            isMenuOpen = true;
        }
    }

    // Dong menu hoac popup dang mo.
    @FXML
    void closePopups(MouseEvent event) {
        closePopupsInternal();
    }

    // Gui yeu cau lay danh sach seller theo doc.
    private void requestSellerItems() {
        NetworkClient.send("{\"action\":\"GET_ITEMS\"}");
    }

    // Xu ly action seller tra ve tu server.
    private void handleSellerResponse(JsonObject response) {
        String action = getString(response, "action");
        switch (action) {
            case "ITEMS_LIST" -> replaceProducts(getArray(response, "items"));
            case "AUCTIONS_LIST" -> replaceProducts(firstNonEmptyArray(getArray(response, "data"), getArray(response, "auctions"), getArray(response, "items")));
            case "ADD_ITEM_REPLY" -> handleUpsertReply(response, "ADD_ITEM");
            case "UPDATE_ITEM_REPLY" -> handleUpsertReply(response, "UPDATE_ITEM");
            case "DELETE_ITEM_REPLY" -> handleDeleteReply(response);
            case "SCHEDULE_AUCTION_REPLY", "START_AUCTION_REPLY" -> handleLifecycleReply(response, action);
            case "GLOBAL_NOTIFY" -> showError(getString(response, "message", "Co thong bao moi tu server."), true);
            case "ERROR" -> showError(getString(response, "message", "Server tra ve loi."), false);
            default -> {
            }
        }
    }

    // Xu ly reply cho add va update item.
    private void handleUpsertReply(JsonObject response, String expectedAction) {
        if (!"SUCCESS".equalsIgnoreCase(getString(response, "status", "SUCCESS"))) {
            showError(getString(response, "message", expectedAction + " that bai."), false);
            return;
        }

        ProductData resolved = extractReplyProduct(response);
        if (resolved != null) {
            upsertProduct(resolved);
        }
        requestSellerItems();
        if ("ADD_ITEM".equals(expectedAction) && resolved == null) {
            showError("Server da xac nhan them san pham. Doc JSON chi tra SUCCESS, client dang tai lai GET_ITEMS de lay itemId that.", true);
        } else {
            showError(expectedAction.equals("ADD_ITEM") ? "Dang san pham thanh cong!" : "Cap nhat san pham thanh cong!", true);
        }
        scheduleAuctionIfNeeded(resolved == null ? pendingDraft : resolved);
        clearPendingState();
        closePopupLater();
    }

    // Xu ly reply khi xoa san pham.
    private void handleDeleteReply(JsonObject response) {
        if (!"SUCCESS".equalsIgnoreCase(getString(response, "status", "SUCCESS"))) {
            showError(getString(response, "message", "Xoa san pham that bai."), false);
            return;
        }

        if (pendingDelete != null) {
            allProducts.removeIf(product -> sameProduct(product, pendingDelete));
            renderProducts();
        }
        showError("Xoa san pham thanh cong!", true);
        clearPendingState();
    }

    // Xu ly reply cho schedule/start auction.
    private void handleLifecycleReply(JsonObject response, String action) {
        if ("SUCCESS".equalsIgnoreCase(getString(response, "status", "SUCCESS"))) {
            String auctionId = getString(response, "auctionId", "");
            if (productBeingViewed != null && !auctionId.isBlank()) {
                productBeingViewed.auctionId = auctionId;
            }
            if ("SCHEDULE_AUCTION_REPLY".equals(action)) {
                showError("Da dong bo lich dau gia voi server.", true);
            } else {
                viewLblMsg.setVisible(true);
                viewLblMsg.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                viewLblMsg.setText(auctionId.isBlank()
                        ? "Server da ghi nhan phien bat dau."
                        : "Server da mo phien dau gia. Auction ID: " + auctionId);
                showError("Server da ghi nhan phien bat dau.", true);
            }
        } else {
            showError(getString(response, "message", "Khong dong bo duoc phien dau gia."), false);
        }
    }

    // Thay toan bo danh sach bang data moi tu server.
    private void replaceProducts(JsonArray incomingItems) {
        List<ProductData> parsed = new ArrayList<>();
        for (JsonElement element : incomingItems) {
            if (element != null && element.isJsonObject()) {
                parsed.add(parseProduct(element.getAsJsonObject()));
            }
        }
        allProducts.clear();
        allProducts.addAll(parsed);
        renderProducts();
    }

    // Ve lai toan bo card seller.
    private void renderProducts() {
        productContainer.getChildren().clear();
        for (ProductData product : allProducts) {
            if (shouldShow(product)) {
                productContainer.getChildren().add(createSellerCard(product));
            }
        }
        updateProductCountLabel();
    }

    // Tao card seller tu ProductData.
    private VBox createSellerCard(ProductData data) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 0; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.setPrefWidth(270);

        ImageView imgView = new ImageView();
        if (!data.imagePaths.isEmpty()) {
            loadImageToView(imgView, data.imagePaths.get(0));
        }
        imgView.setFitHeight(170);
        imgView.setFitWidth(270);
        imgView.setPreserveRatio(false);
        imgView.setStyle("-fx-background-color: #ecf0f1;");

        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 12 12 10 12;");

        Label lblName = new Label(data.name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        lblName.setWrapText(true);
        lblName.setMaxWidth(235);

        Label lblPrice = new Label("Khoi diem: " + formatPrice(data.startPrice) + " d");
        lblPrice.setStyle("-fx-font-size: 14px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");

        Label lblCurrent = new Label("Gia hien tai: " + formatPrice(firstNonBlank(data.currentPrice, data.startPrice)) + " d");
        lblCurrent.setStyle("-fx-font-size: 14px; -fx-text-fill: #1d5fa7; -fx-font-weight: bold;");

        String status = calculateStatus(data.endTimeStr);
        Label lblStatus = new Label(status);
        lblStatus.setStyle("Da ket thuc".equals(status)
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: bold;"
                : "-fx-text-fill: #27ae60; -fx-font-size: 14px; -fx-font-weight: bold;");

        HBox btnBox = new HBox(6);
        btnBox.setStyle("-fx-padding: 5 0 0 0;");

        Button btnView = new Button("Xem");
        btnView.setStyle("-fx-background-color: #12263a; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        btnView.setOnAction(e -> openViewPopup(data));

        Button btnEdit = new Button("Sua");
        btnEdit.setStyle("-fx-background-color: #1d5fa7; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> openEditPopup(data));

        Button btnDelete = new Button("Xoa");
        btnDelete.setStyle("-fx-background-color: #b93832; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> confirmAndDelete(data));

        btnBox.getChildren().addAll(btnView, btnEdit, btnDelete);
        content.getChildren().addAll(lblName, lblPrice, lblCurrent, lblStatus, btnBox);
        card.getChildren().addAll(imgView, content);
        return card;
    }

    // Mo popup xem chi tiet san pham.
    private void openViewPopup(ProductData data) {
        productBeingViewed = data;
        viewLblMsg.setVisible(false);
        viewLblName.setText(data.name);
        viewLblPrice.setText(formatPrice(data.startPrice) + " d");
        viewLblDesc.setText(data.description);
        viewLblEndTime.setText(formatDisplayTime(data.endTimeStr));
        viewLblStatus.setText(calculateStatus(data.endTimeStr));
        viewLblBidCount.setText(data.bidCount + " luot");
        viewLblCurrentPrice.setText(formatPrice(firstNonBlank(data.currentPrice, data.startPrice)) + " d");

        viewImageContainer.getChildren().clear();
        if (!data.imagePaths.isEmpty()) {
            for (String path : data.imagePaths) {
                ImageView imageView = new ImageView();
                loadImageToView(imageView, path);
                imageView.setFitHeight(230);
                imageView.setFitWidth(270);
                imageView.setPreserveRatio(true);
                viewImageContainer.getChildren().add(imageView);
            }
        } else {
            Label noImg = new Label("Khong co anh");
            noImg.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 14px; -fx-padding: 80 60;");
            viewImageContainer.getChildren().add(noImg);
        }

        dimOverlay.setVisible(true);
        dimOverlay.toFront();
        viewProductPopup.setVisible(true);
        viewProductPopup.toFront();
    }

    @FXML
    // Mo phien dau gia ngay lap tuc theo START_AUCTION trong doc.
    void handleStartAuctionNow(ActionEvent event) {
        if (productBeingViewed == null) {
            return;
        }
        if (resolveItemId(productBeingViewed).isBlank()) {
            viewLblMsg.setVisible(true);
            viewLblMsg.setStyle("-fx-text-fill: #b93832; -fx-font-weight: bold;");
            viewLblMsg.setText("Chua co itemId that tu server, nen chua gui duoc START_AUCTION.");
            return;
        }

        int durationMinutes = Math.max(1, (int) ChronoUnit.MINUTES.between(LocalDateTime.now(), parseDateTimeSafe(productBeingViewed.endTimeStr)));
        String startJson = String.format(
                "{\"action\":\"START_AUCTION\",\"itemId\":\"%s\",\"durationMinutes\":%d}",
                escapeJson(resolveItemId(productBeingViewed)),
                durationMinutes
        );
        NetworkClient.send(startJson);
        viewLblMsg.setVisible(true);
        viewLblMsg.setStyle("-fx-text-fill: #1d5fa7; -fx-font-weight: bold;");
        viewLblMsg.setText("Da gui START_AUCTION len server.");
    }

    // Dong popup xem chi tiet.
    @FXML
    void closeViewPopup(ActionEvent event) {
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingViewed = null;
    }

    // Mo popup sua tu popup xem.
    @FXML
    void handleEditFromView(ActionEvent event) {
        if (productBeingViewed == null) {
            return;
        }
        viewProductPopup.setVisible(false);
        openEditPopup(productBeingViewed);
    }

    // Xoa item ngay tu popup xem.
    @FXML
    void handleDeleteFromView(ActionEvent event) {
        if (productBeingViewed == null) {
            return;
        }
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        confirmAndDelete(productBeingViewed);
    }

    // Mo popup them san pham moi.
    @FXML
    void showAddProductPopup(ActionEvent event) {
        productBeingEdited = null;
        lblPopupTitle.setText("Them san pham moi");
        btnSubmitProduct.setText("Dang san pham");
        clearForm();
        if (isMenuOpen) {
            toggleMenu(null);
        }
        dimOverlay.setVisible(true);
        dimOverlay.toFront();
        addProductPopup.setVisible(true);
        addProductPopup.toFront();
    }

    // Mo popup sua san pham da co.
    private void openEditPopup(ProductData data) {
        productBeingEdited = data;
        lblPopupTitle.setText("Chinh sua san pham");
        btnSubmitProduct.setText("Luu thay doi");
        txtTenSP.setText(data.name);
        txtGiaKhoiDiem.setText(data.startPrice);
        txtMoTa.setText(data.description);

        try {
            LocalDateTime endDt = LocalDateTime.parse(data.endTimeStr, DT_FORMATTER);
            datePicker.setValue(endDt.toLocalDate());
            cbGio.setValue(String.format("%02d", endDt.getHour()));
            cbPhut.setValue(String.format("%02d", endDt.getMinute()));
        } catch (Exception exception) {
            datePicker.setValue(LocalDate.now().plusDays(1));
        }

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        uploadedImagePaths.addAll(data.imagePaths);
        for (String path : data.imagePaths) {
            ImageView imageView = new ImageView();
            loadImageToView(imageView, path);
            imageView.setFitHeight(110);
            imageView.setFitWidth(110);
            imageView.setPreserveRatio(true);
            imageContainer.getChildren().add(imageView);
        }

        lblErrorMsg.setVisible(false);
        dimOverlay.setVisible(true);
        dimOverlay.toFront();
        addProductPopup.setVisible(true);
        addProductPopup.toFront();
    }

    // Xac nhan xoa va gui DELETE_ITEM.
    private void confirmAndDelete(ProductData data) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xac nhan xoa");
        confirm.setHeaderText("Xoa san pham \"" + data.name + "\"?");
        confirm.setContentText("Hanh dong nay khong the hoan tac.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                pendingDelete = data;
                pendingAction = "DELETE_ITEM";
                NetworkClient.send(buildDeleteJson(data));
            }
        });
    }

    // Mo file chooser de tai anh len.
    @FXML
    void handleUploadImages(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chon anh san pham");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tap tin anh", "*.png", "*.jpg", "*.jpeg"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        for (File file : selectedFiles) {
            uploadedImagePaths.add(file.getAbsolutePath());
            ImageView imageView = new ImageView();
            loadImageToView(imageView, file.getAbsolutePath());
            imageView.setFitHeight(110);
            imageView.setFitWidth(110);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 4, 0, 0, 0);");
            imageContainer.getChildren().add(imageView);
        }
    }

    // Validate form va gui ADD_ITEM hoac UPDATE_ITEM.
    @FXML
    void handleAddProductSubmit(ActionEvent event) {
        lblErrorMsg.setVisible(false);
        lblErrorMsg.setText("");

        ProductData draft = buildDraftFromForm();
        if (draft == null) {
            return;
        }

        pendingDraft = draft;
        if (productBeingEdited != null) {
            pendingAction = "UPDATE_ITEM";
            NetworkClient.send(buildUpdateJson(draft));
        } else {
            pendingAction = "ADD_ITEM";
            NetworkClient.send(buildAddJson(draft));
        }
        showError("Dang dong bo du lieu voi server...", true);
    }

    // Dong popup them sua.
    @FXML
    void handleCancelAction(ActionEvent event) {
        closePopupsInternal();
    }

    // Loc tat ca san pham.
    @FXML
    void filterAll(ActionEvent event) {
        currentFilter = "ALL";
        renderProducts();
    }

    // Loc san pham dang dau gia.
    @FXML
    void filterActive(ActionEvent event) {
        currentFilter = "ACTIVE";
        renderProducts();
    }

    // Loc san pham da ket thuc.
    @FXML
    void filterEnded(ActionEvent event) {
        currentFilter = "ENDED";
        renderProducts();
    }

    // Chuyen sang man bidder.
    @FXML
    void switchToBidder(ActionEvent event) {
        try {
            Stage stage = (Stage) sideMenu.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nguoi Mua");
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Khong mo duoc man hinh Nguoi Mua", false);
        }
    }

    // Dang xuat ve scene login.
    @FXML
    void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) accountPopup.getScene().getWindow();
            AppNavigator.openEntry(stage, "/com/bidding/client/scene/Scene1.fxml", "Dang nhap he thong");
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Khong mo duoc man hinh dang nhap.", false);
        }
    }

    // Bat tat popup tai khoan.
    @FXML
    void toggleAccountPopup(ActionEvent event) {
        if (accountPopup.isVisible()) {
            accountPopup.setVisible(false);
            dimOverlay.setVisible(false);
        } else {
            if (isMenuOpen) {
                toggleMenu(null);
            }
            dimOverlay.setVisible(true);
            dimOverlay.toFront();
            accountPopup.setVisible(true);
            accountPopup.toFront();
        }
    }

    // Tai avatar moi cho seller.
    @FXML
    public void handleUploadAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chon anh dai dien");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tap tin anh", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            setAvatarImage(selectedFile.toURI().toString());
        }
    }

    // Yeu cau server tra danh sach auction seller.
    @FXML
    void handleManageAuctions(ActionEvent event) {
        NetworkClient.send("{\"action\":\"GET_AUCTIONS\"}");
        showError("Dang tai danh sach phien dau gia...", true);
    }

    // Tam thoi thong bao khu vuc doanh thu chua co API rieng.
    @FXML
    void handleRevenueStats(ActionEvent event) {
        showError("Doanh thu hien tai chua co action rieng trong doc JSON.", true);
    }

    // Dong toan bo popup va reset state.
    private void closePopupsInternal() {
        if (isMenuOpen) {
            toggleMenu(null);
            return;
        }

        addProductPopup.setVisible(false);
        accountPopup.setVisible(false);
        viewProductPopup.setVisible(false);
        dimOverlay.setVisible(false);
        productBeingEdited = null;
        productBeingViewed = null;
    }

    // Tao ProductData tu form popup.
    private ProductData buildDraftFromForm() {
        String tenSP = txtTenSP.getText().trim();
        String gia = txtGiaKhoiDiem.getText().trim();
        String moTa = txtMoTa.getText().trim();

        if (tenSP.length() < 5 || tenSP.length() > 50) {
            showError("Ten san pham phai tu 5 den 50 ky tu!", false);
            return null;
        }
        if (gia.isEmpty() || !gia.matches("\\d+")) {
            showError("Gia khoi diem khong hop le (chi nhap so)!", false);
            return null;
        }
        if (moTa.length() < 50) {
            showError("Mo ta qua ngan! Can it nhat 50 ky tu.", false);
            return null;
        }
        if (datePicker.getValue() == null || cbGio.getValue() == null || cbPhut.getValue() == null) {
            showError("Vui long chon day du ngay gio ket thuc!", false);
            return null;
        }
        if (productBeingEdited == null && uploadedImagePaths.isEmpty()) {
            showError("Bat buoc phai co it nhat 1 hinh anh!", false);
            return null;
        }

        LocalDateTime startTime = productBeingEdited == null
                ? LocalDateTime.now()
                : parseDateTime(firstNonBlank(productBeingEdited.startTimeStr, LocalDateTime.now().format(DT_FORMATTER)));
        LocalDateTime endDateTime = LocalDateTime.of(
                datePicker.getValue(),
                LocalTime.of(Integer.parseInt(cbGio.getValue()), Integer.parseInt(cbPhut.getValue()))
        );

        if (!endDateTime.isAfter(startTime)) {
            showError("Thoi gian ket thuc phai sau hien tai.", false);
            return null;
        }

        int id = productBeingEdited != null ? productBeingEdited.id : dummyIdCounter++;
        String itemIdRef = productBeingEdited != null && productBeingEdited.itemIdRef != null && !productBeingEdited.itemIdRef.isBlank()
                ? productBeingEdited.itemIdRef
                : "";
        String auctionId = productBeingEdited != null && productBeingEdited.auctionId != null && !productBeingEdited.auctionId.isBlank()
                ? productBeingEdited.auctionId
                : "";
        String currentPrice = productBeingEdited != null && productBeingEdited.currentPrice != null && !productBeingEdited.currentPrice.isBlank()
                ? productBeingEdited.currentPrice
                : gia;
        int bidCount = productBeingEdited != null ? productBeingEdited.bidCount : 0;
        List<String> images = uploadedImagePaths.isEmpty() && productBeingEdited != null
                ? new ArrayList<>(productBeingEdited.imagePaths)
                : new ArrayList<>(uploadedImagePaths);

        return new ProductData(
                id,
                itemIdRef,
                auctionId,
                tenSP,
                gia,
                currentPrice,
                moTa,
                startTime.format(DT_FORMATTER),
                endDateTime.format(DT_FORMATTER),
                images,
                bidCount
        );
    }

    // Parse item tu JSON server theo schema linh hoat.
    private ProductData parseProduct(JsonObject object) {
        int id = getInt(object, "itemId", getInt(object, "id", dummyIdCounter++));
        String itemIdRef = firstNonBlank(getString(object, "itemId"), getString(object, "id"), String.valueOf(id));
        String auctionId = firstNonBlank(getString(object, "auctionId"), "");
        String name = firstNonBlank(getString(object, "itemName"), getNestedString(object, "item", "name"), getString(object, "name"), "Chua dat ten");
        String startPrice = String.valueOf(getLong(object, "startingPrice", getLong(object, "startPrice", getLong(object, "price", getLong(object, "currentPrice", 0L)))));
        String currentPrice = String.valueOf(getLong(object, "currentPrice", getLong(object, "newPrice", getLong(object, "startPrice", 0L))));
        String description = firstNonBlank(getString(object, "description"), getString(object, "desc"), "Chua co mo ta");
        String startTime = firstNonBlank(getString(object, "startTime"), LocalDateTime.now().minusMinutes(5).format(DT_FORMATTER));
        String endTime = firstNonBlank(getString(object, "endTime"), LocalDateTime.now().plusDays(1).format(DT_FORMATTER));
        List<String> images = new ArrayList<>();
        JsonArray imageArray = firstNonEmptyArray(getArray(object, "imagePaths"), getArray(object, "images"));
        for (JsonElement imageElement : imageArray) {
            try {
                images.add(imageElement.getAsString());
            } catch (Exception ignored) {
            }
        }
        int bidCount = getInt(object, "bidCount", 0);
        return new ProductData(id, itemIdRef, auctionId, name, startPrice, currentPrice, description, startTime, endTime, images, bidCount);
    }

    // Trich san pham tu payload reply cua server.
    private ProductData extractReplyProduct(JsonObject response) {
        if (response.has("data") && response.get("data").isJsonObject()) {
            return parseProduct(response.getAsJsonObject("data"));
        }
        if (response.has("item") && response.get("item").isJsonObject()) {
            return parseProduct(response.getAsJsonObject("item"));
        }
        return null;
    }

    // Them moi hoac cap nhat item trong danh sach hien tai.
    private void upsertProduct(ProductData incoming) {
        for (int index = 0; index < allProducts.size(); index++) {
            if (sameProduct(allProducts.get(index), incoming)) {
                allProducts.set(index, incoming);
                renderProducts();
                return;
            }
        }
        allProducts.add(0, incoming);
        renderProducts();
    }

    // Gui add item theo schema server.
    private String buildAddJson(ProductData product) {
        return buildItemJson("ADD_ITEM", product);
    }

    // Gui update item theo schema server.
    private String buildUpdateJson(ProductData product) {
        return buildItemJson("UPDATE_ITEM", product);
    }

    // Tao payload chung cho add va update.
    private String buildItemJson(String action, ProductData product) {
        if ("ADD_ITEM".equals(action)) {
            return String.format(
                    "{\"action\":\"ADD_ITEM\",\"name\":\"%s\",\"description\":\"%s\",\"startingPrice\":%s}",
                    escapeJson(product.name),
                    escapeJson(product.description),
                    normalizeNumber(product.startPrice)
            );
        }
        return String.format(
                "{\"action\":\"UPDATE_ITEM\",\"itemId\":\"%s\",\"name\":\"%s\",\"description\":\"%s\"}",
                escapeJson(resolveItemId(product)),
                escapeJson(product.name),
                escapeJson(product.description)
        );
    }

    // Tao payload xoa item.
    private String buildDeleteJson(ProductData product) {
        return String.format(
                "{\"action\":\"DELETE_ITEM\",\"itemId\":\"%s\"}",
                escapeJson(resolveItemId(product))
        );
    }

    // Tu dong gui schedule auction neu can.
    private void scheduleAuctionIfNeeded(ProductData product) {
        if (product == null || resolveItemId(product).isBlank()) {
            return;
        }
        String scheduleJson = String.format(
                "{\"action\":\"SCHEDULE_AUCTION\",\"itemId\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                escapeJson(resolveItemId(product)),
                escapeJson(toServerDateTime(product.startTimeStr)),
                escapeJson(toServerDateTime(product.endTimeStr))
        );
        NetworkClient.send(scheduleJson);
    }

    // Dong popup sau khi server xac nhan.
    private void closePopupLater() {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            closePopupsInternal();
            clearForm();
        });
        pause.play();
    }

    // Reset state tam khi gui request.
    private void clearPendingState() {
        pendingAction = "";
        pendingDraft = null;
        pendingDelete = null;
    }

    // Xoa trang thai tren form.
    private void clearForm() {
        txtTenSP.clear();
        txtGiaKhoiDiem.clear();
        txtMoTa.clear();
        datePicker.setValue(null);
        cbGio.setValue("12");
        cbPhut.setValue("00");
        imageContainer.getChildren().clear();
        uploadedImagePaths.clear();
        lblErrorMsg.setText("");
        lblErrorMsg.setVisible(false);
    }

    // Tinh xem card co can hien theo filter khong.
    private boolean shouldShow(ProductData data) {
        boolean ended = "Da ket thuc".equals(calculateStatus(data.endTimeStr));
        return switch (currentFilter) {
            case "ACTIVE" -> !ended;
            case "ENDED" -> ended;
            default -> true;
        };
    }

    // Cap nhat so luong san pham dang hien thi.
    private void updateProductCountLabel() {
        lblProductCount.setText(productContainer.getChildren().size() + " san pham dang hien thi");
        if (productContainer.getChildren().isEmpty()) {
            lblSellerNote.setText("Chua co du lieu san pham that tu server. Neu server khong tra GET_ITEMS/AUCTIONS_LIST thi man nay se de trong.");
        } else {
            lblSellerNote.setText("Danh sach dang hien thi tu du lieu that cua server.");
        }
    }

    // Hien thong bao tren popup seller.
    private void showError(String message, boolean isSuccess) {
        lblErrorMsg.setText(message);
        lblErrorMsg.setVisible(true);
        lblErrorMsg.setStyle(isSuccess
                ? "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13px;"
                : "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13px;");
    }

    // Tinh trang thai con han hay da het han.
    private String calculateStatus(String endTimeStr) {
        try {
            LocalDateTime endTime = parseDateTime(endTimeStr);
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(endTime)) {
                return "Da ket thuc";
            }
            long minutesLeft = ChronoUnit.MINUTES.between(now, endTime);
            if (minutesLeft > 24 * 60) {
                return "Con " + (minutesLeft / 60 / 24) + " ngay nua";
            }
            return "Con " + (minutesLeft / 60) + " gio " + (minutesLeft % 60) + " phut";
        } catch (Exception exception) {
            return "Khong xac dinh";
        }
    }

    // Format tien ve dang de doc.
    private String formatPrice(String rawPrice) {
        try {
            long val = Long.parseLong(normalizeNumber(rawPrice));
            return String.format("%,d", val).replace(',', '.');
        } catch (Exception exception) {
            return rawPrice;
        }
    }

    // Format thoi gian de hien thi len popup.
    private String formatDisplayTime(String endTimeStr) {
        try {
            return parseDateTime(endTimeStr).format(DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy"));
        } catch (Exception exception) {
            return endTimeStr;
        }
    }

    // Set avatar tron cho button.
    private void setAvatarImage(String imagePath) {
        try {
            Image image = new Image(imagePath);
            ImageView imgView = new ImageView(image);
            imgView.setFitWidth(42);
            imgView.setFitHeight(42);
            imgView.setPreserveRatio(false);
            imgView.setClip(new Circle(21, 21, 21));
            btnAvatar.setGraphic(imgView);
            btnAvatar.setText("");
        } catch (Exception exception) {
            System.out.println("Loi load avatar: " + exception.getMessage());
        }
    }

    // Load anh tu file path hoac URL.
    private void loadImageToView(ImageView imageView, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            String source = path.startsWith("file:") || path.startsWith("http") ? path : "file:" + path;
            imageView.setImage(new Image(source, true));
        } catch (Exception ignored) {
        }
    }

    // So sanh hai product theo itemId, auctionId hoac id.
    private boolean sameProduct(ProductData left, ProductData right) {
        if (left == null || right == null) {
            return false;
        }
        if (!resolveItemId(left).isBlank() && !resolveItemId(right).isBlank()) {
            return resolveItemId(left).equals(resolveItemId(right));
        }
        if (left.auctionId != null && !left.auctionId.isBlank() && right.auctionId != null && !right.auctionId.isBlank()) {
            return left.auctionId.equals(right.auctionId);
        }
        return left.id == right.id;
    }

    // Chuan hoa chuoi so de dua vao JSON.
    private String normalizeNumber(String rawValue) {
        return rawValue == null || rawValue.isBlank() ? "0" : rawValue.replace(".", "").replace(",", "").trim();
    }

    // Tao mang imagePaths dang JSON.
    private String buildImageArrayJson(List<String> paths) {
        List<String> safePaths = paths == null ? new ArrayList<>() : paths;
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < safePaths.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(safePaths.get(index))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    // Escape text truoc khi dua vao JSON.
    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Parse LocalDateTime theo format chung.
    private LocalDateTime parseDateTime(String value) {
        if (value != null && value.contains("T")) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.parse(value, DT_FORMATTER);
    }

    // Parse an toan khi gia tri thoi gian loi.
    private LocalDateTime parseDateTimeSafe(String value) {
        try {
            return parseDateTime(value);
        } catch (Exception exception) {
            return LocalDateTime.now().plusHours(1);
        }
    }

    // Chuyen datetime client sang format server co chu T.
    private String toServerDateTime(String value) {
        return parseDateTimeSafe(value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Lay itemId that de gui len server.
    private String resolveItemId(ProductData product) {
        if (product == null) {
            return "";
        }
        return firstNonBlank(product.itemIdRef, String.valueOf(product.id));
    }

    // Lay string tu object.
    private String getString(JsonObject object, String key) {
        return getString(object, key, "");
    }

    // Lay string tu object voi fallback.
    private String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    // Lay string long nhau tu object con.
    private String getNestedString(JsonObject object, String objectKey, String valueKey) {
        if (object == null || !object.has(objectKey) || !object.get(objectKey).isJsonObject()) {
            return "";
        }
        return getString(object.getAsJsonObject(objectKey), valueKey, "");
    }

    // Lay int tu object voi fallback.
    private int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception exception) {
            try {
                return Integer.parseInt(object.get(key).getAsString().replaceAll("[^0-9-]", ""));
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    // Lay long tu object voi fallback.
    private long getLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception exception) {
            try {
                return Long.parseLong(object.get(key).getAsString().replace(".", "").replace(",", "").trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    // Lay mang tu object.
    private JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    // Chon mang dau tien khong rong.
    private JsonArray firstNonEmptyArray(JsonArray... arrays) {
        for (JsonArray array : arrays) {
            if (array != null && !array.isEmpty()) {
                return array;
            }
        }
        return new JsonArray();
    }

    // Chon chuoi dau tien co noi dung.
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
