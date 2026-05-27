package com.bidding.server.model.auction;
import com.bidding.server.model.user.User;
import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.model.core.Entity; // Class ông nội chứa @Id và @MappedSuperclass
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "auctions")
public class AuctionEntity extends Entity {
    private boolean isReverse = false; // Đánh dấu đây là đấu giá ngược
    private double dropStep = 0.0;

    // Một sản phẩm có thể mở lại phiên đấu giá sau khi phiên trước bị hủy/thất bại.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Ép Hibernate lưu Enum dưới dạng chữ (vd: "OPEN", "RUNNING") thay vì số 0, 1
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuctionStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // Đã lột bỏ AtomicReference, chỉ giữ lại Double thuần túy cho DB dễ hiểu
    @Column(name = "current_price")
    private Double currentPrice = 0.0;

    // Nhiều phiên đấu giá có thể có chung 1 người thắng (1 User thắng nhiều giải)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User currentWinner;

    // cascade = ALL: Lưu Auction thì tự động lưu luôn đống lịch sử Bid bên trong
    // orphanRemoval = true: Xóa lịch sử nào ra khỏi List là DB tự động xóa dòng đó
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BiddingTransactionEntity> transactions;

    @Column(name = "anti_sniping_seconds")
    private int antiSnipingSeconds;

    @Column(name = "extension_seconds")
    private int extensionSeconds;

    // Vũ khí chống xung đột dữ liệu đa luồng dưới Database (Optimistic Locking)
    @Version
    @Column(name = "version")
    private int version;

    // =========================================================================
    // LƯU Ý: Các biến đa luồng và runtime bên dưới ĐÃ BỊ LOẠI BỎ vì DB không cần:
    // - private CopyOnWriteArrayList<AuctionObserver> observers;
    // - private ReentrantLock lock;
    // =========================================================================

    // BẮT BUỘC: Hàm khởi tạo rỗng cho Hibernate xài để tạo Proxy
    public AuctionEntity() {
        super();
    }

    // Lombok đã tự động lo toàn bộ Getter và Setter ở ngầm bên dưới rồi!
    public boolean isReverse() { return isReverse; }
    public void setReverse(boolean reverse) { isReverse = reverse; }
    public double getDropStep() { return dropStep; }
    public void setDropStep(double dropStep) { this.dropStep = dropStep; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public User getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(User currentWinner) { this.currentWinner = currentWinner; }
    public List<BiddingTransactionEntity> getTransactions() { return transactions; }
    public void setTransactions(List<BiddingTransactionEntity> transactions) { this.transactions = transactions; }
    public int getAntiSnipingSeconds() { return antiSnipingSeconds; }
    public void setAntiSnipingSeconds(int antiSnipingSeconds) { this.antiSnipingSeconds = antiSnipingSeconds; }
    public int getExtensionSeconds() { return extensionSeconds; }
    public void setExtensionSeconds(int extensionSeconds) { this.extensionSeconds = extensionSeconds; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
