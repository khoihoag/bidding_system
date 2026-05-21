package com.bidding.controller;

import com.bidding.controller.auctiondetail.*;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * FXML controller for auction detail. Delegates to helpers under
 * {@link com.bidding.controller.auctiondetail}.
 */
public class AuctionDetailController {

    @FXML private VBox autoBidContainer;
    @FXML private Label headerItemName;
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
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;

    private final AuctionDetailState state = new AuctionDetailState();
    private AuctionDetailNetworkGateway network;
    private AuctionDetailUiPresenter ui;
    private AuctionDetailChartBinder chartBinder;
    private AuctionDetailCountdown countdown;
    private AuctionDetailBidHandler bidHandler;
    private AuctionDetailResponseHandler responseHandler;

    @FXML
    public void initialize() {
        ui = new AuctionDetailUiPresenter(
                state, headerItemName, itemNameLabel, currentPriceLabel, statusBadgeHeader,
                currentWinnerLabel, totalBidsLabel, timeRemainingLabel, resultLabel, lastUpdateLabel,
                bidErrorLabel, autoBidErrorLabel, bidAmountField, maxBidField, incrementField,
                bidButton, autoBidButton, autoBidContainer);
        network = new AuctionDetailNetworkGateway();
        chartBinder = new AuctionDetailChartBinder(state, priceChart, ui);
        countdown = new AuctionDetailCountdown(
                state, timeRemainingLabel, bidButton, autoBidButton);
        bidHandler = new AuctionDetailBidHandler(state, network, ui);
        responseHandler = new AuctionDetailResponseHandler(
                AuctionDetailController.class, state, ui, chartBinder, countdown);

        chartBinder.setupChart();
        ui.applyInitialStyles();
        network.registerMessageHandler(responseHandler::handle);

        String auctionId = (String) AppNavigator.getData();
        if (auctionId != null) {
            System.out.println("Đã nhảy sang màn Chi Tiết. ID nhận được: " + auctionId);
            initData(auctionId);
        } else {
            System.err.println("Toang! Không nhận được ID phiên đấu giá nào cả.");
        }
    }

    public void initData(String auctionId) {
        state.currentAuctionId = auctionId;
        headerAuctionId.setText("ID: " + auctionId);
        network.requestAuctionHistory(auctionId);
        network.requestAuctions();
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

    @FXML
    private void handleViewAuctionItemDetails() {
        if (state.currentSelectedAuction == null || !state.currentSelectedAuction.has("item")) {
            AuctionDetailItemDialog.showWarning(AuctionDetailController.class,
                    "Không tìm thấy thông tin vật phẩm!");
            return;
        }
        JsonObject item = state.currentSelectedAuction.getAsJsonObject("item");
        AuctionDetailItemDialog.show(item, AuctionDetailController.class);
    }
}
