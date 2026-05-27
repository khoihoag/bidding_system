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
    private final ItemService quanLyKho;
    private final AuctionRepository repository;
    private final AuctionMapper mapper = AuctionMapper.INSTANCE;
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private UserService baoVe;
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> auctionTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> reversePriceTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> paymentTimers = new ConcurrentHashMap<>();
    private static final long PAYMENT_TIMEOUT_MINUTES = 10;

    // BỘ ĐẾM THỜI GIAN
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public AuctionService() {
        this(new ItemService(new com.bidding.server.repository.ItemRepository()), new AuctionRepository());
    }

    public AuctionService(ItemService itemService, AuctionRepository repository) {
        this.quanLyKho = itemService;
        this.repository = repository;
    }

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
            if (auction.getStatus() == AuctionStatus.FINISHED && auction.getCurrentWinner() != null) {
                if (isItemAlreadyTransferredToWinner(auction)) {
                    auction.setStatus(AuctionStatus.PAID);
                    repository.finalizeAuction(auction.getId(), AuctionStatus.PAID, auction.getCurrentWinner());
                    continue;
                }
                schedulePaymentTimeout(auction);
                continue;
            }
            if (isTerminalStatus(auction.getStatus())) {
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
    private boolean isTerminalStatus(AuctionStatus status) {
        return status == AuctionStatus.FINISHED
                || status == AuctionStatus.PAID
                || status == AuctionStatus.FAILED
                || status == AuctionStatus.CANCELED;
    }

    private boolean isItemAlreadyTransferredToWinner(Auction auction) {
        return auction != null
                && auction.getItem() != null
                && auction.getCurrentWinner() != null
                && auction.getCurrentWinner().getId().equals(auction.getItem().getSellerId());
    }

    private boolean isItemAlreadyTransferredToWinner(AuctionEntity auction) {
        return auction != null
                && auction.getItem() != null
                && auction.getCurrentWinner() != null
                && auction.getCurrentWinner().getId().equals(auction.getItem().getSellerId());
    }

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
            java.util.concurrent.ScheduledFuture<?> dropTask = scheduler.scheduleAtFixedRate(() -> {
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
            reversePriceTimers.put(auction.getId(), dropTask);
        }
        // ============================================================
    }

    private void cancelAuctionTimers(String auctionId) {
        java.util.concurrent.ScheduledFuture<?> endTimer = auctionTimers.remove(auctionId);
        if (endTimer != null) {
            endTimer.cancel(false);
        }
        java.util.concurrent.ScheduledFuture<?> dropTimer = reversePriceTimers.remove(auctionId);
        if (dropTimer != null) {
            dropTimer.cancel(false);
        }
    }

    private void cancelPaymentTimer(String auctionId) {
        java.util.concurrent.ScheduledFuture<?> paymentTimer = paymentTimers.remove(auctionId);
        if (paymentTimer != null) {
            paymentTimer.cancel(false);
        }
    }

    public Auction findAuctionById(String auctionId) {
        Auction active = activeAuctions.get(auctionId);
        if (active != null) {
            return active;
        }
        AuctionEntity entity = repository.findById(auctionId);
        return entity != null ? mapper.toModel(entity) : null;
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
        cancelAuctionTimers(auction.getId());
        try {
            LocalDateTime closedAt = LocalDateTime.now();
            auction.setEndTime(closedAt);

            User winner = auction.getCurrentWinner();
            if (winner != null) {
                auction.setStatus(AuctionStatus.FINISHED);

                // CHỐT 1: Lệnh DB chốt người thắng (Không dùng mapper nữa)
                repository.finalizeAuction(auction.getId(), AuctionStatus.FINISHED, winner, closedAt);

                // CHỐT 2: Lệnh DB chuyển sổ đỏ món hàng
                schedulePaymentTimeout(auction);

            } else {
                auction.setStatus(AuctionStatus.FAILED);
                repository.finalizeAuction(auction.getId(), AuctionStatus.FAILED, null, closedAt);
                activeAuctions.remove(auction.getId());
            }

            System.out.println("[Service] Đã chốt đơn thành công phiên ID: " + auction.getId());

            AuctionEvent closeEvent = new AuctionEvent(EventType.AUCTION_CLOSED, auction, null, LocalDateTime.now(), "Phiên đấu giá kết thúc");
            notifyObservers(closeEvent);

        } catch (Exception e) {
            System.err.println("❌ LỖI CHỐT ĐƠN PHIÊN " + auction.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public AuctionRepository getAuctionRepository() {
        return repository;
    }

    private void schedulePaymentTimeout(Auction auction) {
        cancelPaymentTimer(auction.getId());

        long delaySeconds = ChronoUnit.SECONDS.between(
                LocalDateTime.now(),
                getPaymentDeadline(auction.getEndTime())
        );

        Runnable expireTask = () -> expireUnpaidAuction(auction.getId());
        if (delaySeconds <= 0) {
            expireTask.run();
            return;
        }

        paymentTimers.put(
                auction.getId(),
                scheduler.schedule(expireTask, delaySeconds, TimeUnit.SECONDS)
        );
    }

    private LocalDateTime getPaymentDeadline(LocalDateTime closedAt) {
        LocalDateTime baseTime = closedAt != null ? closedAt : LocalDateTime.now();
        return baseTime.plusMinutes(PAYMENT_TIMEOUT_MINUTES);
    }

    private void expireUnpaidAuction(String auctionId) {
        AuctionEntity entity = repository.findByIdWithDetails(auctionId);
        if (entity == null || entity.getStatus() != AuctionStatus.FINISHED || entity.getCurrentWinner() == null) {
            paymentTimers.remove(auctionId);
            return;
        }
        if (false && isItemAlreadyTransferredToWinner(entity)) {
            repository.finalizeAuction(auctionId, AuctionStatus.PAID, entity.getCurrentWinner());
            activeAuctions.remove(auctionId);
            paymentTimers.remove(auctionId);
            return;
        }

        Auction active = activeAuctions.get(auctionId);
        if (active != null) {
            active.setCurrentWinner(null);
            active.setStatus(AuctionStatus.FAILED);
        }
        repository.finalizeAuction(auctionId, AuctionStatus.FAILED, null);
        activeAuctions.remove(auctionId);
        paymentTimers.remove(auctionId);
        System.out.println("[Service] Qua han thanh toan phien " + auctionId + ", da cho phep dau gia lai.");
    }

    public void payWonAuction(String auctionId, User payer) {
        if (payer == null) {
            throw new RuntimeException("Bạn phải đăng nhập để thanh toán.");
        }

        AuctionEntity entity = repository.findByIdWithDetails(auctionId);
        if (entity == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá.");
        }
        if (entity.getStatus() != AuctionStatus.FINISHED || entity.getCurrentWinner() == null) {
            throw new RuntimeException("Phiên đấu giá này không còn trong trạng thái chờ thanh toán.");
        }
        if (!payer.getId().equals(entity.getCurrentWinner().getId())) {
            throw new RuntimeException("Chỉ người chiến thắng mới được thanh toán phiên này.");
        }

        if (LocalDateTime.now().isAfter(getPaymentDeadline(entity.getEndTime()))) {
            expireUnpaidAuction(auctionId);
            throw new RuntimeException("Đã quá hạn thanh toán 10 phút. Sản phẩm được trả lại cho người bán.");
        }

        User seller;
        if (baoVe == null) {
            throw new RuntimeException("Lỗi hệ thống: chưa kết nối hệ thống ví.");
        }
        User freshPayer = baoVe.findById(payer.getId());
        if (freshPayer == null) {
            throw new RuntimeException("Khong tim thay tai khoan thanh toan.");
        }
        seller = baoVe.findById(entity.getItem().getSellerId());
        if (seller == null) {
            throw new RuntimeException("Không tìm thấy người bán để thanh toán.");
        }

        double paymentAmount = entity.getCurrentPrice();
        baoVe.deductBalance(freshPayer, paymentAmount);
        payer.setBalance(freshPayer.getBalance());

        try {
            baoVe.addBalance(seller, entity.getCurrentPrice());

            quanLyKho.changeItemOwner(entity.getItem().getId(), entity.getCurrentWinner());
            repository.finalizeAuction(auctionId, AuctionStatus.PAID, entity.getCurrentWinner());

            Auction active = activeAuctions.get(auctionId);
            if (active != null) {
                active.getItem().setSellerId(entity.getCurrentWinner().getId());
                active.getItem().setSellerFullName(entity.getCurrentWinner().getFullName());
                active.setStatus(AuctionStatus.PAID);
            }
            activeAuctions.remove(auctionId);
            cancelPaymentTimer(auctionId);
        } catch (RuntimeException e) {
            baoVe.addBalance(freshPayer, paymentAmount);
            payer.setBalance(freshPayer.getBalance());
            throw e;
        }
    }

    public long countAuctionsCreatedBySeller(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return 0;
        }
        return repository.countBySellerId(sellerId);
    }

    public long countSuccessfulAuctionsBySeller(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return 0;
        }
        return repository.countBySellerIdAndStatuses(sellerId, List.of(AuctionStatus.PAID));
    }

    public long countCanceledAuctionsBySeller(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return 0;
        }
        return repository.countBySellerIdAndStatuses(sellerId, List.of(AuctionStatus.CANCELED, AuctionStatus.FAILED));
    }

    public long countWonItemsByUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0;
        }
        return repository.countWonByUserId(userId);
    }

    public void forceCloseManual(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            throw new RuntimeException("Không tìm thấy phiên đấu giá này trên sàn!");
        }
        cancelAuctionByAdmin(auction);
    }

    private void cancelAuctionByAdmin(Auction auction) {
        cancelAuctionTimers(auction.getId());

        auction.setCurrentWinner(null);
        auction.setStatus(AuctionStatus.CANCELED);
        repository.finalizeAuction(auction.getId(), AuctionStatus.CANCELED, null);
        activeAuctions.remove(auction.getId());

        AuctionEvent closeEvent = new AuctionEvent(
                EventType.AUCTION_CLOSED,
                auction,
                null,
                LocalDateTime.now(),
                "Phiên đấu giá đã bị admin buộc dừng"
        );
        notifyObservers(closeEvent);
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

            try {
                auction.setCurrentWinner(bidder);

                BiddingTransactionEntity txEntity = createBidEntity(finalPrice, bidder);
                txEntity.setStatus(com.bidding.server.enums.BidStatus.ACCEPTED); // Đủ trạng thái

                // 1. Lưu lịch sử đặt giá (Bây giờ đã dùng merge() nên cực mượt, không sập nữa)
                repository.saveNewBid(auction.getId(), finalPrice, bidder, auction.getEndTime(), txEntity);

                System.out.println("[Reverse Auction] Đại gia " + bidder.getUsername() + " đã chốt đơn với giá: " + finalPrice);

                // 2. Gọi hàm đóng phiên ngay trên luồng này, an toàn 100%
                closeAuction(auction);

                return;

            } catch (Exception e) {
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
        LocalDateTime oldEndTime = auction.getEndTime();

        validateBidAmount(auction, amount);

        try {
            BiddingTransaction tx = auction.placeBid(bidder, amount, false);

            if (auction.getEndTime().isAfter(oldEndTime)) {
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) oldTimer.cancel(false);
                scheduleAuctionEnd(auction);
            }

            BiddingTransactionEntity txEntity = createBidEntity(tx, bidder);

            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);

            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, java.time.LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);

            triggerAutoBids(auction);
        } catch (Exception e) {
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
                double nextBid = Math.max(
                        bot.getNextBidAmount(currentPrice),
                        currentPrice + getRequiredBidStep(auction)
                );

                if (nextBid <= bot.getMaxBid()) {
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

    private double getRequiredBidStep(Auction auction) {
        if (auction == null || auction.getItem() == null) {
            return 0.0;
        }

        Item item = auction.getItem();
        double bidStep = item.getBidStep();
        if (bidStep <= 0 && item.getId() != null) {
            Item freshItem = quanLyKho.findById(item.getId());
            if (freshItem != null) {
                bidStep = freshItem.getBidStep();
                item.setBidStep(bidStep);
            }
        }
        return Math.max(0.0, bidStep);
    }

    private void validateBidAmount(Auction auction, double amount) {
        double currentPrice = auction.getCurrentPrice().get();
        double bidStep = getRequiredBidStep(auction);

        if (bidStep <= 0) {
            throw new RuntimeException("Phiên đấu giá chưa có bước giá hợp lệ.");
        }

        double minimumBid = currentPrice + bidStep;
        if (amount < minimumBid) {
            throw new RuntimeException(
                    "Giá đặt phải tối thiểu " + String.format("%,.0f", minimumBid)
                            + " đ (giá hiện tại + bước giá)."
            );
        }
    }

    private void executeInternalBid(Auction auction, double amount, User bidder) {
        validateBidAmount(auction, amount);

        try {
            BiddingTransaction tx = auction.placeBid(bidder, amount, true);

            if (tx.isExtended()) {
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) oldTimer.cancel(false);
                scheduleAuctionEnd(auction);
            }

            BiddingTransactionEntity txEntity = createBidEntity(tx, bidder);

            repository.saveNewBid(auction.getId(), auction.getCurrentPrice().get(), bidder, auction.getEndTime(), txEntity);

            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, java.time.LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);

        } catch (Exception e) {
            throw e;
        }
    }

    private BiddingTransactionEntity createBidEntity(BiddingTransaction tx, User bidder) {
        BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
        txEntity.setId(tx.getId());
        txEntity.setBidAmount(tx.getBidAmount());
        txEntity.setBidTime(tx.getBidTime());
        txEntity.setAutoBid(tx.isAutoBid());
        txEntity.setExtended(tx.isExtended());
        txEntity.setStatus(tx.getStatus());
        txEntity.setBidder(bidder);
        return txEntity;
    }

    private BiddingTransactionEntity createBidEntity(double amount, User bidder) {
        BiddingTransactionEntity txEntity = new BiddingTransactionEntity();
        txEntity.setId(System.currentTimeMillis());
        txEntity.setBidAmount(amount);
        txEntity.setBidTime(LocalDateTime.now());
        txEntity.setAutoBid(false);
        txEntity.setExtended(false);
        txEntity.setBidder(bidder);
        txEntity.setStatus(BidStatus.ACCEPTED);
        return txEntity;
    }

    public boolean isItemInActiveAuction(String itemId) {
        for (Auction auction : activeAuctions.values()) {
            if (auction.getItem() != null && auction.getItem().getId().equals(itemId)) return true;
        }
        return false;
    }

    public Auction findActiveAuctionByItemId(String itemId) {
        for (Auction auction : activeAuctions.values()) {
            if (auction.getItem() != null && auction.getItem().getId().equals(itemId)) {
                return auction;
            }
        }
        return null;
    }

    public boolean isItemInAnyAuction(String itemId) {
        if (isItemInActiveAuction(itemId)) {
            return true;
        }
        return repository.existsByItemId(itemId);
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
        if (!item.isApprovedForAuction()) throw new RuntimeException("San pham chua duoc admin duyet.");
        validateAuctionItemBidStep(item);
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
        if (!item.isApprovedForAuction()) throw new RuntimeException("San pham chua duoc admin duyet.");
        validateAuctionItemBidStep(item);
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

    private void validateAuctionItemBidStep(Item item) {
        if (item == null) {
            throw new RuntimeException("Không tìm thấy sản phẩm.");
        }
        if (item.getBidStep() <= 0) {
            throw new RuntimeException("Sản phẩm chưa có bước giá hợp lệ. Vui lòng cập nhật bước giá trước khi mở phiên.");
        }
        if (item.getBidStep() > item.getStartingPrice()) {
            throw new RuntimeException("Bước giá không được lớn hơn giá trị sản phẩm.");
        }
    }
}
