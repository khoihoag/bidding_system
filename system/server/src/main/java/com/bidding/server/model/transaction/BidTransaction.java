package com.bidding.server.model.transaction;

import com.bidding.server.enums.BidStatus;
import com.bidding.server.model.core.Entity;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    // CHÚ Ý: Đã thay đổi từ Object (User/Auction) sang String ID
    // Điều này giúp Object siêu nhẹ, an toàn bảo mật khi serialize thành JSON gửi qua Socket
    private final String bidderId;
    private final String auctionId;
    private final double amount;
    private final LocalDateTime timestamp;
    private final boolean isAutoBid;

    // Thuộc tính status có thể bị thay đổi khi có người đặt giá cao hơn
    private BidStatus status;

    // =======================================================
    // VŨ KHÍ MỚI: BÁO CÁO ANTI-SNIPE (Cho Socket đọc)
    // =======================================================
    private boolean antiSnipeTriggered = false;
    private LocalDateTime newEndTime = null;

    public BidTransaction(String bidderId, String auctionId, double amount,
                          LocalDateTime timestamp, BidStatus status, boolean isAutoBid) {
        super(); // Kế thừa id, createdAt từ class Entity cha

        // Validate căn bản
        if (bidderId == null || auctionId == null) {
            throw new IllegalArgumentException("Bidder ID và Auction ID không được phép null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền đặt giá phải lớn hơn 0.");
        }

        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = status;
        this.isAutoBid = isAutoBid;
    }

    // --- CÁC HÀM NGHIỆP VỤ (DOMAIN LOGIC) ---

    public boolean isWinning() {
        return this.status == BidStatus.ACCEPTED;
    }

    public void markOutbid() {
        if (this.status == BidStatus.ACCEPTED) {
            this.status = BidStatus.OUTBID;
        }
    }

    // --- GETTERS (Dùng cho Socket và Database Mapper) ---
    public String getBidderId() { return bidderId; }
    public String getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BidStatus getStatus() { return status; }
    public boolean isAutoBid() { return isAutoBid; }

    // --- GETTERS & SETTERS CHO ANTI-SNIPE ---
    public boolean isAntiSnipeTriggered() { return antiSnipeTriggered; }
    public void setAntiSnipeTriggered(boolean antiSnipeTriggered) { this.antiSnipeTriggered = antiSnipeTriggered; }

    public LocalDateTime getNewEndTime() { return newEndTime; }
    public void setNewEndTime(LocalDateTime newEndTime) { this.newEndTime = newEndTime; }

    // ==========================================
    // PHƯƠNG THỨC OVERRIDE TỪ ENTITY
    // ==========================================

    @Override
    public void validate() {
        if (this.amount <= 0 || this.bidderId == null || this.auctionId == null) {
            throw new IllegalStateException("Trạng thái BidTransaction không hợp lệ.");
        }
    }

    @Override
    public void printInfo() {
        System.out.println("=== CHI TIẾT GIAO DỊCH ĐẶT GIÁ ===");
        System.out.println("Giao dịch ID: " + this.getId());
        System.out.println("Phiên đấu giá ID: " + this.auctionId);
        System.out.println("Người đặt ID: " + this.bidderId);
        System.out.println("Số tiền: $" + this.amount);
        System.out.println("Thời gian: " + this.timestamp.toString());
        System.out.println("Loại: " + (this.isAutoBid ? "Tự động (Auto-Bid)" : "Thủ công (Manual)"));
        System.out.println("Gia hạn giờ (Anti-Snipe): " + (this.antiSnipeTriggered ? "CÓ" : "KHÔNG"));
        System.out.println("Trạng thái: " + this.status.name());
        System.out.println("==================================");
    }
}