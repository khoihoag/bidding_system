package com.bidding.controller;

import com.bidding.controller.auctiondetail.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane; // ĐÃ THÊM IMPORT
import javafx.scene.control.SplitPane;  // ĐÃ THÊM IMPORT
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AuctionDetailController {
    @FXML private Button btnFollow;
    @FXML private Button btnEnterTradingRoom;
    // ================= BIẾN CỦA MÀN 1 (SOTHEBY'S SHOWCASE) =================
    @FXML private ScrollPane showcaseView;
    @FXML private SplitPane tradingRoomView;
    @FXML private javafx.scene.image.ImageView showcaseImageView;
    @FXML private Button showcasePrevImageButton;
    @FXML private Button showcaseNextImageButton;
    @FXML private Label showcaseImageCounterLabel;
    @FXML private HBox showcaseThumbnailsBox;
    @FXML private Label showcaseDescLabel;
    @FXML private VBox showcaseSpecsBox;
    @FXML private Label showcaseNameLabel;
    @FXML private Label showcaseTimeLabel;
    @FXML private Label showcasePriceLabel;
    @FXML private Label showcaseWinnerLabel;
    @FXML private Label sellerAvatarLabel;
    @FXML private Label sellerNameLabel;
    @FXML private Label sellerMetaLabel;

    // ================= BIẾN CỦA MÀN 2 (TRADING ROOM - PHÒNG GIAO DỊCH) =================
    @FXML private VBox manualBidContainer;
    @FXML private VBox autoBidContainer;
    @FXML private Label headerAuctionId;
    @FXML private Label statusBadgeHeader;
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private Label currentWinnerLabel;
    @FXML private Label totalBidsLabel;
    @FXML private TextField bidAmountField;
    @FXML private Label bidErrorLabel;
    @FXML private Button bidButton;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label autoBidErrorLabel;
    @FXML private Button autoBidButton;
    @FXML private Label resultLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private AreaChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;
    @FXML private HBox tradingThumbnailsBox;
    @FXML private VBox tradingImagePreview;
    @FXML private ImageView tradingImageMainView;
    @FXML private StackPane tradingImageMainContainer;
    @FXML private StackPane tradingChartFrame;

    private final AuctionDetailState state = new AuctionDetailState();
    private AuctionDetailNetworkGateway network;
    private AuctionDetailUiPresenter ui;
    private AuctionDetailChartBinder chartBinder;
    private AuctionDetailCountdown countdown;
    private AuctionDetailBidHandler bidHandler;
    private AuctionDetailResponseHandler responseHandler;
    private final List<String> showcaseImagePaths = new ArrayList<>();
    private int currentShowcaseImageIndex = 0;
    private String currentSellerId;
    private String currentSellerName;
    private Dialog<Void> sellerProfileDialog;
    private Label sellerProfileNameValue;
    private Label sellerProfileEmailValue;
    private Label sellerProfileCreatedAuctionsValue;
    private Label sellerProfileSuccessfulAuctionsValue;
    private Label sellerProfileCanceledAuctionsValue;
    private Label sellerProfileWonItemsValue;
    private Label sellerProfileAuctionProductsValue;
    private boolean sellerProfileLoading;
    private int sellerProfileRequestVersion;

    @FXML
    public void initialize() {
        ui = new AuctionDetailUiPresenter(btnFollow, btnEnterTradingRoom,
                state, itemNameLabel, currentPriceLabel, statusBadgeHeader,
                currentWinnerLabel, totalBidsLabel, timeRemainingLabel, resultLabel, lastUpdateLabel,
                bidErrorLabel, autoBidErrorLabel, bidAmountField, maxBidField, incrementField,
                bidButton, autoBidButton, manualBidContainer, autoBidContainer);

        // Binding dữ liệu từ Màn 2 ra Màn 1 để giá nhảy Realtime
        showcaseNameLabel.textProperty().bind(itemNameLabel.textProperty());
        showcasePriceLabel.textProperty().bind(currentPriceLabel.textProperty());
        showcaseTimeLabel.textProperty().bind(timeRemainingLabel.textProperty());
        showcaseWinnerLabel.textProperty().bind(currentWinnerLabel.textProperty());

        network = new AuctionDetailNetworkGateway();
        chartBinder = new AuctionDetailChartBinder(state, priceChart, ui);
        countdown = new AuctionDetailCountdown(
                state, timeRemainingLabel, bidButton, autoBidButton);
        bidHandler = new AuctionDetailBidHandler(state, network, ui);
        responseHandler = new AuctionDetailResponseHandler(
                AuctionDetailController.class, state, ui, chartBinder, countdown, this);

        chartBinder.setupChart();
        configureTradingChartFrameClip();
        ui.applyInitialStyles();
        network.registerMessageHandler(responseHandler::handle);

        String auctionId = (String) AppNavigator.getData();
        if (auctionId != null) {
            initData(auctionId);
        } else {
            System.err.println("Lỗi: Không nhận được ID phiên đấu giá!");
        }
    }

    private void configureTradingChartFrameClip() {
        if (tradingChartFrame == null) return;

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(tradingChartFrame.widthProperty());
        clip.heightProperty().bind(tradingChartFrame.heightProperty());
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        tradingChartFrame.setClip(clip);
    }

    public void initData(String auctionId) {
        state.currentAuctionId = auctionId;
        headerAuctionId.setText("ID: " + auctionId);
        network.requestAuctionHistory(auctionId);
        network.requestAuctions();
    }

    @FXML
    private void handleEnterTradingRoom() {
        showcaseView.setVisible(false);
        showcaseView.setManaged(false);

        tradingRoomView.setVisible(true);
        tradingRoomView.setManaged(true);

        updateTradingImageViews();
    }

    @FXML
    private void handleBackToShowcase() {
        tradingRoomView.setVisible(false);
        tradingRoomView.setManaged(false);

        showcaseView.setVisible(true);
        showcaseView.setManaged(true);
    }

    @FXML
    private void handleViewTradingImage() {
        if (showcaseImagePaths.isEmpty()) {
            AuctionDetailItemDialog.showWarning(AuctionDetailController.class, "Phiên này chưa có hình ảnh để xem.");
            return;
        }
        showTradingImageDialog(currentShowcaseImageIndex);
    }

    private void updateTradingImageViews() {
        if (tradingImagePreview != null) {
            boolean hasImages = !showcaseImagePaths.isEmpty();
            tradingImagePreview.setVisible(hasImages);
            tradingImagePreview.setManaged(hasImages);
        }
        if (tradingImageMainView != null && tradingImageMainContainer != null) {
            loadTradingMainAt(currentShowcaseImageIndex);
        }
        renderTradingThumbnails();
    }

    private void loadTradingMainAt(int index) {
        if (tradingImageMainView == null || tradingImageMainContainer == null) return;

        tradingImageMainContainer.getChildren().removeIf(node -> "trading-no-image".equals(node.getUserData()));

        if (showcaseImagePaths.isEmpty() || index < 0 || index >= showcaseImagePaths.size()) {
            tradingImageMainView.setImage(null);
            return;
        }

        try {
            File file = new File(showcaseImagePaths.get(index));
            if (file.exists()) {
                tradingImageMainView.setImage(new Image(file.toURI().toString()));
                return;
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh chính khi đấu giá: " + e.getMessage());
        }

        tradingImageMainView.setImage(null);
        Label noImgLabel = new Label("Không có hình ảnh");
        noImgLabel.setUserData("trading-no-image");
        noImgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 16px;");
        StackPane.setAlignment(noImgLabel, javafx.geometry.Pos.CENTER);
        tradingImageMainContainer.getChildren().add(noImgLabel);
    }

    private void renderTradingThumbnails() {
        if (tradingThumbnailsBox == null) return;

        tradingThumbnailsBox.getChildren().clear();

        for (int i = 0; i < showcaseImagePaths.size(); i++) {
            int imageIndex = i;
            StackPane thumbnail = new StackPane();
            thumbnail.getStyleClass().add(imageIndex == currentShowcaseImageIndex
                    ? "trading-thumb-active"
                    : "trading-thumb-muted");
            thumbnail.setOnMouseClicked(event -> {
                currentShowcaseImageIndex = imageIndex;
                loadTradingMainAt(currentShowcaseImageIndex);
                loadShowcaseImageAt(currentShowcaseImageIndex);
                updateShowcaseImageNavigation();
                renderShowcaseThumbnails();
                renderTradingThumbnails();
            });

            File file = new File(showcaseImagePaths.get(imageIndex));
            if (file.exists()) {
                ImageView thumbnailImage = new ImageView(new Image(file.toURI().toString()));
                thumbnailImage.setFitWidth(60);
                thumbnailImage.setFitHeight(52);
                thumbnailImage.setPreserveRatio(true);
                thumbnailImage.setSmooth(true);
                thumbnail.getChildren().add(thumbnailImage);
            } else {
                Label fallbackLabel = new Label(String.valueOf(imageIndex + 1));
                fallbackLabel.getStyleClass().add("auction-detail-thumb-label");
                thumbnail.getChildren().add(fallbackLabel);
            }

            tradingThumbnailsBox.getChildren().add(thumbnail);
        }
    }

    private void showTradingImageDialog(int startIndex) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Xem ảnh vật phẩm");

        try {
            dialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception ignored) {
        }

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 18;");

        ImageView imgView = new ImageView();
        imgView.setFitWidth(680);
        imgView.setFitHeight(480);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);

        Label counter = new Label();
        counter.setStyle("-fx-text-fill: #e5e7eb; -fx-font-weight: 800;");

        Button prevBtn = new Button("←");
        Button nextBtn = new Button("→");
        HBox nav = new HBox(12, prevBtn, nextBtn);
        nav.setAlignment(javafx.geometry.Pos.CENTER);

        final int[] idx = {
                Math.max(0, Math.min(startIndex, showcaseImagePaths.size() - 1))
        };

        Runnable update = () -> {
            if (showcaseImagePaths.isEmpty()) {
                imgView.setImage(null);
                counter.setText("");
                prevBtn.setDisable(true);
                nextBtn.setDisable(true);
                return;
            }

            String path = showcaseImagePaths.get(idx[0]);
            try {
                File file = new File(path);
                if (file.exists()) {
                    imgView.setImage(new Image(file.toURI().toString()));
                } else {
                    imgView.setImage(null);
                }
            } catch (Exception e) {
                imgView.setImage(null);
            }

            counter.setText((idx[0] + 1) + " / " + showcaseImagePaths.size());
            boolean hasMultiple = showcaseImagePaths.size() > 1;
            prevBtn.setDisable(!hasMultiple);
            nextBtn.setDisable(!hasMultiple);
        };

        prevBtn.setOnAction(e -> {
            idx[0] = (idx[0] - 1 + showcaseImagePaths.size()) % showcaseImagePaths.size();
            update.run();
        });
        nextBtn.setOnAction(e -> {
            idx[0] = (idx[0] + 1) % showcaseImagePaths.size();
            update.run();
        });

        update.run();

        content.getChildren().addAll(imgView, counter, nav);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // ================= TỰ ĐỘNG ĐỔ ẢNH VÀ MÔ TẢ RA MÀN HÌNH CHÍNH =================
    // ================= TỰ ĐỘNG ĐỔ ẢNH VÀ MÔ TẢ RA MÀN HÌNH CHÍNH =================
    public void renderSothebysItemDetails(JsonObject item) {
        if (item == null) return;

        currentSellerId = getStringSafe(item, "sellerId");
        currentSellerName = getStringSafe(item, "sellerFullName");
        if (currentSellerName.isBlank()) {
            currentSellerName = "Người bán";
        }
        updateSellerCard();

        // 1. Đổ Mô Tả
        showcaseDescLabel.setText(item.has("description") && !item.get("description").isJsonNull()
                ? item.get("description").getAsString() : "Chưa có mô tả chi tiết.");

        // 2. Load Ảnh & Xử lý khi "Không có ảnh"
        showcaseImagePaths.clear();
        currentShowcaseImageIndex = 0;
        showcaseImageView.setImage(null); // Reset ảnh cũ

        // Dùng tà thuật lấy cái khung StackPane bọc bên ngoài bức ảnh
        javafx.scene.layout.StackPane container = (javafx.scene.layout.StackPane) showcaseImageView.getParent();
        // Dọn dẹp mấy cái chữ "Không có ảnh" (nếu bị dính từ phiên xem trước đó)
        container.getChildren().removeIf(node -> "showcase-no-image".equals(node.getUserData()));

        if (item.has("images") && item.get("images").isJsonArray()) {
            for (JsonElement imageElement : item.getAsJsonArray("images")) {
                if (!imageElement.isJsonNull()) {
                    String imagePath = imageElement.getAsString();
                    if (imagePath != null && !imagePath.isBlank()) {
                        showcaseImagePaths.add(imagePath);
                    }
                }
            }
        }

        boolean hasImage = loadShowcaseImageAt(currentShowcaseImageIndex);
        updateShowcaseImageNavigation();
        renderShowcaseThumbnails();

        // Khi vào màn đấu giá trực tiếp, dùng chung danh sách ảnh này
        updateTradingImageViews();

        // Đóng dấu chữ "Không có ảnh" nếu load thất bại
        if (!hasImage) {
            Label noImgLabel = new Label("Không có hình ảnh");
            noImgLabel.setUserData("showcase-no-image");
            noImgLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 16px;");
            container.getChildren().add(noImgLabel);
        }

        // 3. Khôi phục toàn bộ thông số chi tiết (Loại, Tình trạng, Thuộc tính riêng)
        showcaseSpecsBox.getChildren().clear();

        // -- Thêm Loại --
        String type = item.has("type") && !item.get("type").isJsonNull() ? item.get("type").getAsString() : "Khác";
        addSpecRow("Phân loại", type);

        // -- Thêm Tình trạng --
        if (item.has("condition") && !item.get("condition").isJsonNull()) {
            String conditionText = item.get("condition").getAsString();
            if ("NEW".equals(conditionText)) conditionText = "Mới 100%";
            else if ("USED".equals(conditionText)) conditionText = "Đã sử dụng";
            addSpecRow("Tình trạng", conditionText);
        }

        // -- Thêm Thông số chuyên sâu (Tác giả, Hãng xe, Kích thước, Cân nặng...) --
        if (item.has("specifications") && item.get("specifications").isJsonObject()) {
            JsonObject specs = item.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                addSpecRow(key, specs.get(key).getAsString());
            }
        }
    }

    private void updateSellerCard() {
        if (sellerNameLabel != null) {
            sellerNameLabel.setText(currentSellerName);
        }
        if (sellerMetaLabel != null) {
            sellerMetaLabel.setText(currentSellerId == null || currentSellerId.isBlank()
                    ? "Người bán đã xác minh"
                    : "ID: " + currentSellerId + " • Người bán đã xác minh");
        }
        if (sellerAvatarLabel != null) {
            sellerAvatarLabel.setText(buildInitials(currentSellerName));
        }
    }

    @FXML
    private void handleViewSellerProfile() {
        if (currentSellerId == null || currentSellerId.isBlank()) {
            AuctionDetailItemDialog.showWarning(AuctionDetailController.class, "Chưa có dữ liệu người bán cho phiên này.");
            return;
        }

        showSellerProfileDialog();
        sellerProfileLoading = true;
        int requestVersion = ++sellerProfileRequestVersion;
        network.requestSellerProfile(currentSellerId);

        javafx.animation.PauseTransition timeout =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        timeout.setOnFinished(event -> {
            if (sellerProfileLoading && requestVersion == sellerProfileRequestVersion) {
                renderSellerProfileError("Chưa nhận được phản hồi từ server. Hãy khởi động lại server để nạp action GET_SELLER_PROFILE.");
            }
        });
        timeout.play();
    }

    private void showSellerProfileDialog() {
        sellerProfileDialog = new Dialog<>();
        sellerProfileDialog.setTitle("Hồ sơ người bán");

        try {
            sellerProfileDialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        VBox content = new VBox(18);
        content.getStyleClass().add("seller-profile-dialog");

        HBox header = new HBox(14);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("seller-profile-avatar");
        Label avatarText = new Label(buildInitials(currentSellerName));
        avatarText.getStyleClass().add("seller-profile-avatar-text");
        avatar.getChildren().add(avatarText);

        VBox headerText = new VBox(4);
        Label title = new Label("Hồ sơ người bán");
        title.getStyleClass().add("seller-profile-title");
        Label subtitle = new Label("Thông tin tổng quan và hiệu suất đấu giá");
        subtitle.getStyleClass().add("seller-profile-subtitle");
        headerText.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(avatar, headerText);

        VBox infoList = new VBox(0);
        infoList.getStyleClass().add("seller-profile-info-list");
        sellerProfileNameValue = new Label(currentSellerName);
        sellerProfileEmailValue = new Label("Đang tải...");
        infoList.getChildren().addAll(
                createProfileInfoRow("Họ và tên", sellerProfileNameValue),
                createProfileInfoRow("Email", sellerProfileEmailValue)
        );

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(12);
        statsGrid.setVgap(12);
        statsGrid.getColumnConstraints().addAll(
                createPercentColumn(50),
                createPercentColumn(50)
        );

        sellerProfileCreatedAuctionsValue = addProfileStat(statsGrid, 0, "Số phiên đã tạo");
        sellerProfileSuccessfulAuctionsValue = addProfileStat(statsGrid, 1, "Số phiên thành công");
        sellerProfileCanceledAuctionsValue = addProfileStat(statsGrid, 2, "Số phiên bị hủy");
        sellerProfileWonItemsValue = addProfileStat(statsGrid, 3, "Số chiến lợi phẩm");
        sellerProfileAuctionProductsValue = addProfileStat(statsGrid, 4, "Số sản phẩm đấu giá");

        content.getChildren().addAll(header, infoList, statsGrid);
        sellerProfileDialog.getDialogPane().setContent(content);
        sellerProfileDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        sellerProfileDialog.show();
    }

    private HBox createProfileInfoRow(String label, Label valueLabel) {
        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("seller-profile-info-row");

        Label keyLabel = new Label(label);
        keyLabel.getStyleClass().add("seller-profile-info-label");
        keyLabel.setMinWidth(120);
        keyLabel.setPrefWidth(120);

        valueLabel.getStyleClass().add("seller-profile-info-value");
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(keyLabel, valueLabel);
        return row;
    }

    private javafx.scene.layout.ColumnConstraints createPercentColumn(double percentWidth) {
        javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
        column.setPercentWidth(percentWidth);
        column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        return column;
    }

    private Label addProfileStat(GridPane grid, int index, String caption) {
        VBox statCard = new VBox(5);
        statCard.getStyleClass().add("seller-profile-stat-card");

        Label value = new Label("...");
        value.getStyleClass().add("seller-profile-stat-value");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("seller-profile-stat-caption");
        captionLabel.setWrapText(true);

        statCard.getChildren().addAll(value, captionLabel);
        GridPane.setColumnIndex(statCard, index % 2);
        GridPane.setRowIndex(statCard, index / 2);
        grid.getChildren().add(statCard);
        return value;
    }

    public void renderSellerProfile(JsonObject response) {
        if (sellerProfileDialog == null) {
            return;
        }

        sellerProfileLoading = false;
        String status = getStringSafe(response, "status");
        if (!"SUCCESS".equals(status)) {
            String message = getStringSafe(response, "message");
            renderSellerProfileError(message.isBlank() ? "Không tải được hồ sơ người bán." : message);
            return;
        }

        sellerProfileNameValue.setText(getStringOrFallback(response, "fullName", currentSellerName));
        sellerProfileEmailValue.setText(getStringOrFallback(response, "email", "Chưa có email"));
        sellerProfileCreatedAuctionsValue.setText(getIntString(response, "createdAuctions"));
        sellerProfileSuccessfulAuctionsValue.setText(getIntString(response, "successfulAuctions"));
        sellerProfileCanceledAuctionsValue.setText(getIntString(response, "canceledAuctions"));
        sellerProfileWonItemsValue.setText(getIntString(response, "wonItems"));
        sellerProfileAuctionProductsValue.setText(getIntString(response, "auctionProducts"));
    }

    public boolean isSellerProfileLoading() {
        return sellerProfileLoading;
    }

    public void renderSellerProfileError(String message) {
        sellerProfileLoading = false;
        if (sellerProfileEmailValue != null) {
            sellerProfileEmailValue.setText(message == null || message.isBlank()
                    ? "Không tải được hồ sơ người bán."
                    : message);
        }
        if (sellerProfileNameValue != null && (sellerProfileNameValue.getText() == null || sellerProfileNameValue.getText().isBlank())) {
            sellerProfileNameValue.setText(currentSellerName);
        }
        setProfileStats("-");
    }

    private void setProfileStats(String value) {
        sellerProfileCreatedAuctionsValue.setText(value);
        sellerProfileSuccessfulAuctionsValue.setText(value);
        sellerProfileCanceledAuctionsValue.setText(value);
        sellerProfileWonItemsValue.setText(value);
        sellerProfileAuctionProductsValue.setText(value);
    }

    private String getIntString(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? String.valueOf(json.get(key).getAsInt())
                : "0";
    }

    private String getStringOrFallback(JsonObject json, String key, String fallback) {
        String value = getStringSafe(json, key);
        return value.isBlank() ? fallback : value;
    }

    private String getStringSafe(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : "";
    }

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) {
            return "--";
        }

        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(java.util.Locale.ROOT);
    }

    @FXML
    private void showPreviousShowcaseImage() {
        if (showcaseImagePaths.size() <= 1) {
            return;
        }

        currentShowcaseImageIndex = (currentShowcaseImageIndex - 1 + showcaseImagePaths.size()) % showcaseImagePaths.size();
        loadShowcaseImageAt(currentShowcaseImageIndex);
        updateShowcaseImageNavigation();
        renderShowcaseThumbnails();
    }

    @FXML
    private void showNextShowcaseImage() {
        if (showcaseImagePaths.size() <= 1) {
            return;
        }

        currentShowcaseImageIndex = (currentShowcaseImageIndex + 1) % showcaseImagePaths.size();
        loadShowcaseImageAt(currentShowcaseImageIndex);
        updateShowcaseImageNavigation();
        renderShowcaseThumbnails();
    }

    private boolean loadShowcaseImageAt(int index) {
        javafx.scene.layout.StackPane container = (javafx.scene.layout.StackPane) showcaseImageView.getParent();
        container.getChildren().removeIf(node -> "showcase-no-image".equals(node.getUserData()));

        if (showcaseImagePaths.isEmpty() || index < 0 || index >= showcaseImagePaths.size()) {
            showcaseImageView.setImage(null);
            return false;
        }

        try {
            File file = new File(showcaseImagePaths.get(index));
            if (file.exists()) {
                showcaseImageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                return true;
            }
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh: " + e.getMessage());
        }

        showcaseImageView.setImage(null);
        return false;
    }

    private void updateShowcaseImageNavigation() {
        boolean hasMultipleImages = showcaseImagePaths.size() > 1;

        showcasePrevImageButton.setVisible(hasMultipleImages);
        showcasePrevImageButton.setManaged(hasMultipleImages);
        showcaseNextImageButton.setVisible(hasMultipleImages);
        showcaseNextImageButton.setManaged(hasMultipleImages);
        showcaseImageCounterLabel.setVisible(false);
        showcaseImageCounterLabel.setManaged(false);
        showcaseImageCounterLabel.setText("");
    }

    private void renderShowcaseThumbnails() {
        showcaseThumbnailsBox.getChildren().clear();

        for (int i = 0; i < showcaseImagePaths.size(); i++) {
            int imageIndex = i;
            StackPane thumbnail = new StackPane();
            thumbnail.getStyleClass().add(imageIndex == currentShowcaseImageIndex
                    ? "auction-detail-thumb-active"
                    : "auction-detail-thumb-muted");
            thumbnail.setOnMouseClicked(event -> {
                currentShowcaseImageIndex = imageIndex;
                loadShowcaseImageAt(currentShowcaseImageIndex);
                updateShowcaseImageNavigation();
                renderShowcaseThumbnails();
                updateTradingImageViews();
            });

            File file = new File(showcaseImagePaths.get(imageIndex));
            if (file.exists()) {
                ImageView thumbnailImage = new ImageView(new Image(file.toURI().toString()));
                thumbnailImage.setFitWidth(88);
                thumbnailImage.setFitHeight(76);
                thumbnailImage.setPreserveRatio(true);
                thumbnailImage.setSmooth(true);
                thumbnail.getChildren().add(thumbnailImage);
            } else {
                Label fallbackLabel = new Label(String.valueOf(imageIndex + 1));
                fallbackLabel.getStyleClass().add("auction-detail-thumb-label");
                thumbnail.getChildren().add(fallbackLabel);
            }

            showcaseThumbnailsBox.getChildren().add(thumbnail);
        }
    }

    // ================= HÀM PHỤ TRỢ: VẼ TỪNG DÒNG THÔNG SỐ SIÊU ĐẸP =================
    private void addSpecRow(String label, String value) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setSpacing(15);
        // Gạch chân mỏng dưới mỗi thông số
        row.setStyle("-fx-padding: 10 0; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

        Label lblKey = new Label(label);
        lblKey.setPrefWidth(150); // Cố định chiều rộng cột tên thông số cho thẳng hàng
        lblKey.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblValue.setWrapText(true);
        javafx.scene.layout.HBox.setHgrow(lblValue, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(lblKey, lblValue);
        showcaseSpecsBox.getChildren().add(row);
    }

    @FXML
    private void handleBid() {
        bidHandler.placeBid(bidAmountField.getText());
    }

    @FXML
    private void handleAutoBid() {
        bidHandler.registerAutoBid(maxBidField.getText().trim(), incrementField.getText().trim());
    }

    @FXML
    private void handleBack() {
        AppNavigator.navigate("Main.fxml");
    }
    // ================= XỬ LÝ THEO DÕI PHIÊN ĐẤU GIÁ =================
    @FXML
    private void handleFollowAuction() {
        if (state.currentAuctionId == null) return;

        boolean following = state.isFollowing;
        JsonObject request = new JsonObject();
        request.addProperty("action", following ? "UNFOLLOW_AUCTION" : "FOLLOW_AUCTION");
        request.addProperty("auctionId", state.currentAuctionId);
        com.bidding.network.NetworkClient.getInstance().sendJson(request);

        state.isFollowing = !following;
        ui.setFollowButtonState(state.isFollowing);
    }
}
