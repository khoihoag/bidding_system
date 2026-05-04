package com.bidding.client.scene;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * AuctionDetailController - Màn hình chi tiết phiên đấu giá (Bidder)
 */
public class AuctionDetailController {

    @FXML private BorderPane detailPage;

    // Labels
    @FXML private Label detailLblBreadcrumb;
    @FXML private Label detailLblName;
    @FXML private Label detailLblStatus;
    @FXML private Label detailLblCategory;
    @FXML private Label detailLblStartPrice;
    @FXML private Label detailLblCurrentPrice;
    @FXML private Label detailLblCountdown;
    @FXML private Label detailLblStartTime;
    @FXML private Label detailLblEndTime;
    @FXML private Label detailLblBidCount;
    @FXML private Label detailLblTopBidder;
    @FXML private Label detailLblParticipants;
    @FXML private Label detailLblDesc;
    @FXML private Label detailLblSeller;
    @FXML private Label detailLblSellerRating;
    @FXML private Label detailLblMsg;

    @FXML private ImageView detailImgMain;
    @FXML private HBox detailImageThumbs;
    @FXML private VBox specsBox;

    @FXML private Button btnJoinBid;
    @FXML private Button btnWatchDetail;

    @FXML private BidHistoryTableController bidHistoryController;

    private SceneBidder1.ProductData currentProduct;
    private Timeline countdownTimeline;

    private Runnable onBackAction;
    private Consumer<Integer> onToggleWatchAction;
    private Function<Integer, Boolean> checkWatchStatusAction;

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ====================== INITIALIZE ======================
    @FXML
    public void initialize() {
        // Có thể thêm logic khởi tạo nếu cần
    }

    // ====================== CALLBACKS ======================
    public void setCallbacks(Runnable onBack, Consumer<Integer> onToggleWatch,
                             Function<Integer, Boolean> checkWatchStatus) {
        this.onBackAction = onBack;
        this.onToggleWatchAction = onToggleWatch;
        this.checkWatchStatusAction = checkWatchStatus;
    }

    // ====================== SET DATA CHÍNH ======================
    public void setProductData(SceneBidder1.ProductData product) {
        if (product == null) return;

        this.currentProduct = product;

        updateBasicInfo(product);
        startLiveCountdown(product.endTime);           // endTime là LocalDateTime

        updateWatchButtonState();
        updateJoinBidButtonState();

        if (bidHistoryController != null) {
            bidHistoryController.setBidHistory(product.bidHistory);
        }
    }

    private void updateBasicInfo(SceneBidder1.ProductData p) {
        setTextSafe(detailLblBreadcrumb, "Chi tiết phiên đấu giá");
        setTextSafe(detailLblName, p.name);
        setTextSafe(detailLblCategory, p.category != null ? p.category : "Chưa phân loại");
        setTextSafe(detailLblStartPrice, formatPrice(p.startPrice) + " ₫");
        setTextSafe(detailLblCurrentPrice, formatPrice(p.currentPrice) + " ₫");
        setTextSafe(detailLblDesc, p.description);
        setTextSafe(detailLblSeller, p.seller);
        setTextSafe(detailLblSellerRating, (p.sellerRating > 0 ? p.sellerRating : 0) + " ★");
        setTextSafe(detailLblTopBidder, maskBidder(p.topBidder));
        setTextSafe(detailLblBidCount, String.valueOf(p.bidCount));
        setTextSafe(detailLblParticipants, String.valueOf(p.participants));

        setTextSafe(detailLblStartTime, "Bắt đầu: " + formatDateTime(p.startTime));
        setTextSafe(detailLblEndTime,   "Kết thúc: " + formatDateTime(p.endTime));
        setTextSafe(detailLblStatus, p.getStatusType() != null ? p.getStatusType() : "Đang diễn ra");
    }

