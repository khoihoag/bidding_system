package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.user.User;
import com.bidding.server.enums.BidStatus;
import java.time.LocalDateTime;

public class BiddingTransaction {

    private Long id;
    private User bidder;
    private Auction auction;
    private Double bidAmount;
    private LocalDateTime bidTime;
    private BidStatus status;

    // TÁCH BẠCH 2 NGHIỆP VỤ RÕ RÀNG
    private boolean isAutoBid;  // Do người đặt hay do Bot (AutoBidConfig) đặt?
    private boolean isExtended; // Cú bid này có làm gia hạn (Anti-snipe) không?

    // Cập nhật lại Constructor
    public BiddingTransaction() {
    }

    public BiddingTransaction(Long id, User bidder, Auction auction, Double bidAmount, LocalDateTime bidTime, BidStatus status, boolean isAutoBid, boolean isExtended) {
        this.id = id;
        this.bidder = bidder;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.status = status;
        this.isAutoBid = isAutoBid;
        this.isExtended = isExtended;
    }

    public BiddingTransaction(User bidder, Auction auction, Double bidAmount, LocalDateTime bidTime, BidStatus status, boolean isAutoBid, boolean isExtended) {
        this.bidder = bidder;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.status = status;
        this.isAutoBid = isAutoBid;
        this.isExtended = isExtended; // Gán thêm biến này
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }
    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }
    public Double getBidAmount() { return bidAmount; }
    public void setBidAmount(Double bidAmount) { this.bidAmount = bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
    public BidStatus getStatus() { return status; }
    public void setStatus(BidStatus status) { this.status = status; }
    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }
    public boolean isExtended() { return isExtended; }
    public void setExtended(boolean extended) { isExtended = extended; }
}
