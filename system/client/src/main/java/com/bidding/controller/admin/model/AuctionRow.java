package com.bidding.controller.admin.model;

import com.google.gson.JsonObject;

/**
 * Table row model for the admin auctions panel.
 */
public class AuctionRow {
    private final String id;
    private final String title;
    private final String status;
    private final double currentPrice;
    private final JsonObject itemJson;

    public AuctionRow(String id, String title, String status, double currentPrice, JsonObject itemJson) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.currentPrice = currentPrice;
        this.itemJson = itemJson;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getPriceFormatted() {
        return String.format("VNĐ %,.0f", currentPrice);
    }

    public JsonObject getItemJson() {
        return itemJson;
    }
}
