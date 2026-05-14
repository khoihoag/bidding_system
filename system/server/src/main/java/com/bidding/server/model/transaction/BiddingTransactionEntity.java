package com.bidding.server.model.transaction;

import com.bidding.server.enums.BidStatus;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.user.User; // <--- THÊM IMPORT NÀY
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}