package com.bidding.server.model;
import com.bidding.server.model.user.User;
import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.model.core.Entity; // Class ông nội chứa @Id và @MappedSuperclass
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "auctions")
public class AuctionEntity extends Entity {

    // Giả định 1 món đồ (Item) chỉ được đấu giá 1 lần duy nhất trong đời
    @OneToOne(fetch = FetchType.LAZY)
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
}