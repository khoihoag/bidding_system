package com.bidding.server.events;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.transaction.BidTransaction;
import java.time.LocalDateTime;

public class AuctionEvent {
    private EventType eventType;
    private Auction auction;
    private BidTransaction bidTransaction;
    private LocalDateTime timestamp;
    private String message;

    public AuctionEvent() {
    }

    public AuctionEvent(EventType eventType, Auction auction, BidTransaction bidTransaction, LocalDateTime timestamp, String message) {
        this.eventType = eventType;
        this.auction = auction;
        this.bidTransaction = bidTransaction;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BidTransaction getBidTransaction() {
        return bidTransaction;
    }

    public void setBidTransaction(BidTransaction bidTransaction) {
        this.bidTransaction = bidTransaction;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

}
