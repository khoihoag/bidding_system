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
import java.util.concurrent.ConcurrentLinkedDeque;
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
    private ConcurrentLinkedDeque<BidTransaction> bidHistory;// có thể trực tiếp dưới db
    private ConcurrentLinkedDeque<AuctionObserver> observers;
    private int antiSnipingSeconds;
    private int extensionSeconds;
    private ReentrantLock lock;
    private volatile BidTransaction currentWinningTransaction;

    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(0.0);
        this.bidHistory = new ConcurrentLinkedDeque<>();
        this.observers = new ConcurrentLinkedDeque<>();
        this.lock = new ReentrantLock();
        this.currentWinningTransaction = null;
    }

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime,
                   int antiSnipingSeconds, int extensionSeconds) {
        super();
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentPrice = new AtomicReference<>(item != null ? item.getStartingPrice() : 0.0);
        this.bidHistory = new ConcurrentLinkedDeque<>();
        this.observers = new ConcurrentLinkedDeque<>();
        this.lock = new ReentrantLock();
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.extensionSeconds = extensionSeconds;
        this.currentWinningTransaction = null;
    }



    // --- Getter, Setter & Utility Methods ---
    public BidTransaction getCurrentWinningTransaction() {return this.currentWinningTransaction;}

    public LocalDateTime getEndTime() {return endTime;}

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
    public BidTransaction placeBid(Bidder bidder, double amount) {
        // 2. Fail-fast validation (Kiểm tra ngoài lock để tiết kiệm tài nguyên)
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá đã đóng hoặc chưa bắt đầu.");
        }

        BidTransaction newTransaction = null;
        AuctionEvent eventToNotify = null;

        // 3. Bắt đầu vùng găng (Critical Section)
        lock.lock();
        try {
            // 4. Double-check trạng thái: Bắt buộc phải kiểm tra lại bên trong Lock
            // để đề phòng trường hợp hàm end() vừa chạy xong ở luồng khác.
            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá vừa bị chốt đóng. Vui lòng tham gia phiên khác.");
            }

            BidTransaction previousWinningBid = this.getCurrentWinningTransaction();

            // 5. Kiểm tra giá đặt
            if (!isValidBid(amount)) { // Đã an toàn vì bên trong lock
                throw new InvalidBidAmountException("Giá đặt $" + amount + " thấp hơn hoặc bằng giá hiện tại: $" + currentPrice.get());
            }

            // 6. Cập nhật dữ liệu (Dùng .set() thông thường vì đã có Lock bảo vệ độc quyền)
            currentPrice.set(amount);

            if (previousWinningBid != null) {
                previousWinningBid.markOutbid();
            }



            // Cập nhật người thắng tạm thời
            this.currentWinner = bidder;

            // Tạo giao dịch và lưu vào lịch sử (CopyOnWriteArrayList)
            newTransaction = new BidTransaction(bidder, this, amount, LocalDateTime.now(), BidStatus.ACCEPTED, false);
            this.currentWinningTransaction = newTransaction;
            this.bidHistory.addLast(newTransaction);

            // Kiểm tra và kích hoạt Anti-sniping
            checkAntiSnipe();

            // Chuẩn bị sự kiện
            eventToNotify = new AuctionEvent(EventType.BID_PLACED, this, newTransaction, LocalDateTime.now(), "Có lượt đặt giá mới: $" + amount);

        } finally {
            // 7. LUÔN LUÔN giải phóng khóa trong khối finally
            lock.unlock();
        }

        // 8. Notify Observers NGOÀI khối lock để tránh Deadlock (Ví dụ: Observer lại gọi ngược API vào Auction)
        if (eventToNotify != null) {
            //notifyObservers(eventToNotify); // Bỏ comment khi bạn đã implement hàm này
        }

        return newTransaction;
    }
    // chuyển trạng thái phiên giao dịch
    // 1. Phương thức start(): Đã bổ sung Lock để ngăn chặn 2 luồng cùng lúc "khởi động" phiên
    public void start() {
        AuctionEvent startEvent = null;

        lock.lock(); // Dùng chung ReentrantLock với placeBid và end
        try {
            // Kiểm tra tính hợp lệ bên trong Lock
            if (this.status != AuctionStatus.OPEN) {
                throw new IllegalStateException("Hành động từ chối: Chỉ có thể bắt đầu phiên đấu giá đang ở trạng thái OPEN.");
            }

            // Cập nhật trạng thái
            this.status = AuctionStatus.RUNNING;
            this.startTime = LocalDateTime.now();

            System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] đã CHÍNH THỨC BẮT ĐẦU lúc " + this.startTime);

            // Chuẩn bị Event
            startEvent = new AuctionEvent(EventType.AUCTION_STARTED, this, null, LocalDateTime.now(), "Phiên đấu giá bắt đầu");
        } finally {
            // Đảm bảo luôn nhả khóa
            lock.unlock();
        }

        // Bắn Event ra bên ngoài để tránh Deadlock với Observer
        if (startEvent != null) {
            // notifyObservers(startEvent);
        }
    }

    // 2. Phương thức end(): Loại bỏ 'synchronized(this)', dùng 'lock' để đồng bộ hóa với placeBid
    public void close() {
        AuctionEvent closeEvent = null;

        lock.lock(); // Bước ngoặt: Giờ thì end() và placeBid() đã "nhìn thấy" nhau
        try {
            // Double-check: Nếu không phải RUNNING thì không làm gì cả
            if (this.status != AuctionStatus.RUNNING) {
                return;
            }

            // Chốt trạng thái FINISHED [cite: 73]
            this.status = AuctionStatus.FINISHED;
            this.endTime = LocalDateTime.now();

            System.out.println("[AUDIT] Phiên đấu giá [" + this.getId() + "] ĐÃ ĐÓNG lúc " + this.endTime);

            // Xử lý Business Logic: Có người thắng hay không?
            if (this.currentWinner != null) {
                System.out.println(">>> Người chiến thắng: " + this.currentWinner.getUsername() + " với giá $" + this.currentPrice.get());
                closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, this, null, LocalDateTime.now(), "Phiên kết thúc. Đang chờ thanh toán.");

                // TODO (Tương lai): Gọi Job kích hoạt luồng thanh toán -> PAID [cite: 73]
            } else {
                System.out.println(">>> Không có ai đặt giá. Sản phẩm không bán được.");
                closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, this, null, LocalDateTime.now(), "Phiên kết thúc: Không có người mua.");

                // Tùy chọn: Bạn có thể cập nhật trạng thái thành CANCELED ngay tại đây nếu logic business yêu cầu [cite: 73]
            }
        } finally {
            lock.unlock();
        }

        // Phát sự kiện an toàn ngoài vùng khóa
        if (closeEvent != null) {
            // notifyObservers(closeEvent);
        }
    }

    // Gia hạn phiên
    public void extendTime(int second) {
        // TODO: Implement extendTimelogic
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
        // 1. Kiểm tra ID kế thừa từ Entity
        if (this.getId() == null || this.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: ID phiên đấu giá không được để trống.");
        }

        // 2. Kiểm tra quan hệ với Item
        if (this.item == null) {
            throw new IllegalArgumentException("Validation failed: Phiên đấu giá phải gắn với một sản phẩm (Item).");
        }

        // 3. Kiểm tra tính hợp lệ của thời gian
        if (this.startTime == null || this.endTime == null) {
            throw new IllegalArgumentException("Validation failed: Thời gian bắt đầu và kết thúc không được null.");
        }
        if (this.startTime.isAfter(this.endTime)) {
            throw new IllegalArgumentException("Validation failed: Thời gian bắt đầu không thể diễn ra sau thời gian kết thúc.");
        }

        // 4. Kiểm tra dữ liệu an toàn luồng (Thread-safe)
        if (this.currentPrice == null || this.currentPrice.get() < 0) {
            throw new IllegalArgumentException("Validation failed: Giá hiện tại (AtomicReference) không hợp lệ.");
        }
    }

    @Override
    public void printInfo() {

        // Xây dựng chuỗi thông tin thay vì in lắt nhắt
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===\n");
        infoBuilder.append("ID Phiên: ").append(this.getId()).append("\n");
        infoBuilder.append("Ngày tạo: ").append(this.getCreatedAt()).append("\n");
        infoBuilder.append("Trạng thái hiện tại: ").append(this.status).append("\n");

        // Đảm bảo lấy giá trị an toàn từ AtomicReference
        if (this.currentPrice != null) {
            infoBuilder.append("Giá đang dẫn đầu: ").append(this.currentPrice.get()).append("\n");
        }

        // TODO:Trong thực tế hãy dùng logger.info(infoBuilder.toString());
        System.out.println(infoBuilder.toString());
    }

    // Kiểm tra xem phiên đã có người đặt giá hay chưa
    public boolean hasBids() {
        return this.bidHistory != null && !this.bidHistory.isEmpty();
    }
}