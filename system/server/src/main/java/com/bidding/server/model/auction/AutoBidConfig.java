package com.bidding.server.model.auction;
import com.bidding.server.model.user.User;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho cấu hình đấu giá tự động của một Bidder.
 * Implements Comparable để có thể sắp xếp trong PriorityQueue của AuctionManager.
 */
public class AutoBidConfig implements Comparable<AutoBidConfig> {

    private User bidder;
    private double maxBid;           // Giá tối đa cho phép đặt tự động
    private double increment;        // Bước giá mỗi lần tự động đặt
    private LocalDateTime registeredAt; // Thời điểm đăng ký
    private boolean isActive;        // Cấu hình còn hiệu lực không

    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public AutoBidConfig(double maxBid, double increment) {
        this(null, maxBid, increment);
    }

    public AutoBidConfig(User bidder,double maxBid, double increment) {
        if (maxBid <= 0 || increment <= 0) {
            throw new IllegalArgumentException("Giá tối đa và bước giá phải lớn hơn 0.");
        }
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now(); // Ghi nhận ngay thời điểm tạo
        this.isActive = true; // Mặc định kích hoạt khi vừa tạo
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
     * Kiểm tra xem cấu hình này còn khả năng đặt giá tiếp hay không.
     * Điều kiện: Cấu hình đang active VÀ (giá hiện tại + bước giá) không vượt quá maxBid.
     */
    public boolean canBid(double currentPrice) {
        return this.isActive && (getNextBidAmount(currentPrice) <= this.maxBid);
    }

    /**
     * Vô hiệu hóa cấu hình đấu giá tự động.
     */
    public void deactivate() {
        this.isActive = false;
    }

    // ==========================================
    // GIAO TIẾP SO SÁNH (DÀNH CHO PRIORITY QUEUE)
    // ==========================================

    /**
     * Ghi đè phương thức compareTo để PriorityBlockingQueue biết cách sắp xếp.
     * Theo tài liệu: "ưu tiên khi hòa" dựa vào registeredAt.
     */
    @Override
    public int compareTo(AutoBidConfig other) {
        // Ai đăng ký trước (thời gian nhỏ hơn) sẽ được ưu tiên đứng trước trong hàng đợi
        return this.registeredAt.compareTo(other.registeredAt);
    }

    // ==========================================
    // GETTERS
    // ==========================================
    public User getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public boolean isActive() { return isActive; }
}