    // ====================== LIVE COUNTDOWN ======================
    private void startLiveCountdown(LocalDateTime endTime) {
        stopCountdown();

        if (endTime == null) {
            setTextSafe(detailLblCountdown, "Không có thời gian");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateCountdownDisplay(endTime);
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownDisplay(LocalDateTime endTime) {
        if (detailLblCountdown == null) return;

        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

        if (secondsLeft <= 0) {
            detailLblCountdown.setText("ĐÃ KẾT THÚC");
            detailLblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 18px;");
            stopCountdown();
            return;
        }

        long hours = secondsLeft / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;

        String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        detailLblCountdown.setText(timeStr);

        // Đổi màu khi sắp hết giờ
        if (secondsLeft <= 300) { // 5 phút cuối
            detailLblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            detailLblCountdown.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    // ====================== UTILITY METHODS ======================
    private String formatPrice(String priceStr) {
        try {
            long value = Long.parseLong(priceStr.replaceAll("[^0-9]", ""));
            return String.format("%,d", value).replace(',', '.');
        } catch (Exception e) {
            return priceStr != null ? priceStr : "0";
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Chưa xác định";
        return dateTime.format(DISPLAY_FORMATTER);
    }

    private String maskBidder(String bidder) {
        if (bidder == null || bidder.isBlank() || bidder.contains("—") || "â€”".equals(bidder)) {
            return "—";
        }
        return bidder.length() <= 3 ? bidder.charAt(0) + "***" : bidder.substring(0, 3) + "***";
    }

    private void setTextSafe(Label label, String text) {
        if (label != null) {
            label.setText(text != null ? text : "");
        }
    }

    private void updateWatchButtonState() {
        if (currentProduct == null || btnWatchDetail == null || checkWatchStatusAction == null) return;

        boolean isWatched = checkWatchStatusAction.apply(currentProduct.id);
        btnWatchDetail.setText(isWatched ? "Bỏ theo dõi" : "Theo dõi");
        btnWatchDetail.setStyle(isWatched
                ? "-fx-background-color: #f3f4f6; -fx-text-fill: #1f2937;"
                : "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
    }

    private void updateJoinBidButtonState() {
        if (btnJoinBid == null || currentProduct == null) return;
        boolean isEnded = isAuctionEnded(currentProduct.endTime);
        btnJoinBid.setDisable(isEnded);
    }

    private boolean isAuctionEnded(LocalDateTime endTime) {
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    // ====================== FXML HANDLERS ======================
    @FXML
    void goBackToMain(ActionEvent event) {
        stopCountdown();
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    @FXML
    void btnJoinBid(ActionEvent event) throws Exception {
        handleJoinBid(event);
    }

    @FXML
    void handleToggleWatchFromDetail(ActionEvent event) {
        if (currentProduct != null && onToggleWatchAction != null) {
            onToggleWatchAction.accept(currentProduct.id);
            updateWatchButtonState();
        }
    }

    @FXML
    void handleJoinBid(ActionEvent event) throws Exception {
        if (currentProduct == null) return;

        try {
            Stage stage = (Stage) detailPage.getScene().getWindow();
            Scene previousScene = stage.getScene();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("BiddingView.fxml"));
            Parent root = loader.load();

            BiddingViewController controller = loader.getController();
            controller.setAuctionData(currentProduct, stage, previousScene,
                    () -> setProductData(currentProduct)); // refresh sau khi bid

            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("BidViet - Phòng đấu giá trực tiếp");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            showMessage("Không thể mở phòng đấu giá. Vui lòng thử lại sau!");
            e.printStackTrace();
        }
    }

    @FXML
    void handleImgPrev(ActionEvent event) {
        showMessage("Tính năng chuyển ảnh đang được phát triển.");
    }

    @FXML
    void handleImgNext(ActionEvent event) {
        showMessage("Tính năng chuyển ảnh đang được phát triển.");
    }

    private void showMessage(String msg) {
        if (detailLblMsg != null) {
            detailLblMsg.setVisible(true);
            detailLblMsg.setText(msg);
        }
    }

    // ====================== CLEANUP ======================
    public void cleanup() {
        stopCountdown();
    }
}