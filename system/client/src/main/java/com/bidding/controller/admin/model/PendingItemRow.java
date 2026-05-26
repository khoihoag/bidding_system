package com.bidding.controller.admin.model;

import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Row model for items waiting for admin approval.
 */
public class PendingItemRow {
    private static final NumberFormat MONEY_FMT = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    private final String id;
    private final String name;
    private final String seller;
    private final String type;
    private final double startingPrice;
    private final JsonObject itemJson;

    public PendingItemRow(String id, String name, String seller, String type, double startingPrice, JsonObject itemJson) {
        this.id = id;
        this.name = name;
        this.seller = seller;
        this.type = type;
        this.startingPrice = startingPrice;
        this.itemJson = itemJson;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSeller() {
        return seller;
    }

    public String getType() {
        return type;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getStartingPriceFormatted() {
        return MONEY_FMT.format(startingPrice) + " d";
    }

    public JsonObject getItemJson() {
        return itemJson;
    }
}
