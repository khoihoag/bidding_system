package com.bidding.server.model.auction;

import com.bidding.server.model.user.Bidder; // Giả định package
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho cấu hình đấu giá tự động của một Bidder.
 */
@Getter // Sử dụng triệt để Lombok để clean code
public class AutoBidConfig implements Comparable<AutoBidConfig> {

    private final String auctionId;      // Bắt buộc: Cấu hình này thuộc về phiên nào?
    private final Bidder bidder;         // Bắt buộc: Ai là người cài đặt cấu hình này?
    private final double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;
    private boolean isActive;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public AutoBidConfig(String auctionId, Bidder bidder, double maxBid, double increment) {
        if (auctionId == null || bidder == null) {
            throw new IllegalArgumentException("Auction ID và Bidder không được để trống.");
        }
        if (maxBid <= 0 || increment <= 0) {
            throw new IllegalArgumentException("Giá tối đa và bước giá phải lớn hơn 0.");
        }

        this.auctionId = auctionId;
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
        this.isActive = true;
    }

    // ==========================================
    // BUSINESS METHODS
    // ==========================================

    /**
     * Tính toán mức giá đặt tiếp theo dựa trên giá hiện tại.
     */
    public double getNextBidAmount(double currentPrice) {
        return currentPrice + this.increment;
    }

    /**
     * Kiểm tra khả năng đặt giá tiếp.
     * @param currentPrice Giá cao nhất hiện tại của phiên
     * @param currentWinnerId ID của người đang dẫn đầu (tránh tự đấu giá)
     */
    public boolean canBid(double currentPrice, String currentWinnerId) {
        if (!this.isActive) return false;

        // Ngăn chặn tự đẩy giá nếu user này đang là người dẫn đầu
        if (this.bidder.getId().equals(currentWinnerId)) {
            return false;
        }

        return getNextBidAmount(currentPrice) <= this.maxBid;
    }

    public void deactivate() {
        this.isActive = false;
    }

    @Override
    public int compareTo(AutoBidConfig other) {
        return this.registeredAt.compareTo(other.registeredAt);
    }
}