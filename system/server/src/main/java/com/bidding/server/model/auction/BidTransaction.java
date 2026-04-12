package com.bidding.server.model.auction;

import com.bidding.server.enums.BidStatus;
import com.bidding.server.model.core.Entity;
import com.bidding.server.model.user.Bidder;

import java.time.LocalDateTime;


public class BidTransaction extends Entity {



    // Sử dụng từ khóa 'final' cho các thuộc tính cốt lõi để đảm bảo tính bất biến (immutable)
    private final Bidder bidder;
    private final Auction auction;
    private final double amount;
    private final LocalDateTime timestamp;
    private final boolean isAutoBid;

    // Thuộc tính status không đặt final vì có phương thức markOutbid() thay đổi trạng thái này
    private BidStatus status;


    public BidTransaction(Bidder bidder, Auction auction, double amount,
                          LocalDateTime timestamp, BidStatus status, boolean isAutoBid) {
        // Lưu ý: Các thuộc tính id, createdAt của Entity sẽ được khởi tạo qua super()
        super();

        // Validate căn bản để chống dữ liệu rác
        if (bidder == null || auction == null) {
            throw new IllegalArgumentException("Bidder và Auction không được phép null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền đặt giá phải lớn hơn 0.");
        }

        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = status;
        this.isAutoBid = isAutoBid;
    }


    public double getAmount() {
        return this.amount;
    }

    /**
     * Kiểm tra bid này có đang dẫn đầu hay không.
     */
    public boolean isWinning() {
        // Trong ngữ cảnh giao dịch, một bid được coi là đang dẫn đầu
        // nếu trạng thái của nó vẫn là ACCEPTED (chưa bị vượt giá).
        return this.status == BidStatus.ACCEPTED;
    }

    /**
     * Đánh dấu giao dịch này đã bị vượt giá (outbid).
     */
    public void markOutbid() {
        if (this.status == BidStatus.ACCEPTED) {
            this.status = BidStatus.OUTBID;
        }
    }

    // ==========================================
    // PHƯƠNG THỨC OVERRIDE TỪ ENTITY
    // ==========================================

    @Override
    public void validate() {
        // Tương tự Constructor, kiểm tra tính hợp lệ của dữ liệu nội tại
        if (this.amount <= 0 || this.bidder == null || this.auction == null) {
            throw new IllegalStateException("Trạng thái BidTransaction không hợp lệ.");
        }
    }

    @Override
    public void printInfo() {
        System.out.println("=== CHI TIẾT GIAO DỊCH ĐẶT GIÁ ===");
        System.out.println("Giao dịch ID: " + this.getId());
        System.out.println("Phiên đấu giá ID: " + this.auction.getId());
        System.out.println("Người đặt (Bidder): " + this.bidder.getUsername());
        System.out.println("Số tiền (Amount): $" + this.amount);
        System.out.println("Thời gian (Timestamp): " + this.timestamp.toString());
        System.out.println("Loại đặt giá: " + (this.isAutoBid ? "Tự động (Auto-Bid)" : "Thủ công (Manual)"));
        System.out.println("Trạng thái (Status): " + this.status.name());
        System.out.println("==================================");
    }

    public Bidder getBidder() { return bidder; }
    public Auction getAuction() { return auction; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BidStatus getStatus() { return status; }
    public boolean isAutoBid() { return isAutoBid; }
}
