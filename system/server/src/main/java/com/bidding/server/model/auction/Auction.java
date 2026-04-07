package com.bidding.server.model.auction;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.InvalidBidAmountException;
import com.bidding.server.model.core.Entity;
import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.enums.BidStatus;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.Bidder;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;
    private AuctionStatus status;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    private AtomicReference<Double> currentPrice;
    private Bidder currentWinner;
    private CopyOnWriteArrayList<BidTransaction> bidHistory;
    private CopyOnWriteArrayList<AuctionObserver> observers;
    private int antiSnipingSeconds;
    private int ectensionSeconds;
    private ReentrantLock lock;

    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(0.0);
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
    }

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds, int ectensionSeconds) {
        super();
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(item != null ? item.getStartingPrice() : 0.0);
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.ectensionSeconds = ectensionSeconds;
    }

    // --- Getter, Setter & Utility Methods ---
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public Bidder getCurrentWinner() {
        return currentWinner;
    }

    public AtomicReference<Double> getCurrentPrice() {
        return currentPrice;
    }

    //đặt giá
    public synchronized BidTransaction placeBid(Bidder bidder, double amount) {
        // TODO: Implement placeBid logic
        // 1. Kiểm tra trạng thái phiên (Fail-fast validation)
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng hoặc chưa bắt đầu."); //
        }

        BidTransaction newTransaction = null;
        AuctionEvent eventToNotify = null;

        // 2. Bắt đầu vùng găng (Critical Section) bằng ReentrantLock
        lock.lock(); // Mỗi lần đặt giá đều phải acquire lock trước
        try {
            // Đọc giá trị hiện tại
            Double expectedPrice = currentPrice.get();

            // Kiểm tra giá đặt có hợp lệ không (phải lớn hơn giá hiện tại)
            if (!isValidBid(amount)) {
                throw new InvalidBidAmountException("Giá đặt thấp hơn hoặc bằng giá hiện tại."); //
            }

            // 3. Ngăn chặn Lost Update bằng AtomicReference.compareAndSet
            // Chỉ cập nhật nếu currentPrice chưa bị luồng khác thay đổi
            if (currentPrice.compareAndSet(expectedPrice, amount)) { //

                // Cập nhật người thắng tạm thời
                this.currentWinner = bidder; // [cite: 32]

                // Tạo giao dịch và lưu vào lịch sử
                // bidHistory sử dụng CopyOnWriteArrayList để đảm bảo nhất quán khi đọc/ghi đồng thời
                newTransaction = new BidTransaction(bidder, this, amount, LocalDateTime.now(), BidStatus.ACCEPTED, false);
                this.bidHistory.add(newTransaction);

                // Kiểm tra và kích hoạt Anti-sniping (gia hạn thời gian nếu cần)
                checkAntiSnipe(); // [cite: 32]

                // Tạo sự kiện để thông báo, nhưng CHƯA gọi notify ngay lập tức
                eventToNotify = new AuctionEvent(EventType.BID_PLACED, this, newTransaction, LocalDateTime.now(), "Có lượt đặt giá mới: $" + amount); // [cite: 48, 62]
            } else {
                // Nếu compareAndSet trả về false, nghĩa là có thread khác đã chèn vào và đổi giá thành công
                throw new InvalidBidAmountException("Giá đã bị thay đổi bởi luồng khác. Vui lòng tải lại và thử lại.");
            }
        } finally {
            // 4. Luôn luôn giải phóng khóa trong khối finally
            lock.unlock();
        }

        /**
         * // TODO: Fix
        // 5. Notify Observers NGOÀI khối lock để tránh Deadlock
        if (eventToNotify != null) {
            notifyObservers(eventToNotify); //
        }*/

        return newTransaction; // [cite: 32]

    }
    // chuyển trạng thái phiên giao dịch
    public void start() {
        // 1. Kiểm tra tính hợp lệ của State Machine (Chỉ cho phép start khi đang OPEN)
        if (this.status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Lỗi logic: Chỉ có thể bắt đầu phiên đấu giá đang ở trạng thái OPEN.");
        }

        // 2. Chuyển trạng thái và thiết lập thời gian
        this.status = AuctionStatus.RUNNING;
        this.startTime = LocalDateTime.now();

        // 3. Ghi log hệ thống (Audit)
        // Lưu ý: Thực tế sẽ dùng Logger thay vì System.out.println
        System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] đã CHÍNH THỨC BẮT ĐẦU lúc " + this.startTime);

        // 4. Phát sự kiện cho các Observer (Client/Biểu đồ) biết phiên đã bắt đầu
        // Sử dụng EventType.AUCTION_STARTED đã định nghĩa trong tài liệu
        AuctionEvent event = new AuctionEvent(EventType.AUCTION_STARTED, this, null, LocalDateTime.now(), "Phiên đấu giá bắt đầu");
        //notifyObservers(event); observer pattern
    }
    public void end() {
        AuctionEvent closeEvent = null;

        // 1. Vùng Critical Section: Sử dụng khối synchronized hoặc ReentrantLock (this.lock)
        // để đảm bảo không có 2 luồng cùng chốt Winner.
        synchronized (this) {
            // Double-check: Đảm bảo phiên chưa bị đóng bởi một luồng (thread) khác chui vào trước đó
            if (this.status != AuctionStatus.RUNNING) {
                return; // Nếu không phải RUNNING thì bỏ qua (hoặc ném Exception tùy nghiệp vụ)
            }

            // Chuyển trạng thái và chốt thời gian
            this.status = AuctionStatus.FINISHED;
            this.endTime = LocalDateTime.now();

            // Lúc này block synchronized đảm bảo không ai có thể gọi placeBid() thành công nữa
            // currentWinner hiện tại chính là người thắng cuộc hợp lệ cuối cùng

            System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] ĐÃ ĐÓNG lúc " + this.endTime);
            if (this.currentWinner != null) {
                System.out.println(">>> Người chiến thắng: " + this.currentWinner.getUsername());
            }

            // Chuẩn bị Event (nhưng chưa phát đi vội)
            closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, this, null, LocalDateTime.now(), "Phiên đấu giá kết thúc");
        }

        // 2. Phát sự kiện (Gọi notifyObservers() ở ngoài khối lock)
        if (closeEvent != null) {
            //notifyObservers(closeEvent); observer pattern
        }
    }

    // Gia hạn phiên
    public void extendTimt(int second) {
        // TODO: Implement extendTimt logic
    }

    // Quản lý các observer
    public void addObserver(AuctionObserver observer) {
        if (observer != null && !this.observers.contains(observer)) {
            this.observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        if (observer != null) {
            this.observers.remove(observer);
        }
    }

    // Thông báo AutionEvent đến tất cả observer
    private void notifyObserver(AuctionEvent event) {
        // TODO: Implement notifyObserver logic
    }

    // Kiểm rta giá đặt hợp lệ
    private boolean isValidBid(double amount) {
        return amount > currentPrice.get();
    }

    private void checkAntiSnipe() {
        // TODO: Implement checkAntiSnipe logic
    }

    @Override
    public void validate() {
        // TODO: Implement validate logic
    }

    @Override
    public void printInfo() {
        // TODO: Implement printInfo logic
    }

    // Kiểm tra xem phiên đã có người đặt giá hay chưa
    public boolean hasBids() {
        return this.bidHistory != null && !this.bidHistory.isEmpty();
    }
}