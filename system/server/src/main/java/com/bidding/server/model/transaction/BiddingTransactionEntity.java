package com.bidding.server.model.transaction;

import com.bidding.server.enums.BidStatus;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.user.User; // <--- THÊM IMPORT NÀY
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "bidding_transactions")
public class BiddingTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // <--- Đổi Bidder thành User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private AuctionEntity auction;

    @Column(name = "bid_amount", nullable = false)
    private Double bidAmount;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BidStatus status;

    @Column(name = "is_auto_bid", nullable = false)
    private boolean isAutoBid;
    @Column(name = "is_extended", nullable = false)
    private boolean isExtended;
    public BiddingTransactionEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }
    public AuctionEntity getAuction() { return auction; }
    public void setAuction(AuctionEntity auction) { this.auction = auction; }
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
