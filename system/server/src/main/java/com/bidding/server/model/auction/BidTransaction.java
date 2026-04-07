package com.bidding.server.model.auction;

import com.bidding.server.model.core.Entity;
import com.bidding.server.model.user.Bidder;
import com.bidding.server.enums.BidStatus;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private Bidder bidder;
    private Auction auction;
    private double amount;
    private LocalDateTime timestamp;
    private BidStatus status;
    private boolean isAutoBid;

    public BidTransaction() {
        super();
    }

    public BidTransaction(Bidder bidder, Auction auction, double amount) {
        super();
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.status = BidStatus.ACCEPTED;
        this.isAutoBid = false;
    }

    public BidTransaction(Bidder bidder, Auction auction, double amount, LocalDateTime localDateTime, BidStatus bidStatus, boolean isAutoBid){
        super();
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = localDateTime;
        this.status = bidStatus;
        this.isAutoBid = isAutoBid;
    }

    //gettter
    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public Auction getAuction() {
        return auction;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BidStatus getStatus() {
        return status;
    }

    //setter
    public void setAuction(Auction auction) {
        this.auction = auction;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }

    // tự động đấu giá
    public boolean isAutoBid() {
        return isAutoBid;
    }

    public void setAutoBid(boolean autoBid) {
        isAutoBid = autoBid;
    }

    // --- Core Operations ---
    public boolean isWinning() {
        if (this.auction == null || this.bidder == null) {
            return false;
        }
        return this.bidder.equals(this.auction.getCurrentWinner());
    }

    public void markOutbid() {
        this.status = BidStatus.OUTBID;
    }

    @Override
    public void printInfo() {
        System.out.println("Bid Transaction: " + amount + " by " + bidder.getUsername() + " on " + auction.getId());
    }

    @Override
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive.");
        }
        if (bidder == null || auction == null) {
            throw new IllegalArgumentException("Bidder and Auction must be set.");
        }
    }
}
