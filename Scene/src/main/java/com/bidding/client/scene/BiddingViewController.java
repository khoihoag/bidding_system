package com.bidding.client.scene;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;

public class BiddingViewController {

    @FXML private Label lblProductName;
    @FXML private Label lblAuctionId;
    @FXML private Label lblOnlineViewers;
    @FXML private Label lblSessionStatus;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblCurrentLeader;
    @FXML private Label lblCountdown;
    @FXML private ProgressBar progressTime;
    @FXML private Label lblEndTime;
    @FXML private Label lblAntiSnipe;
    @FXML private TextField txtBidAmount;
    @FXML private Label lblMinBid;
    @FXML private Button btnQuick1;
    @FXML private Button btnQuick2;
    @FXML private Button btnQuick3;
    @FXML private Button btnTabFeed;
    @FXML private Button btnTabChart;
    @FXML private VBox tabFeedContent;
    @FXML private VBox tabChartContent;
    @FXML private Label lblWsStatus;
    @FXML private Label lblInfoName;
    @FXML private Label lblStartPrice;
    @FXML private Label lblBidStep;
    @FXML private Label lblSellerName;
    @FXML private Label lblTotalBids;
    @FXML private Label lblMyHighestBid;
    @FXML private Label lblMyStatus;
    @FXML private HBox hboxBidLoading;
    @FXML private VBox vboxBidResult;
    @FXML private Label lblBidResultIcon;
    @FXML private Label lblBidResultMessage;
    @FXML private Label lblBidValidation;
    @FXML private Hyperlink linkAutoBid;
    @FXML private LineChart<String, Number> bidChart;

    @FXML private AutoBidPanelController autoBidPanelController;
    @FXML private LiveBidFeedController liveBidFeedController;
    @FXML private BidResultOverlayController resultOverlayController;

    private final NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private SceneBidder1.ProductData currentProduct;
    private Stage hostStage;
    private Scene previousScene;
    private Runnable beforeBackAction;
    private Timeline countdownTimeline;
    private XYChart.Series<String, Number> priceSeries;

    private long currentPrice;
    private long myHighestBid;
    private long bidStep = 100_000L;
    private long initialDurationSeconds = 1L;
    private boolean resultOverlayShown = false;

