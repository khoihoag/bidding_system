package com.bidding.server.model.auction;

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
    private Item item;
    private AuctionStatus status;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    private AtomicReference<Double> currentPrice;
    private User currentWinner;
    private CopyOnWriteArrayList<BiddingTransaction> bidHistory;
    private CopyOnWriteArrayList<AuctionObserver> observers;
    private int antiSnipingSeconds;
    private int extensionSeconds; // Đã sửa lỗi chính tả từ ectensionSeconds
    private ReentrantLock lock;
    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(0.0);
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
    }

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds, int extensionSeconds) {
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
        this.extensionSeconds = extensionSeconds; // Đã sửa lỗi chính tả
    }

    // Đặt giá
    public synchronized BiddingTransaction placeBid(User bidder, double amount) {
        // 1. Kiểm tra trạng thái phiên (Fail-fast validation)
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng hoặc chưa bắt đầu.");
        }

        BiddingTransaction newTransaction = null;
        AuctionEvent eventToNotify = null;

        // 2. Bắt đầu vùng găng (Critical Section) bằng ReentrantLock
        lock.lock();
        try {
            // Đọc giá trị hiện tại
            Double expectedPrice = currentPrice.get();

            // Kiểm tra giá đặt có hợp lệ không (phải lớn hơn giá hiện tại)
            if (!isValidBid(amount)) {
                throw new InvalidBidAmountException("Giá đặt thấp hơn hoặc bằng giá hiện tại.");
            }

            // 3. Ngăn chặn Lost Update bằng AtomicReference.compareAndSet
            if (currentPrice.compareAndSet(expectedPrice, amount)) {

                // Cập nhật người thắng tạm thời
                this.currentWinner = bidder;

                // Tạo giao dịch và lưu vào lịch sử
                newTransaction = new BiddingTransaction(bidder, this, amount, LocalDateTime.now(), BidStatus.ACCEPTED, false);
                this.bidHistory.add(newTransaction);

                // Kiểm tra và kích hoạt Anti-sniping
                checkAntiSnipe();

                // Tạo sự kiện để thông báo
                eventToNotify = new AuctionEvent(EventType.BID_PLACED, this, newTransaction, LocalDateTime.now(), "Có lượt đặt giá mới: $" + amount);
            } else {
                throw new InvalidBidAmountException("Giá đã bị thay đổi bởi luồng khác. Vui lòng tải lại và thử lại.");
            }
        } finally {
            // 4. Luôn luôn giải phóng khóa
            lock.unlock();
        }

        /* TODO: Fix
        // 5. Notify Observers NGOÀI khối lock để tránh Deadlock
        if (eventToNotify != null) {
            notifyObservers(eventToNotify);
        }
        */

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

        AuctionEvent event = new AuctionEvent(EventType.AUCTION_STARTED, this, null, LocalDateTime.now(), "Phiên đấu giá bắt đầu");
        //notifyObservers(event);
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

            closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, this, null, LocalDateTime.now(), "Phiên đấu giá kết thúc");
        }

        if (closeEvent != null) {
            //notifyObservers(closeEvent);
        }
    }

    // Gia hạn phiên (Đã sửa tên hàm cho đúng chính tả)
    public void extendTime(int seconds) {
        // TODO: Implement extendTime logic
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

    // Kiểm tra giá đặt hợp lệ
    private boolean isValidBid(double amount) {
        return amount > currentPrice.get();
    }

    private void checkAntiSnipe() {
        // TODO: Implement checkAntiSnipe logic
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