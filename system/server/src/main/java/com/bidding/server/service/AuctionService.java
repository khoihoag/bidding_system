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
import java.util.ArrayList; // Bổ sung thư viện này
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.bidding.server.enums.AuctionStatus;
public class AuctionService {
    private final AuctionRepository repository = new AuctionRepository();
    private final AuctionMapper mapper = AuctionMapper.INSTANCE;
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private UserService baoVe;
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> auctionTimers = new ConcurrentHashMap<>();
    // =======================================================
    // VŨ KHÍ MỚI: BỘ ĐẾM THỜI GIAN TỰ ĐỘNG ĐÓNG PHIÊN
    // =======================================================
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    public void setUserService(UserService userService) {
        this.baoVe = userService;
    }
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(AuctionEvent event) {
        for (AuctionObserver obs : observers) {
            if (event.getEventType() == EventType.BID_PLACED) {
                obs.onBidPlaced(event);
            } else if (event.getEventType() == EventType.AUCTION_CLOSED) {
                obs.onAuctionClosed(event);
            } else if (event.getEventType() == EventType.AUCTION_STARTED) {
                // THÊM DÒNG NÀY: Để truyền tin đi khi phiên vừa bắt đầu
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

            // Nạp lên RAM xong là gài đồng hồ đếm ngược cho nó luôn
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                scheduleAuctionEnd(auction);
            }
            else if (auction.getStatus() == AuctionStatus.OPEN) {
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

        // Lấy phiếu hẹn giờ và cất vào kho
        java.util.concurrent.ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
            closeAuction(auction);
            auctionTimers.remove(auction.getId()); // Đóng xong thì vứt phiếu đi
        }, delaySeconds, TimeUnit.SECONDS);

        auctionTimers.put(auction.getId(), timerTask);
    }
    private void handleOpenAuctionOnStartup(Auction auction) {
        long secondsToStart = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getStartTime());

