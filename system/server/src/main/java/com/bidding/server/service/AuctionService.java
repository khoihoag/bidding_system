package com.bidding.server.service;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.auction.AuctionMapper;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.model.transaction.BiddingTransaction;
import com.bidding.server.events.AuctionEvent;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.user.User;
import com.bidding.server.model.auction.AutoBidConfig;
import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {
    private final AuctionRepository repository = new AuctionRepository();
    private final AuctionMapper mapper = AuctionMapper.INSTANCE;
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private UserService baoVe;
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> auctionTimers = new ConcurrentHashMap<>();

    // BỘ ĐẾM THỜI GIAN
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public void setUserService(UserService userService) {
        this.baoVe = userService;
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    // =======================================================
    // TỔNG CỤC THÔNG TIN: CHỈ SERVICE MỚI ĐƯỢC PHÁT EVENT
    // =======================================================
    private void notifyObservers(AuctionEvent event) {
        for (AuctionObserver obs : observers) {
            if (event.getEventType() == EventType.BID_PLACED) {
                obs.onBidPlaced(event);
            } else if (event.getEventType() == EventType.AUCTION_CLOSED) {
                obs.onAuctionClosed(event);
            } else if (event.getEventType() == EventType.AUCTION_STARTED) {
                obs.onAuctionStarted(event);
            }
        }
    }

    // ================= LOAD TỪ DB LÊN RAM =================
    public void loadAllAuctions() {
        List<AuctionEntity> entities = repository.findAll();
        List<Auction> models = mapper.toModelList(entities);

        for (Auction auction : models) {
            if (auction.getStatus() == AuctionStatus.FINISHED) {
                continue;
            }
            activeAuctions.put(auction.getId(), auction);

            if (auction.getStatus() == AuctionStatus.RUNNING) {
                scheduleAuctionEnd(auction);
            } else if (auction.getStatus() == AuctionStatus.OPEN) {
                handleOpenAuctionOnStartup(auction);
            }
        }
        System.out.println("[Service] Đã nạp " + activeAuctions.size() + " phiên đấu giá và gài đồng hồ.");
    }

    // ================= LOGIC ĐẾM NGƯỢC =================
    private void scheduleAuctionEnd(Auction auction) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());

        if (delaySeconds <= 0) {
            closeAuction(auction);
            return;
        }

        java.util.concurrent.ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
            closeAuction(auction);
            auctionTimers.remove(auction.getId());
        }, delaySeconds, TimeUnit.SECONDS);

        auctionTimers.put(auction.getId(), timerTask);
    }

    private void handleOpenAuctionOnStartup(Auction auction) {
        long secondsToStart = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getStartTime());

        if (secondsToStart <= 0) {
            auction.start();
            repository.saveOrUpdate(mapper.toEntity(auction));
            scheduleAuctionEnd(auction);

            // CHUẨN HÓA: Bắn loa thông báo nếu sàn lỡ mở ngay lúc Server khởi động
            AuctionEvent startEvent = new AuctionEvent(EventType.AUCTION_STARTED, auction, null, LocalDateTime.now(), "Sàn đấu giá đã mở!");
            notifyObservers(startEvent);
        } else {
            scheduler.schedule(() -> {
                auction.start();
                repository.saveOrUpdate(mapper.toEntity(auction));
                scheduleAuctionEnd(auction);

                // CHUẨN HÓA: Tới giờ hẹn tự động bắn loa
                AuctionEvent startEvent = new AuctionEvent(EventType.AUCTION_STARTED, auction, null, LocalDateTime.now(), "Phiên đấu giá cho " + auction.getItem().getName() + " chính thức bắt đầu!");
                notifyObservers(startEvent);
            }, secondsToStart, TimeUnit.SECONDS);
        }
    }

    private void closeAuction(Auction auction) {
        try {
            auction.end();
            repository.saveOrUpdate(mapper.toEntity(auction));
            System.out.println("[Service] Đã tự động đóng phiên ID: " + auction.getId());

            // CHUẨN HÓA: Hết giờ tự động khóa mõm và bắn loa
            AuctionEvent closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, auction, null, LocalDateTime.now(), "Phiên đấu giá kết thúc");
            notifyObservers(closeEvent);
        } catch (Exception e) {
            System.err.println("Lỗi khi tự động đóng phiên: " + auction.getId());
            e.printStackTrace();
        }
    }

    public void forceCloseManual(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá này trên sàn!");
        }
        closeAuction(auction); // Lợi dụng hàm ở trên để đóng và bắn loa luôn
    }

    // ================= XỬ LÝ ĐẶT GIÁ =================
    public void placeBid(String auctionId, Double amount, User bidder) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) throw new RuntimeException("Phiên đấu giá ID " + auctionId + " không tồn tại.");
        if (baoVe == null) throw new RuntimeException("Lỗi hệ thống: Chưa kết nối hệ thống Ngân hàng (UserService)!");

        User oldWinner = auction.getCurrentWinner();
        Double oldPrice = auction.getCurrentPrice().get();
        LocalDateTime oldEndTime = auction.getEndTime();

        baoVe.deductBalance(bidder, amount);

        try {
            BiddingTransaction tx = auction.placeBid(bidder, amount, false);

            if (auction.getEndTime().isAfter(oldEndTime)) {
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) oldTimer.cancel(false);
                scheduleAuctionEnd(auction);
            }

            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                baoVe.addBalance(oldWinner, oldPrice);
            }

            // 1. Lên DB lấy cái Entity gốc (cái mà Hibernate đang âm thầm theo dõi) ra trước
            // 1. Lên DB lấy cái Entity gốc ra trước
            // ===== THAY THẾ ĐOẠN LƯU DB BẰNG ĐOẠN SAU =====
            // Tạo luôn cục Entity mới toanh từ Model trên RAM
            // ===== ĐOẠN LƯU DB MỚI: TỰ TAY GÓI HÀNG, KHÔNG DÙNG MAPSTRUCT =====
            BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
            txEntity.setId(tx.getId());
            txEntity.setBidAmount(tx.getBidAmount());
            txEntity.setBidTime(tx.getBidTime());
            txEntity.setAutoBid(tx.isAutoBid());
            txEntity.setExtended(tx.isExtended());
            txEntity.setStatus(tx.getStatus());
            txEntity.setBidder(bidder); // bidder có sẵn trên RAM rồi

            // Gọi xạ thủ bắn thẳng vào DB
            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);
            // =================================================================

            // Đoạn phát loa bên dưới vẫn giữ nguyên nhé sếp
            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, java.time.LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);

            triggerAutoBids(auction);
        } catch (Exception e) {
            baoVe.addBalance(bidder, amount);
            throw e;
        }
    }

    public void triggerAutoBids(Auction auction) {
        boolean hasNewBid;
        do {
            hasNewBid = false;
            var queue = auction.getAutoBidQueue();

            for (AutoBidConfig bot : queue) {
                if (!bot.isActive()) continue;
                if (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId().equals(bot.getBidder().getId())) continue;

                double currentPrice = auction.getCurrentPrice().get();
                double nextBid = bot.getNextBidAmount(currentPrice);

                if (bot.canBid(currentPrice)) {
                    try {
                        executeInternalBid(auction, nextBid, bot.getBidder());
                        hasNewBid = true;
                        break;
                    } catch (Exception e) {
                        bot.deactivate();
                    }
                }
            }
        } while (hasNewBid);
    }

    private void executeInternalBid(Auction auction, double amount, User bidder) {
        User oldWinner = auction.getCurrentWinner();
        Double oldPrice = auction.getCurrentPrice().get();

        baoVe.deductBalance(bidder, amount);

        try {
            BiddingTransaction tx = auction.placeBid(bidder, amount, true);

            if (tx.isExtended()) {
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) oldTimer.cancel(false);
                scheduleAuctionEnd(auction);
            }

            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                baoVe.addBalance(oldWinner, oldPrice);
            }

            // ===== ĐOẠN LƯU DB MỚI: TỰ TAY GÓI HÀNG, KHÔNG DÙNG MAPSTRUCT =====
            BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
            txEntity.setId(tx.getId());
            txEntity.setBidAmount(tx.getBidAmount());
            txEntity.setBidTime(tx.getBidTime());
            txEntity.setAutoBid(tx.isAutoBid());
            txEntity.setExtended(tx.isExtended());
            txEntity.setStatus(tx.getStatus());
            txEntity.setBidder(bidder); // bidder có sẵn trên RAM rồi

            // Gọi xạ thủ bắn thẳng vào DB
            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);
            // =================================================================

            // Đoạn phát loa bên dưới vẫn giữ nguyên nhé sếp
            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, java.time.LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);

        } catch (Exception e) {
            baoVe.addBalance(bidder, amount);
            throw e;
        }
    }

    // ================= TẠO MỚI PHIÊN ĐẤU GIÁ =================
    public boolean isItemInActiveAuction(String itemId) {
        for (Auction auction : activeAuctions.values()) {
            if (auction.getItem() != null && auction.getItem().getId().equals(itemId)) return true;
        }
        return false;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Auction startAuction(Item item, int durationMinutes) {
        if (isItemInActiveAuction(item.getId())) throw new RuntimeException("Sản phẩm này đang được đấu giá rồi!");

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);
        newAuction.start();

        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        activeAuctions.put(newAuction.getId(), newAuction);
        scheduleAuctionEnd(newAuction);

        // CHUẨN HÓA: Chuyển nhiệm vụ thông báo "Món hàng mới" từ Controller sang đây!
        AuctionEvent startEvent = new AuctionEvent(
                EventType.AUCTION_STARTED,
                newAuction,
                null,
                LocalDateTime.now(),
                "Món hàng mới lên sàn: " + item.getName() + "!"
        );
        notifyObservers(startEvent);

        System.out.println("[Service] Đã mở phiên đấu giá cho: " + item.getName() + " (Status: RUNNING)");
        return newAuction;
    }

    public Auction scheduleAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        if (isItemInActiveAuction(item.getId())) throw new RuntimeException("Sản phẩm này đang nằm trong một phiên đấu giá khác!");
        if (startTime.isBefore(LocalDateTime.now())) throw new RuntimeException("Thời gian bắt đầu không được ở trong quá khứ!");
        if (endTime.isBefore(startTime)) throw new RuntimeException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");

        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);

        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        activeAuctions.put(newAuction.getId(), newAuction);

        long secondsToStart = ChronoUnit.SECONDS.between(LocalDateTime.now(), startTime);
        handleOpenAuctionOnStartup(newAuction); // Tái sử dụng luôn logic gài báo thức đã viết ở trên cho đồng bộ

        return newAuction;
    }
}