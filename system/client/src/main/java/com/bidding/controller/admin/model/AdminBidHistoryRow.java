package com.bidding.controller.admin.model;

/**
 * Table row model for per-auction bid history dialog.
 */
public class AdminBidHistoryRow {
    private final int index;
    private final String bidder;
    private final String email;
    private final String amount;
    private final String time;
    private final String type;
    private final String status;

    public AdminBidHistoryRow(int index, String bidder, String email, String amount,
                               String time, String type, String status) {
        this.index = index;
        this.bidder = bidder;
        this.email = email;
        this.amount = amount;
        this.time = time;
        this.type = type;
        this.status = status;
    }

    public int getIndex() {
        return index;
    }

    public String getBidder() {
        return bidder;
    }

    public String getEmail() {
        return email;
    }

    public String getAmount() {
        return amount;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
