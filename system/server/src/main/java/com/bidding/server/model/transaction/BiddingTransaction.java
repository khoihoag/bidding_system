package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.user.User;
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
    private User bidder;
    private Auction auction;
    private Double bidAmount;
    private LocalDateTime bidTime;
    private BidStatus status;

    // TÁCH BẠCH 2 NGHIỆP VỤ RÕ RÀNG
    private boolean isAutoBid;  // Do người đặt hay do Bot (AutoBidConfig) đặt?
    private boolean isExtended; // Cú bid này có làm gia hạn (Anti-snipe) không?

    // Cập nhật lại Constructor
    public BiddingTransaction(User bidder, Auction auction, Double bidAmount, LocalDateTime bidTime, BidStatus status, boolean isAutoBid, boolean isExtended) {
        this.bidder = bidder;
        this.auction = auction;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.status = status;
        this.isAutoBid = isAutoBid;
        this.isExtended = isExtended; // Gán thêm biến này
    }
}