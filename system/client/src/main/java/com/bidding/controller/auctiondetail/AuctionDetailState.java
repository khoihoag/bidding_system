package com.bidding.controller.auctiondetail;

import com.google.gson.JsonObject;
import javafx.scene.chart.XYChart;

import java.time.LocalDateTime;

/**
 * Mutable session state for the auction detail screen.
 */
public class AuctionDetailState {

    public String currentAuctionId;
    public int totalBidCount;
    public JsonObject currentSelectedAuction;
    public XYChart.Series<String, Number> priceSeries;
    public LocalDateTime endTime;
}
