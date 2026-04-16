package com.bidding.server.service;

import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.exception.AuctionException;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AutoBidConfig;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.*;

public class AuctionManager {
    // Biến instance volatile đảm bảo memory visibility trong Double-Checked Locking
    private static volatile AuctionManager instance;

    // Lưu trữ các phiên đang hoạt động, đảm bảo thread-safe
    private final ConcurrentHashMap<String, Auction> activeAuctions;

    // Lên lịch tự động đóng phiên
    private final ScheduledExecutorService schedulerService;

    // Hàng đợi xử lý autobid, thread-safe và sắp xếp theo độ ưu tiên
    private final PriorityBlockingQueue<AutoBidConfig> autoBidQueue;

    // Private constructor ngăn khởi tạo từ bên ngoài
    private AuctionManager() {
        this.activeAuctions = new ConcurrentHashMap<>();
        // Khởi tạo ThreadPool (Cần tuning số lượng thread dựa trên CPU cores thực tế)
        this.schedulerService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        // PriorityQueue cần Comparator, dựa trên thời gian đăng ký (registeredAt)
        this.autoBidQueue = new PriorityBlockingQueue<>(100,
                (c1, c2) -> c1.getRegisteredAt().compareTo(c2.getRegisteredAt())
        );
    }

    // Lấy Singleton bằng Double-Checked Locking
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // Đăng ký phiên vào quản lý
    public void registerAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            activeAuctions.put(auction.getId(), auction);
            scheduleClose(auction);
        }
    }

    // Tìm phiên theo id, sử dụng Optional để tránh NullPointerException
    public Optional<Auction> getAuction(String id) {
        return Optional.ofNullable(activeAuctions.get(id));
    }

    // Đặt lịch tự động đóng phiên
    public void scheduleClose(Auction auction) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
        if (delaySeconds <= 0) {
            auction.close(); // Đóng ngay nếu thời gian đã qua
            return;
        }

        schedulerService.schedule(() -> {
            try {
                // Kiểm tra lại trạng thái trước khi đóng để tránh race condition
                if (auction.getStatus() == AuctionStatus.RUNNING) {
                    auction.close();
                    // Clean up: Xóa khỏi bộ nhớ sau khi hoàn thành để tránh Memory Leak
                    activeAuctions.remove(auction.getId());
                }
            } catch (Exception e) {
                // Ghi log lỗi hệ thống, không để exception làm chết thread pool
                System.err.println("Error closing auction: " + auction.getId());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    // Xử lý hàng đợi auto-bid sau mỗi bid [cite: 44]
    private void processAutoBids(Auction auction) {
        // Logic giả định: Lấy config có ưu tiên cao nhất ra xử lý
        // Lưu ý: Cần cẩn trọng vòng lặp vô hạn nếu nhiều autobid tranh giành nhau
        while (!autoBidQueue.isEmpty()) {
            AutoBidConfig config = autoBidQueue.poll();
            if (config != null && config.isActive() && config.canBid(auction.getCurrentPrice().get(), auction.getCurrentWinner().getId())) {
                try {
                    double nextAmount = config.getNextBidAmount(auction.getCurrentPrice().get());
                    auction.placeBid(config.getBidder(), nextAmount);
                } catch (AuctionException e) {
                    // Xử lý custom exception (ví dụ: AuctionClosedException) [cite: 51]
                    config.deactivate();
                }
            }
        }
    }

    // Dừng sạch toàn bộ scheduler khi shutdown server
    public void shutdown() {
        schedulerService.shutdown();
        try {
            if (!schedulerService.awaitTermination(60, TimeUnit.SECONDS)) {
                schedulerService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            schedulerService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}