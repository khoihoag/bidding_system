package com.bidding.server.service;
import com.bidding.server.enums.BidStatus;
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
    private final ItemService quanLyKho = new ItemService(new com.bidding.server.repository.ItemRepository());
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

        // ================= ĐỒNG HỒ TỤT GIÁ TỰ ĐỘNG =================
        if (auction.isReverse()) {
            scheduler.scheduleAtFixedRate(() -> {
                if (auction.getStatus() == AuctionStatus.RUNNING) {
                    double current = auction.getCurrentPrice().get();
                    double newPrice = current - auction.getDropStep();

                    if (newPrice > 0) {
                        auction.getCurrentPrice().set(newPrice);
                        // Phát loa cho toàn bộ UI biết giá vừa giảm
                        AuctionEvent dropEvent = new AuctionEvent(EventType.BID_PLACED, auction, null, LocalDateTime.now(), "Giá giảm!");
                        notifyObservers(dropEvent);
                    } else {
                        // 1. CHẠM ĐÁY NỖI ĐAU: Ép giá về đúng 0 đồng (Không cho âm)
                        auction.getCurrentPrice().set(0.0);

                        // 2. Phát loa lần cuối để UI kịp hiển thị nút "CHỐT ĐƠN: 0 đ"
                        AuctionEvent finalDropEvent = new AuctionEvent(EventType.BID_PLACED, auction, null, LocalDateTime.now(), "Sập sàn!");
                        notifyObservers(finalDropEvent);

                        // 3. Câu giờ 1.5 giây cho người xem nhìn thấy số 0, rồi mới Đóng Phiên (Bán Ế)
                        scheduler.schedule(() -> {
                            // Check lại phát nữa cho chắc ăn, nhỡ trong 1.5s đó có ông bấm mua rồi
                            if (auction.getStatus() == AuctionStatus.RUNNING) {
                                closeAuction(auction);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
        }
        // ============================================================
    }

    private void handleOpenAuctionOnStartup(Auction auction) {
        long secondsToStart = java.time.temporal.ChronoUnit.SECONDS.between(java.time.LocalDateTime.now(), auction.getStartTime());

        if (secondsToStart <= 0) {
            auction.start();
            safeUpdateAuctionStatus(auction); // <--- GỌI TÀ THUẬT Ở ĐÂY
            scheduleAuctionEnd(auction);

            AuctionEvent startEvent = new AuctionEvent(EventType.AUCTION_STARTED, auction, null, java.time.LocalDateTime.now(), "Sàn đấu giá đã mở!");
            notifyObservers(startEvent);
        } else {
            scheduler.schedule(() -> {
                auction.start();
                safeUpdateAuctionStatus(auction); // <--- VÀ Ở ĐÂY NỮA
                scheduleAuctionEnd(auction);

                AuctionEvent startEvent = new AuctionEvent(EventType.AUCTION_STARTED, auction, null, java.time.LocalDateTime.now(), "Phiên đấu giá cho " + auction.getItem().getName() + " chính thức bắt đầu!");
                notifyObservers(startEvent);
            }, secondsToStart, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    // ================= VŨ KHÍ VUỐT VE HIBERNATE =================
    private void safeUpdateAuctionStatus(Auction auction) {
        // 1. Lôi cái Entity "chính chủ" đang được Hibernate bảo kê ra
        // (Sếp dùng hàm findById hoặc get tương ứng trong Repository của sếp nhé)
        com.bidding.server.model.auction.AuctionEntity existingEntity = repository.findById(auction.getId());

        if (existingEntity != null) {
            // 2. Chỉ cập nhật đúng cái trạng thái vừa được auction.start() đổi
            existingEntity.setStatus(auction.getStatus());

            // 3. Lưu lại nhẹ nhàng. Danh sách transactions không bị đụng tới nên Hibernate ăn ngon ngủ yên!
            repository.saveOrUpdate(existingEntity);
        } else {
            // Backup trường hợp hiếm: Mới tinh chưa có trong DB
            repository.saveOrUpdate(mapper.toEntity(auction));
        }
    }
    // ============================================================

    private void closeAuction(Auction auction) {
        try {
            auction.end();
            auction.setStatus(AuctionStatus.FINISHED);

            User winner = auction.getCurrentWinner();
            if (winner != null) {
                User originalSeller = baoVe != null ? baoVe.findById(auction.getItem().getSellerId()) : null;
                auction.getItem().setSellerId(winner.getId());
                auction.getItem().setSellerFullName(winner.getFullName());

                if (baoVe != null && originalSeller != null) {
                    baoVe.addBalance(originalSeller, auction.getCurrentPrice().get());
                }

                // CHỐT 1: Lệnh DB chốt người thắng (Không dùng mapper nữa)
                repository.finalizeAuction(auction.getId(), AuctionStatus.FINISHED, winner);

                // CHỐT 2: Lệnh DB chuyển sổ đỏ món hàng
                quanLyKho.changeItemOwner(auction.getItem().getId(), winner);

            } else {
                auction.setStatus(AuctionStatus.FAILED);
                repository.finalizeAuction(auction.getId(), AuctionStatus.FAILED, null);
            }

            System.out.println("[Service] Đã chốt đơn thành công phiên ID: " + auction.getId());

            AuctionEvent closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, auction, null, LocalDateTime.now(), "Phiên đấu giá kết thúc");
            notifyObservers(closeEvent);

        } catch (Exception e) {
            System.err.println("❌ LỖI CHỐT ĐƠN PHIÊN " + auction.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void forceCloseManual(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá này trên sàn!");
        }
        closeAuction(auction);
    }

    // ================= XỬ LÝ ĐẶT GIÁ =================
    public void placeBid(String auctionId, Double amount, User bidder) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) throw new RuntimeException("Phiên đấu giá ID " + auctionId + " không tồn tại.");
        if (baoVe == null) throw new RuntimeException("Lỗi hệ thống: Chưa kết nối hệ thống Ngân hàng (UserService)!");

        // ================= LOGIC ĐẤU GIÁ NGƯỢC (INSTANT WIN) =================
        // ================= LOGIC ĐẤU GIÁ NGƯỢC (INSTANT WIN) =================
        // ================= LOGIC ĐẤU GIÁ NGƯỢC (INSTANT WIN) =================
        // ================= LOGIC ĐẤU GIÁ NGƯỢC (INSTANT WIN) =================
        // ================= LOGIC ĐẤU GIÁ NGƯỢC (INSTANT WIN) =================
        if (auction.isReverse()) {
            double finalPrice = auction.getCurrentPrice().get();
            baoVe.deductBalance(bidder, finalPrice);

            try {
                auction.setCurrentWinner(bidder);

                BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
                txEntity.setId(System.currentTimeMillis());
                txEntity.setBidAmount(finalPrice);
                txEntity.setBidTime(LocalDateTime.now());
                txEntity.setAutoBid(false);
                txEntity.setExtended(false);
                txEntity.setBidder(bidder);
                txEntity.setStatus(com.bidding.server.enums.BidStatus.ACCEPTED); // Đủ trạng thái

                // 1. Lưu lịch sử đặt giá (Bây giờ đã dùng merge() nên cực mượt, không sập nữa)
                repository.saveNewBid(auction.getId(), finalPrice, bidder, auction.getEndTime(), txEntity);

                System.out.println("[Reverse Auction] Đại gia " + bidder.getUsername() + " đã chốt đơn với giá: " + finalPrice);

                // 2. Gọi hàm đóng phiên ngay trên luồng này, an toàn 100%
                closeAuction(auction);

                return;

            } catch (Exception e) {
                baoVe.addBalance(bidder, finalPrice);
                System.err.println("❌ LỖI CHỐT ĐƠN NGƯỢC: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Lỗi chốt đơn: " + e.getMessage());
            }
        }
        // =====================================================================
        // =====================================================================
        // =====================================================================

        // =====================================================================

        // ------------------ LUỒNG ĐẤU GIÁ THÔNG THƯỜNG -----------------------
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

            BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
            txEntity.setId(tx.getId());
            txEntity.setBidAmount(tx.getBidAmount());
            txEntity.setBidTime(tx.getBidTime());
            txEntity.setAutoBid(tx.isAutoBid());
            txEntity.setExtended(tx.isExtended());
            txEntity.setStatus(tx.getStatus());
            txEntity.setBidder(bidder);

            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);

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

            BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
            txEntity.setId(tx.getId());
            txEntity.setBidAmount(tx.getBidAmount());
            txEntity.setBidTime(tx.getBidTime());
            txEntity.setAutoBid(tx.isAutoBid());
            txEntity.setExtended(tx.isExtended());
            txEntity.setStatus(tx.getStatus());
            txEntity.setBidder(bidder);

            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);

            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, java.time.LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);

        } catch (Exception e) {
            baoVe.addBalance(bidder, amount);
            throw e;
        }
    }

    public boolean isItemInActiveAuction(String itemId) {
        for (Auction auction : activeAuctions.values()) {
            if (auction.getItem() != null && auction.getItem().getId().equals(itemId)) return true;
        }
        return false;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public List<Auction> getAllAuctionsForDisplay() {
        java.util.LinkedHashMap<String, Auction> auctionsById = new java.util.LinkedHashMap<>();

        for (Auction auction : mapper.toModelList(repository.findAll())) {
            auctionsById.put(auction.getId(), auction);
        }

        for (Auction auction : activeAuctions.values()) {
            auctionsById.put(auction.getId(), auction);
        }

        return new ArrayList<>(auctionsById.values());
    }

    // ================= KHỞI TẠO PHIÊN =================
    public Auction startAuction(Item item, int durationMinutes) {
        return startAuction(item, durationMinutes, false, 0.0);
    }

    public Auction startAuction(Item item, int durationMinutes, boolean isReverse, double dropStep) {
        if (isItemInActiveAuction(item.getId())) throw new RuntimeException("Sản phẩm này đang được đấu giá rồi!");

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);

        newAuction.setReverse(isReverse);
        newAuction.setDropStep(dropStep);
        newAuction.start();

        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        activeAuctions.put(newAuction.getId(), newAuction);
        scheduleAuctionEnd(newAuction);

        AuctionEvent startEvent = new AuctionEvent(
                EventType.AUCTION_STARTED,
                newAuction,
                null,
                LocalDateTime.now(),
                "Món hàng mới lên sàn: " + item.getName() + "!"
        );
        notifyObservers(startEvent);

        System.out.println("[Service] Đã mở phiên đấu giá cho: " + item.getName() + " (Status: RUNNING, Reverse: " + isReverse + ")");
        return newAuction;
    }

    public Auction scheduleAuction(Item item, LocalDateTime startTime, LocalDateTime endTime, boolean isReverse, double dropStep) {
        if (isItemInActiveAuction(item.getId())) throw new RuntimeException("Sản phẩm này đang nằm trong một phiên đấu giá khác!");
        if (startTime.isBefore(LocalDateTime.now())) throw new RuntimeException("Thời gian bắt đầu không được ở trong quá khứ!");
        if (endTime.isBefore(startTime)) throw new RuntimeException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");

        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);

        newAuction.setReverse(isReverse);
        newAuction.setDropStep(dropStep);

        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        activeAuctions.put(newAuction.getId(), newAuction);

        handleOpenAuctionOnStartup(newAuction);

        return newAuction;
    }
}