        if (secondsToStart <= 0) {
            // 1. Chuyển trạng thái trên RAM
            auction.start();

            // 2. !!! QUAN TRỌNG: Cập nhật ngay xuống DB !!!
            AuctionEntity entityToSave = mapper.toEntity(auction);
            repository.saveOrUpdate(entityToSave);

            System.out.println("[Service] Đã đồng bộ trạng thái RUNNING xuống DB cho phiên: " + auction.getId());

            // 3. Gài đồng hồ đóng phiên
            scheduleAuctionEnd(auction);
        } else {
            // Trường hợp tương lai: Khi nào scheduler chạy thì nó cũng phải làm tương tự
            scheduler.schedule(() -> {
                auction.start();

                // Đồng bộ DB khi đến giờ mở cửa
                repository.saveOrUpdate(mapper.toEntity(auction));

                scheduleAuctionEnd(auction);

                // Bắn thông báo cho Client...
                AuctionEvent startEvent = new AuctionEvent(EventType.AUCTION_STARTED, auction, null, LocalDateTime.now(), "Phiên đấu giá bắt đầu!");
                notifyObservers(startEvent);
            }, secondsToStart, TimeUnit.SECONDS);
        }
    }

    private void closeAuction(Auction auction) {
        try {
            // Hàm end() đã có sẵn block synchronized bên trong class Auction
            auction.end();

            // Lưu trạng thái FINISHED xuống Database
            AuctionEntity entityToSave = mapper.toEntity(auction);
            repository.saveOrUpdate(entityToSave);
            System.out.println("[Service] Đã tự động đóng phiên ID: " + auction.getId());

            // Bắn loa phường báo cho mọi người biết phiên đã kết thúc
            AuctionEvent closeEvent = new AuctionEvent(
                    EventType.AUCTION_CLOSED,
                    auction,
                    null,
                    LocalDateTime.now(),
                    "Phiên đấu giá kết thúc"
            );
            notifyObservers(closeEvent);

            // Tùy chọn: Xóa khỏi RAM nếu không muốn chứa nhiều (Hoặc giữ lại để Client xem lịch sử)
            // activeAuctions.remove(auction.getId());

        } catch (Exception e) {
            System.err.println("Lỗi khi tự động đóng phiên: " + auction.getId());
            e.printStackTrace();
        }
    }

    // ================= XỬ LÝ ĐẶT GIÁ =================
    // ================= XỬ LÝ ĐẶT GIÁ =================
    public void placeBid(String auctionId, Double amount, User bidder) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            throw new RuntimeException("Phiên đấu giá ID " + auctionId + " không tồn tại.");
        }
        if (baoVe == null) {
            throw new RuntimeException("Lỗi hệ thống: Chưa kết nối hệ thống Ngân hàng (UserService)!");
        }

        // 1. NHỚ MẶT THẰNG CŨ VÀ GIỜ CŨ TRƯỚC KHI ĐẶT GIÁ
        User oldWinner = auction.getCurrentWinner();
        Double oldPrice = auction.getCurrentPrice().get();
        LocalDateTime oldEndTime = auction.getEndTime(); // <<< CHÚ Ý DÒNG NÀY

        // 2. TẠM GIỮ CỌC
        baoVe.deductBalance(bidder, amount);

        try {
            // 3. THỰC HIỆN ĐẶT GIÁ
            BiddingTransaction tx = auction.placeBid(bidder, amount,false);

            // 4. CHECK ANTI-SNIPE ĐỂ LẬP LẠI ĐỒNG HỒ
            if (auction.getEndTime().isAfter(oldEndTime)) {
                System.out.println("[Service] Phát hiện Anti-snipe! Đang thiết lập lại đồng hồ...");

                // Lôi báo thức cũ ra hủy bỏ ngay lập tức (Xé phiếu)
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) {
                    oldTimer.cancel(false);
                }

                // Đặt lại báo thức mới với giờ mới
                scheduleAuctionEnd(auction);
            }


            // 5. HOÀN TIỀN CHO NGƯỜI BỊ VƯỢT GIÁ
            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                baoVe.addBalance(oldWinner, oldPrice);
                System.out.println("[Hoàn Tiền] Đã trả lại " + oldPrice + " cho " + oldWinner.getUsername() + " vì bị vượt giá.");
            }

            // 6. ĐỒNG BỘ DB VÀ BẮN SỰ KIỆN
            AuctionEntity entityToSave = mapper.toEntity(auction);
            repository.saveOrUpdate(entityToSave);

            AuctionEvent event = new AuctionEvent(EventType.BID_PLACED, auction, tx, LocalDateTime.now(), "Mức giá mới");
            notifyObservers(event);
            triggerAutoBids(auction);
        } catch (Exception e) {
            // ROLLBACK
            baoVe.addBalance(bidder, amount);
            System.out.println("[Rollback] Đã hoàn lại tiền cọc cho " + bidder.getUsername() + " vì đặt giá thất bại.");
            throw e;
        }
    }
    public boolean isItemInActiveAuction(String itemId) {
        // Duyệt qua toàn bộ các phiên đấu giá đang chạy trên RAM
        for (Auction auction : activeAuctions.values()) {
            // Nếu tìm thấy phiên nào chứa cái Item ID này thì báo True ngay
            if (auction.getItem() != null && auction.getItem().getId().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    // =======================================================
    // HÀM MỚI: LẤY DANH SÁCH PHIÊN ĐẤU GIÁ CHO CLIENT
    // =======================================================
    public List<Auction> getActiveAuctions() {
        // Lôi tất cả các value (Auction) từ trong ConcurrentHashMap ra rồi tống vào 1 cái ArrayList
        return new ArrayList<>(activeAuctions.values());
    }
    // Thêm hàm này vào AuctionService.java
    public void forceCloseManual(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {

            throw new RuntimeException("Không tìm thấy phiên đấu giá này trên sàn!");
        }

        // Gọi lại hàm đóng phiên chuẩn mà ông đã viết
        closeAuction(auction);
    }
    // =======================================================
    // HÀM MỚI: BẮT ĐẦU MỘT PHIÊN ĐẤU GIÁ MỚI
    // =======================================================
    public Auction startAuction(Item item, int durationMinutes) {
        if (isItemInActiveAuction(item.getId())) {
            throw new RuntimeException("Sản phẩm này đang được đấu giá rồi!");
        }

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        // 1. Tạo Model với đúng 6 tham số (id=null, item, start, end, antiSnipe=30, extension=60)
        // Tui để mặc định 30 giây chống bắn tỉa và 60 giây gia hạn cho ông nhé
        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);

        // 2. !!! QUAN TRỌNG !!!
        // Phải gọi start() để status chuyển từ OPEN -> RUNNING
        // Nếu không thì hàm placeBid() trong Auction.java sẽ chặn không cho đặt giá
        newAuction.start();

        // 3. Chuyển sang Entity và lưu xuống Database
        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);

        // Cập nhật lại ID thực tế từ DB cho Model (nếu ID do DB sinh ra)
        newAuction.setId(entityToSave.getId());

        // 4. Nạp lên RAM và kích hoạt đếm ngược
        activeAuctions.put(newAuction.getId(), newAuction);
        scheduleAuctionEnd(newAuction);

        System.out.println("[Service] Đã mở phiên đấu giá cho: " + item.getName() + " (Status: RUNNING)");
        return newAuction;
    }

    public Auction scheduleAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. Kiểm tra xem món đồ có đang "bận" ở phiên khác không
        if (isItemInActiveAuction(item.getId())) {
            throw new RuntimeException("Sản phẩm này đang nằm trong một phiên đấu giá khác!");
        }

        // 2. Validate thời gian do Client gửi lên
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian bắt đầu không được ở trong quá khứ!");
        }
        if (endTime.isBefore(startTime)) {
            throw new RuntimeException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
        }

        // 3. Tạo Model đấu giá (Trạng thái mặc định là OPEN)
        // Truyền tham số: ID=null, item, start, end, anti-snipe=30s, extension=60s
        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);

        // 4. Lưu xuống Database để lấy ID chính thức và giữ trạng thái OPEN
        AuctionEntity entityToSave = mapper.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        // 5. Đưa lên RAM để quản lý tập trung
        activeAuctions.put(newAuction.getId(), newAuction);

        // 6. TÍNH TOÁN GIÂY CHỜ ĐỂ ĐẶT BÁO THỨC MỞ SÀN
        long secondsToStart = ChronoUnit.SECONDS.between(LocalDateTime.now(), startTime);

        scheduler.schedule(() -> {
            try {
                System.out.println("[Service] Đã tới giờ G! Kích hoạt phiên: " + newAuction.getId());

                // Chuyển trạng thái OPEN -> RUNNING
                newAuction.start();

                // Đồng bộ trạng thái mới xuống Database ngay lập tức
                repository.saveOrUpdate(mapper.toEntity(newAuction));

                // Gài tiếp đồng hồ để ĐÓNG SÀN khi hết giờ
                scheduleAuctionEnd(newAuction);

                // Bắn tin cho toàn bộ Client biết sàn đã mở cửa
                AuctionEvent startEvent = new AuctionEvent(
                        EventType.AUCTION_STARTED,
                        newAuction,
                        null,
                        LocalDateTime.now(),
                        "Phiên đấu giá cho " + item.getName() + " chính thức bắt đầu!"
                );
                notifyObservers(startEvent);

            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng khi kích hoạt phiên đấu giá tự động!");
                e.printStackTrace();
            }
        }, secondsToStart, TimeUnit.SECONDS);

        return newAuction;
    }
    private void triggerAutoBids(Auction auction) {
        boolean hasNewBid;
        do {
            hasNewBid = false;
            // Lấy danh sách bot từ "Chuồng" (Hàng đợi ưu tiên)
            var queue = auction.getAutoBidQueue();

            for (AutoBidConfig bot : queue) {
                // 1. Bot còn hoạt động không?
                if (!bot.isActive()) continue;

                // 2. Đừng tự đè giá chính mình (Nếu đang thắng thì thôi)
                if (auction.getCurrentWinner() != null &&
                        auction.getCurrentWinner().getId().equals(bot.getBidder().getId())) {
                    continue;
                }

                double currentPrice = auction.getCurrentPrice().get();
                double nextBid = bot.getNextBidAmount(currentPrice);

                // 3. Bot còn đủ "máu" (Max Bid) để chơi không?
                if (bot.canBid(currentPrice)) {
                    try {
                        System.out.println("[Auto-Bid] Bot của " + bot.getBidder().getUsername() + " đang tung đòn: " + nextBid);

                        // THỰC THI ĐẶT GIÁ TỰ ĐỘNG
                        // Ở đây ta gọi lại chính logic placeBid nhưng với cờ isAutoBid = true
                        executeInternalBid(auction, nextBid, bot.getBidder());

                        hasNewBid = true;
                        break; // Sau 1 cú bid thành công, ta break để quét lại từ đầu hàng đợi (Ưu tiên người đăng ký trước)
                    } catch (Exception e) {
                        // Nếu bot hụt tiền trong ví hoặc gặp lỗi, cho bot "nghỉ hưu" luôn
                        bot.deactivate();
                    }
                }
            }
        } while (hasNewBid);
    }
    // Hàm phụ để xử lý các thủ tục tài chính cho Bot
    // Hàm phụ để xử lý các thủ tục tài chính cho Bot
    private void executeInternalBid(Auction auction, double amount, User bidder) {
        User oldWinner = auction.getCurrentWinner();
        Double oldPrice = auction.getCurrentPrice().get();
        LocalDateTime oldEndTime = auction.getEndTime();

        // 1. Bot xuất tiền cọc
        baoVe.deductBalance(bidder, amount);

        try {
            // 2. Bot tung đòn (Chú ý: isAutoBid = true)
            BiddingTransaction tx = auction.placeBid(bidder, amount, true);

            // 3. Check xem đòn của Bot có gây gia hạn thời gian không
            if (tx.isExtended()) {
                System.out.println("[Service] Bot đè giá sát giờ! Đang thiết lập lại đồng hồ...");
                java.util.concurrent.ScheduledFuture<?> oldTimer = auctionTimers.get(auction.getId());
                if (oldTimer != null) oldTimer.cancel(false);
                scheduleAuctionEnd(auction);
            }

            // 4. Hoàn tiền cho kẻ chiến bại
            if (oldWinner != null && !oldWinner.getId().equals(bidder.getId())) {
                baoVe.addBalance(oldWinner, oldPrice);
            }

            // 5. Cất lịch sử trận đánh xuống Database
            repository.saveOrUpdate(mapper.toEntity(auction));

            // 6. BẮN LOA PHƯỜNG BÁO CHO TOÀN BỘ CLIENT CẬP NHẬT GIAO DIỆN
            AuctionEvent event = new AuctionEvent(
                    EventType.BID_PLACED, auction, tx, LocalDateTime.now(), "Bot tự động đặt giá: " + amount
            );
            notifyObservers(event);

        } catch (Exception e) {
            // Bot đánh xịt thì trả tiền lại cho nó
            baoVe.addBalance(bidder, amount);
            throw e;
        }
    }
}