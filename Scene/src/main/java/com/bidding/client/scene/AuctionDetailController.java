package com.bidding.client.scene;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class AuctionDetailController {


    

    @FXML private BorderPane detailPage;
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
    @FXML private Label detailLblNoImg;
    @FXML private ImageView detailImgMain;
    @FXML private HBox detailImageThumbs;
    @FXML private VBox specsBox;
    @FXML private Button btnJoinBid;
    @FXML private Button btnWatchDetail;
    @FXML private Button btnImgPrev;
    @FXML private Button btnImgNext;
    @FXML private BidHistoryTableController bidHistoryController;

    private SceneBidder1.ProductData currentProduct;
    private Runnable onBackAction;
    private Consumer<Integer> onToggleWatchAction;
    private Function<Integer, Boolean> checkWatchStatusAction;




    
    @FXML
    public void initialize() {}

    public void setCallbacks(Runnable onBack, Consumer<Integer> onToggleWatch, Function<Integer, Boolean> checkWatchStatus) {
        this.onBackAction = onBack;
        this.onToggleWatchAction = onToggleWatch;
        this.checkWatchStatusAction = checkWatchStatus;
    }

    public void setProductData(SceneBidder1.ProductData product) {
        this.currentProduct = product;
        if (product == null) {
            return;
        }

        if (detailLblBreadcrumb != null) detailLblBreadcrumb.setText("Chi tiet phien dau gia");
        if (detailLblName != null) detailLblName.setText(product.name);
        if (detailLblCategory != null) detailLblCategory.setText(product.category);
        if (detailLblStartPrice != null) detailLblStartPrice.setText(formatPrice(product.startPrice) + " d");
        if (detailLblCurrentPrice != null) detailLblCurrentPrice.setText(formatPrice(product.currentPrice) + " d");
        if (detailLblDesc != null) detailLblDesc.setText(product.description);
        if (detailLblSeller != null) detailLblSeller.setText(product.seller);
        if (detailLblSellerRating != null) detailLblSellerRating.setText(product.sellerRating + " *");
        if (detailLblTopBidder != null) detailLblTopBidder.setText(maskBidder(product.topBidder));
        if (detailLblBidCount != null) detailLblBidCount.setText(String.valueOf(product.bidCount));
        if (detailLblParticipants != null) detailLblParticipants.setText(String.valueOf(product.participants));
        if (detailLblStartTime != null) detailLblStartTime.setText("Bat dau: " + product.startTime.toString().replace('T', ' '));
        if (detailLblEndTime != null) detailLblEndTime.setText("Ket thuc: " + product.endTime.toString().replace('T', ' '));
        if (detailLblStatus != null) detailLblStatus.setText(product.getStatusType());
        if (detailLblCountdown != null) detailLblCountdown.setText(formatRemaining(product.endTime));
        if (bidHistoryController != null) bidHistoryController.setBidHistory(product.bidHistory);

        updateWatchButtonState();
    }

    @FXML
    void goBackToMain(ActionEvent event) {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    @FXML
    void btnJoinBid(ActionEvent event) {
        // Chuyển hướng sang hàm handleJoinBid thống nhất logic
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
    void handleImgPrev(ActionEvent event) {
        if (detailLblMsg != null) {
            detailLblMsg.setVisible(true);
            detailLblMsg.setText("Tinh nang chuyen anh truoc dang cho du lieu hinh anh day du hon.");
        }
    }

    @FXML
    void handleImgNext(ActionEvent event) {
        if (detailLblMsg != null) {
            detailLblMsg.setVisible(true);
            detailLblMsg.setText("Tinh nang chuyen anh tiep theo dang cho du lieu hinh anh day du hon.");
        }
    }

    @FXML
    void handleJoinBid(ActionEvent event) {
        if (currentProduct == null) {
            return;
        }

        try {
            Stage stage = (Stage) detailPage.getScene().getWindow();
            Scene previousScene = stage.getScene();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("BiddingView.fxml"));
            Parent root = loader.load();

            BiddingViewController controller = loader.getController();
            controller.setAuctionData(currentProduct, stage, previousScene, () -> setProductData(currentProduct));

            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("BidViet - Phong dau gia truc tiep");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            if (detailLblMsg != null) {
                detailLblMsg.setVisible(true);
                detailLblMsg.setText("Khong mo duoc man hinh bidding. Can kiem tra lien ket BiddingView.fxml.");
            }
        }
    }

    private void updateWatchButtonState() {
        if (currentProduct == null || checkWatchStatusAction == null || btnWatchDetail == null) {
            return;
        }

        boolean isWatched = checkWatchStatusAction.apply(currentProduct.id);
        if (isWatched) {
            btnWatchDetail.setText("Bo theo doi");
            btnWatchDetail.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #1f2937;");
        } else {
            btnWatchDetail.setText("Theo doi");
            btnWatchDetail.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
        }
    }

    private String formatPrice(String priceStr) {
        try {
            long value = Long.parseLong(priceStr);
            return String.format("%,d", value).replace(',', '.');
        } catch (Exception e) {
            return priceStr;
        }
    }

    private String maskBidder(String bidder) {
        if (bidder == null || bidder.isBlank() || "â€”".equals(bidder)) {
            return "â€”";
        }
        return bidder.length() <= 3 ? bidder.charAt(0) + "***" : bidder.substring(0, 3) + "***";
    }

    private String formatRemaining(LocalDateTime endTime) {
        long seconds = Math.max(0L, ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime));
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
