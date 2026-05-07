package com.bidding.server.events;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.transaction.BiddingTransaction;
import java.time.LocalDateTime;
import lombok.Setter;
import lombok.Getter;
@Getter
@Setter
public class AuctionEvent {
    private EventType eventType;
    private Auction auction;
    private BiddingTransaction biddingTransaction;
    private LocalDateTime timestamp;
    private String message;

    public AuctionEvent() {
    }

    public AuctionEvent(EventType eventType, Auction auction, BiddingTransaction biddingTransaction, LocalDateTime timestamp, String message) {
        this.eventType = eventType;
        this.auction = auction;
        this.biddingTransaction = biddingTransaction;
        this.timestamp = timestamp;
        this.message = message;
    }

    // =========================================================
    // CÁC HÀM TIỆN ÍCH (CONVENIENCE GETTERS) CHO WEBSOCKET/JSON
    // =========================================================

    public String getAuctionId() {
        if (this.auction != null && this.auction.getId() != null) {
            // Dùng String.valueOf() để bọc lại.
            // Dù ID ở Database là Long hay String (UUID) thì xuất ra JSON vẫn an toàn 100%.
            return String.valueOf(this.auction.getId());
        }
        return null;
    }

    public Double getNewPrice() {
        if (this.biddingTransaction != null && this.biddingTransaction.getBidAmount() != null) {
            return this.biddingTransaction.getBidAmount();
        }
        if (this.auction != null && this.auction.getCurrentPrice() != null) {
            // Vì currentPrice trên RAM Model là AtomicReference<Double>
            return this.auction.getCurrentPrice().get();
        }
        return 0.0;
    }

    public String getWinnerId() {
        // Đã gộp Bidder và Seller thành User.
        // Giả sử trong BiddingTransaction, biến chứa người đặt giá ông vẫn giữ tên là 'bidder' (nhưng type là User)
        if (this.biddingTransaction != null && this.biddingTransaction.getBidder() != null) {
            return String.valueOf(this.biddingTransaction.getBidder().getId());
        }
        return null;
    }
    // ... code cũ của ông (getWinnerId) ...

    public String getTimestampStr() {
        if (this.timestamp != null) {
            return this.timestamp.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        // Backup an toàn lỡ timestamp bị null
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