    @FXML
    public void initialize() {
        txtBidAmount.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9]*")) {
                txtBidAmount.setText(newValue.replaceAll("[^0-9]", ""));
            }
        });

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Gia dat");
        bidChart.setData(FXCollections.observableArrayList(priceSeries));

        hideBidValidation();
        hideBidLoading();
        hideBidResult();
        showFeedTab();
    }

    public void setAuctionData(SceneBidder1.ProductData product, Stage stage, Scene oldScene, Runnable onBackRefresh) {
        currentProduct = product;
        hostStage = stage;
        previousScene = oldScene;
        beforeBackAction = onBackRefresh;

        if (product == null) {
            return;
        }

        currentPrice = safeParseLong(product.currentPrice);
        myHighestBid = 0L;
        resultOverlayShown = false;

        lblProductName.setText(product.name);
        lblAuctionId.setText("Phien #" + (product.auctionId == null || product.auctionId.isBlank() ? product.id : product.auctionId));
        lblOnlineViewers.setText(Math.max(product.participants, 1) + " dang xem");
        lblCurrentPrice.setText(nf.format(currentPrice) + " VND");
        lblCurrentLeader.setText(product.topBidder == null || product.topBidder.isBlank() ? "---" : maskBidder(product.topBidder));
        lblEndTime.setText("Ket thuc: " + product.endTime.format(timeFmt));
        lblInfoName.setText(product.name);
        lblStartPrice.setText(nf.format(safeParseLong(product.startPrice)) + " VND");
        lblBidStep.setText(nf.format(bidStep) + " VND");
        lblSellerName.setText(product.seller);
        lblTotalBids.setText(String.valueOf(product.bidCount));
        lblMyHighestBid.setText("Chua dat gia");
        lblMyStatus.setText("Dang theo doi");
        lblWsStatus.setText("Da ket noi gia lap");
        lblMinBid.setText("Gia toi thieu: " + nf.format(currentPrice + bidStep) + " VND");

        autoBidPanelController.setCurrentPrice(currentPrice);
        autoBidPanelController.setAuctionContext(product.auctionId, product.id);
        seedBidHistory();
        startCountdown();
        updateSessionStatus();
    }

    @FXML
    private void handleGoBack() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (beforeBackAction != null) {
            beforeBackAction.run();
        }
        if (hostStage != null && previousScene != null) {
            hostStage.setScene(previousScene);
            hostStage.show();
        }
    }

    @FXML
    private void handleQuickBid1() {
        applyQuickBid(100_000L);
    }

    @FXML
    private void handleQuickBid2() {
        applyQuickBid(500_000L);
    }

    @FXML
    private void handleQuickBid3() {
        applyQuickBid(1_000_000L);
    }

    @FXML
    private void handlePlaceBid() {
        if (currentProduct == null) {
            return;
        }

        String bidText = txtBidAmount.getText() == null ? "" : txtBidAmount.getText().trim();
        if (bidText.isEmpty()) {
            showBidValidation("Vui long nhap so tien truoc khi dat gia.");
            return;
        }

        long bidAmount;
        try {
            bidAmount = Long.parseLong(bidText);
        } catch (NumberFormatException exception) {
            showBidValidation("So tien dat gia khong hop le.");
            return;
        }

        long minimumAccepted = currentPrice + bidStep;
        if (bidAmount < minimumAccepted) {
            showBidValidation("Gia dat phai tu " + nf.format(minimumAccepted) + " VND tro len.");
            return;
        }

        hideBidValidation();
        showBidLoading();

        String jsonBid;
        if (currentProduct.auctionId != null && !currentProduct.auctionId.isBlank()) {
            jsonBid = String.format(
                    "{\"action\": \"BID\", \"auctionId\": \"%s\", \"amount\": %d}",
                    currentProduct.auctionId,
                    bidAmount
            );
        } else {
            jsonBid = String.format(
                    "{\"action\": \"BID\", \"itemId\": %d, \"amount\": %d}",
                    currentProduct.id,
                    bidAmount
            );
        }
        NetworkClient.send(jsonBid);

    }

    @FXML
    private void handleOpenAutoBid() {
        showBidResult(true, "Panel Auto-Bid dang nam ben phai. Ban co the bat / tat va luu maxBid tai do.");
    }

    @FXML
    private void handleTabFeed() {
        showFeedTab();
    }

    @FXML
    private void handleTabChart() {
        tabFeedContent.setVisible(false);
        tabFeedContent.setManaged(false);
        tabChartContent.setVisible(true);
        tabChartContent.setManaged(true);
        btnTabFeed.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0 0 2 0; -fx-text-fill: #9E9E9E; -fx-padding: 12 20 10 20; -fx-cursor: hand;");
        btnTabChart.setStyle("-fx-background-color: transparent; -fx-border-color: #1565C0; -fx-border-width: 0 0 2 0; -fx-font-weight: bold; -fx-text-fill: #1565C0; -fx-padding: 12 20 10 20; -fx-cursor: hand;");
    }

    private void showFeedTab() {
        tabFeedContent.setVisible(true);
        tabFeedContent.setManaged(true);
        tabChartContent.setVisible(false);
        tabChartContent.setManaged(false);
        btnTabFeed.setStyle("-fx-background-color: transparent; -fx-border-color: #1565C0; -fx-border-width: 0 0 2 0; -fx-font-weight: bold; -fx-text-fill: #1565C0; -fx-padding: 12 20 10 20; -fx-cursor: hand;");
        btnTabChart.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0 0 2 0; -fx-text-fill: #9E9E9E; -fx-padding: 12 20 10 20; -fx-cursor: hand;");
    }

    private void applyQuickBid(long increment) {
        txtBidAmount.setText(String.valueOf(currentPrice + increment));
    }

    private void seedBidHistory() {
        liveBidFeedController.clearFeed();
        priceSeries.getData().clear();

        if (currentProduct.bidHistory.isEmpty()) {
            addChartPoint(currentPrice);
            return;
        }

        for (int index = currentProduct.bidHistory.size() - 1; index >= 0; index--) {
            SceneBidder1.BidEntry entry = currentProduct.bidHistory.get(index);
            addChartPoint(entry.price);
        }

        for (SceneBidder1.BidEntry entry : currentProduct.bidHistory) {
            boolean isLeader = entry.price == currentPrice;
            liveBidFeedController.addRow(maskBidder(entry.bidder), entry.price, entry.time.format(timeFmt), isLeader);
        }
    }

    private void addChartPoint(long amount) {
        priceSeries.getData().add(new XYChart.Data<>(LocalDateTime.now().format(timeFmt), amount));
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        initialDurationSeconds = Math.max(1L, ChronoUnit.SECONDS.between(LocalDateTime.now(), currentProduct.endTime));
        countdownTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> refreshCountdown()), new KeyFrame(Duration.seconds(1)));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void handleAuctionEnd(JsonObject response) {

        countdownTimeline.stop();
        lblSessionStatus.setText("DA KET THUC");

        btnQuick1.setDisable(true);
        btnQuick2.setDisable(true);
        btnQuick3.setDisable(true);
        txtBidAmount.setDisable(true);

        
        long finalPrice = response.get("finalPrice").getAsLong();
        String winner = response.get("winner").getAsString();
        boolean isWinner = Scene1.myname.equals(winner); // So sánh với user hiện tại
        
        resultOverlayController.show(isWinner, currentProduct.name, finalPrice);
}
    public void setupNetworkListeners() {
        // 1. Nghe phản hồi khi đặt giá (BID_REPLY)
        // Lưu ý: TV2 cần cung cấp bidListener trong NetworkClient, 
        // Nếu chưa có, bạn có thể tạm dùng code xử lý chung ở NetworkClient.java 
        
        // 2. Quan trọng nhất: Nghe loa phường (UPDATE_PRICE) 
        // Để khi ai đó (kể cả bạn) đặt giá, màn hình sẽ nhảy số
        NetworkClient.itemsListener = (response) -> {
            String action = response.get("action").getAsString();
            
            if ("UPDATE_PRICE".equals(action) || "NEW_BID".equals(action)) {
                Platform.runLater(() -> {
                    String auctionId = response.has("auctionId") ? response.get("auctionId").getAsString() : currentProduct.auctionId;
                    int itemId = response.has("itemId") ? response.get("itemId").getAsInt() : currentProduct.id;
                    long newPrice = response.get("newPrice").getAsLong();
                    String leader = response.has("winnerId") ? response.get("winnerId").getAsString() : response.get("highestBidder").getAsString();

                    boolean sameAuction = (currentProduct.auctionId != null && !currentProduct.auctionId.isBlank() && currentProduct.auctionId.equals(auctionId))
                            || currentProduct.id == itemId;
                    if (!sameAuction) {
                        return;
                    }
                     
                    lblCurrentPrice.setText(nf.format(newPrice) + " VND");
                    lblCurrentLeader.setText(maskBidder(leader));
                    lblMinBid.setText("Gia toi thieu: " + nf.format(newPrice + bidStep) + " VND");
                    currentPrice = newPrice;
                    currentProduct.currentPrice = String.valueOf(newPrice);
                    currentProduct.topBidder = leader;
                     
                    addChartPoint(newPrice);
                    liveBidFeedController.addRow(maskBidder(leader), newPrice, LocalDateTime.now().format(timeFmt), leader.equals(Scene1.myname));

                    autoBidPanelController.evaluateAndBid(currentProduct.auctionId, itemId, newPrice, leader);
                     
                    hideBidLoading();
                });
            } 
            else if ("AUCTION_END".equals(action)) {
                Platform.runLater(() -> {

                    handleAuctionEnd(response);
                });
            }
        };
    }

    private void refreshCountdown() {
        long remaining = Math.max(0L, ChronoUnit.SECONDS.between(LocalDateTime.now(), currentProduct.endTime));
        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;

        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        progressTime.setProgress(Math.max(0, (double) remaining / initialDurationSeconds));

        if (remaining <= 60) {
            lblSessionStatus.setText("SAP KET THUC");
            lblSessionStatus.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        if (remaining == 0) {
            countdownTimeline.stop();
            lblSessionStatus.setText("DA KET THUC");
            lblSessionStatus.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
            btnQuick1.setDisable(true);
            btnQuick2.setDisable(true);
            btnQuick3.setDisable(true);
            txtBidAmount.setDisable(true);
            // Lưu ý: Nên để handleAuctionEnd từ server điều khiển Overlay để chính xác hơn về kết quả DB
            System.out.println("Timer ended locally, waiting for server AUCTION_END...");
        }
    }

    private void updateSessionStatus() {
        lblSessionStatus.setText("DANG DIEN RA");
        lblSessionStatus.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void showBidValidation(String message) {
        lblBidValidation.setText(message);
        lblBidValidation.setVisible(true);
        lblBidValidation.setManaged(true);
    }

    private void hideBidValidation() {
        lblBidValidation.setVisible(false);
        lblBidValidation.setManaged(false);
    }

    private void showBidLoading() {
        hboxBidLoading.setVisible(true);
        hboxBidLoading.setManaged(true);
    }

    private void hideBidLoading() {
        hboxBidLoading.setVisible(false);
        hboxBidLoading.setManaged(false);
    }

    private void showBidResult(boolean success, String message) {
        vboxBidResult.setVisible(true);
        vboxBidResult.setManaged(true);
        vboxBidResult.setStyle("-fx-background-color: " + (success ? "#E8F5E9" : "#FFEBEE") + "; -fx-background-radius: 4; -fx-padding: 10;");
        lblBidResultIcon.setText(success ? "âœ“" : "!");
        lblBidResultMessage.setText(message);
        lblBidResultMessage.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (success ? "#2E7D32" : "#D32F2F") + ";");

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> hideBidResult());
        pause.play();
    }

    private void hideBidResult() {
        vboxBidResult.setVisible(false);
        vboxBidResult.setManaged(false);
    }

    private long safeParseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception exception) {
            return 0L;
        }
    }

    private String maskBidder(String bidder) {
        if (bidder == null || bidder.isBlank()) {
            return "---";
        }
        if (bidder.length() <= 3) {
            return bidder.charAt(0) + "***";
        }
        return bidder.substring(0, 3) + "***";
    }
    
}
