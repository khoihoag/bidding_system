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
public class Auction extends Entity {
    private final CopyOnWriteArrayList<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> followerIds;
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
        this.followerIds = new CopyOnWriteArrayList<>(); // THÊM DÒNG NÀY
    }
    public void addObserver(AuctionObserver observer) { observers.add(observer); }
    public void removeObserver(AuctionObserver observer) { observers.remove(observer); }
    private void notifyObservers(AuctionEvent event) {
        for (AuctionObserver observer : observers) {
            observer.onBidPlaced(event); // Cái này sẽ gọi hàm onBidPlaced trong ClientManager
        }
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
        this.followerIds = new CopyOnWriteArrayList<>(); // THÊM DÒNG NÀY
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.extensionSeconds = extensionSeconds;
        this.autoBidQueue = new PriorityBlockingQueue<>();
    }

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
    public AtomicReference<Double> getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(AtomicReference<Double> currentPrice) { this.currentPrice = currentPrice; }
    public User getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(User currentWinner) { this.currentWinner = currentWinner; }
    public CopyOnWriteArrayList<BiddingTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(CopyOnWriteArrayList<BiddingTransaction> bidHistory) { this.bidHistory = bidHistory; }
    public CopyOnWriteArrayList<String> getFollowerIds() { return followerIds; }
    public void setFollowerIds(CopyOnWriteArrayList<String> followerIds) { this.followerIds = followerIds; }
    public void setAutoBidQueue(PriorityBlockingQueue<AutoBidConfig> autoBidQueue) { this.autoBidQueue = autoBidQueue; }
    public int getAntiSnipingSeconds() { return antiSnipingSeconds; }
    public void setAntiSnipingSeconds(int antiSnipingSeconds) { this.antiSnipingSeconds = antiSnipingSeconds; }
    public int getExtensionSeconds() { return extensionSeconds; }
    public void setExtensionSeconds(int extensionSeconds) { this.extensionSeconds = extensionSeconds; }
    public ReentrantLock getLock() { return lock; }
    public void setLock(ReentrantLock lock) { this.lock = lock; }

    // Đặt giá
    // ĐÃ XÓA CHỮ 'synchronized' Ở ĐÂY
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
            // Trong hàm placeBid của file Auction.java

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

                // ================= GẮN BOM TING TING (ĐÃ FIX ĐỦ 5 THAM SỐ) =================
                notifyObservers(new AuctionEvent(
                        EventType.BID_PLACED,             // 1. Event Type
                        this,                             // 2. Đối tượng Auction hiện tại
                        newTransaction,                   // 3. Giao dịch vừa thực hiện
                        LocalDateTime.now(),              // 4. Thời gian
                        "Mức giá mới: " + amount          // 5. Nội dung thông báo
                ));
                // ===========================================================================

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

    // Gia hạn phiên (Đã sửa tên hàm cho đúng chính tả)
    public void extendTime(int seconds) {
        // TODO: Implement extendTime logic
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
    // ========================================================
    // HÀM CHO TÍNH NĂNG THEO DÕI
    // ========================================================
    public void addFollower(String userId) {
        // Chỉ thêm nếu ông này chưa bấm theo dõi (tránh spam)
        if (userId != null && !this.followerIds.contains(userId)) {
            this.followerIds.add(userId);
            System.out.println("[Theo Dõi] Đã thêm User " + userId + " vào danh sách hóng phiên " + this.getId());
        }
    }

    public void removeFollower(String userId) {
        if (userId != null && this.followerIds.remove(userId)) {
            System.out.println("[Theo Dõi] Đã gỡ User " + userId + " khỏi phiên " + this.getId());
        }
    }

}
