package com.bidding.server.model.auction;
import java.util.concurrent.PriorityBlockingQueue;
import com.bidding.server.enums.BidStatus;
import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.InvalidBidAmountException;
import com.bidding.server.model.core.Entity;
import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransaction;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import com.bidding.server.model.user.User;
import lombok.Setter;
import lombok.Getter;
@Getter
@Setter
public class Auction extends Entity {
    private boolean isReverse = false; // Đánh dấu đây là đấu giá ngược
    private double dropStep = 0.0;
    private Item item;
    private AuctionStatus status;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    private AtomicReference<Double> currentPrice;
    private User currentWinner;
    private CopyOnWriteArrayList<BiddingTransaction> bidHistory;
    private PriorityBlockingQueue<AutoBidConfig> autoBidQueue;
    private int antiSnipingSeconds;
    private int extensionSeconds; // Đã sửa lỗi chính tả từ ectensionSeconds
    private ReentrantLock lock;
    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(0.0);
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.autoBidQueue = new PriorityBlockingQueue<>();
        this.lock = new ReentrantLock();
    }

    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds, int extensionSeconds) {
        super();
        // Nhất quán với cách ông làm ở User và Item để không mất ID khi lưu/load
        if (id != null && !id.isEmpty()) {
            this.setId(id);
        }
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN; // Mặc định là OPEN
        this.currentPrice = new AtomicReference<>(item != null ? item.getStartingPrice() : 0.0);
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.extensionSeconds = extensionSeconds;
        this.autoBidQueue = new PriorityBlockingQueue<>();
    }

    // Đặt giá
    // ĐÃ XÓA CHỮ 'synchronized' Ở ĐÂY
    public BiddingTransaction placeBid(User bidder, double amount) {
        return placeBid(bidder, amount, false);
    }

    public BiddingTransaction placeBid(User bidder, double amount,boolean isAutoBid) {
        // 1. Kiểm tra trạng thái phiên (Fail-fast validation)
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng hoặc chưa bắt đầu.");
        }

        BiddingTransaction newTransaction = null;

        // 2. Bắt đầu vùng găng (Critical Section) bằng ReentrantLock
        lock.lock();
        try {
            // Đọc giá trị hiện tại
            Double expectedPrice = currentPrice.get();

            // Kiểm tra giá đặt có hợp lệ không
            if (!isValidBid(amount)) {
                throw new InvalidBidAmountException("Giá đặt thấp hơn hoặc bằng giá hiện tại.");
            }

            // 3. Ngăn chặn Lost Update bằng AtomicReference.compareAndSet
            if (currentPrice.compareAndSet(expectedPrice, amount)) {

                // Cập nhật người thắng tạm thời
                this.currentWinner = bidder;

                // Kiểm tra Anti-snipe
                boolean isExtended = checkAntiSnipe();

                // Tạo giao dịch và lưu vào lịch sử
                newTransaction = new BiddingTransaction(
                        bidder, this, amount, LocalDateTime.now(), BidStatus.ACCEPTED, isAutoBid, isExtended
                );

                this.bidHistory.add(newTransaction);

            } else {
                throw new InvalidBidAmountException("Giá đã bị thay đổi bởi luồng khác. Vui lòng tải lại và thử lại.");
            }
        } finally {
            // 4. Luôn luôn giải phóng khóa
            lock.unlock();
        }

        return newTransaction;
    }

    // Chuyển trạng thái phiên giao dịch
    public void start() {
        if (this.status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Lỗi logic: Chỉ có thể bắt đầu phiên đấu giá đang ở trạng thái OPEN.");
        }

        this.status = AuctionStatus.RUNNING;
        this.startTime = LocalDateTime.now();

        System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] đã CHÍNH THỨC BẮT ĐẦU lúc " + this.startTime);

    }

    public void end() {
        AuctionEvent closeEvent = null;

        synchronized (this) {
            if (this.status != AuctionStatus.RUNNING) {
                return;
            }

            this.status = AuctionStatus.FINISHED;
            this.endTime = LocalDateTime.now();

            System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] ĐÃ ĐÓNG lúc " + this.endTime);
            if (this.currentWinner != null) {
                System.out.println(">>> Người chiến thắng: " + this.currentWinner.getUsername());
            }

        }

    }

    // Kiểm tra giá đặt hợp lệ
    private boolean isValidBid(double amount) {
        return amount > currentPrice.get();
    }

    private boolean checkAntiSnipe() {
        // Nếu không cấu hình thời gian chống bắn tỉa thì bỏ qua
        if (this.antiSnipingSeconds <= 0 || this.extensionSeconds <= 0) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        // Tính toán "Vùng Đỏ"
        LocalDateTime snipeThreshold = this.endTime.minusSeconds(this.antiSnipingSeconds);

        // Nếu thời điểm hiện tại nằm trong Vùng Đỏ và phiên chưa thực sự kết thúc
        if (now.isAfter(snipeThreshold) && now.isBefore(this.endTime)) {
            // Gia hạn thời gian kết thúc
            this.endTime = this.endTime.plusSeconds(this.extensionSeconds);
            System.out.println("[ANTI-SNIPE] Phát hiện bắn tỉa! Gia hạn thêm " + this.extensionSeconds + " giây.");
            return true;
        }

        return false;
    }
    public void registerAutoBid(User user, double maxBid, double increment) {
        if (this.status != AuctionStatus.RUNNING && this.status != AuctionStatus.OPEN) {
            throw new RuntimeException("Chỉ được cài Auto-Bid khi phiên chưa kết thúc!");
        }

        // Tạo cấu hình mới
        AutoBidConfig config = new AutoBidConfig(user, maxBid, increment);

        // Ném vào hàng đợi, nó sẽ tự nhảy vào đúng vị trí theo thời gian registeredAt
        autoBidQueue.put(config);

        System.out.println("[Auto-Bid] User " + user.getUsername() + " đã cài Bot thành công!");
    }

    // Getter để Service có thể lôi đống Bot ra xử lý
    public PriorityBlockingQueue<AutoBidConfig> getAutoBidQueue() {
        return autoBidQueue;
    }


    public void validate() {
        // TODO: Implement validate logic
    }

    public void printInfo() {
        // TODO: Implement printInfo logic
    }

    // Kiểm tra xem phiên đã có người đặt giá hay chưa
    public boolean hasBids() {
        return this.bidHistory != null && !this.bidHistory.isEmpty();
    }

}
