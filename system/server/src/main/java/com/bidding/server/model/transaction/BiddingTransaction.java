package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.user.User; // <--- THÊM IMPORT NÀY
import com.bidding.server.enums.BidStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BiddingTransaction {

    private Long id;

    private User bidder; // <--- Đổi Bidder thành User

    private Auction auction;

    private Double bidAmount;
    private LocalDateTime bidTime;
    private BidStatus status;
    private boolean isAutoBid;

    // <--- Đổi tham số Bidder thành User ở Constructor
    public BiddingTransaction(User bidder, Auction auction, Double bidAmount, LocalDateTime bidTime, BidStatus status, boolean isAutoBid) {
        this.bidder = bidder;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.status = status;
        this.isAutoBid = isAutoBid;
    }
}