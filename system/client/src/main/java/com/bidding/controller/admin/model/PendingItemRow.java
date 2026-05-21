package com.bidding.controller.admin.model;

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

    public PendingItemRow(String id, String name, String seller, String type, double startingPrice) {
        this.id = id;
        this.name = name;
        this.seller = seller;
        this.type = type;
        this.startingPrice = startingPrice;
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
}
