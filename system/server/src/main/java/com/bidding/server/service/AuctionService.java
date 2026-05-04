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
import java.util.ArrayList; // Bổ sung thư viện này
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

    // =======================================================
    // VŨ KHÍ MỚI: BỘ ĐẾM THỜI GIAN TỰ ĐỘNG ĐÓNG PHIÊN
    // =======================================================
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

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
            activeAuctions.put(auction.getId(), auction);

            // Nạp lên RAM xong là gài đồng hồ đếm ngược cho nó luôn
            scheduleAuctionEnd(auction);
        }
        System.out.println("[Service] Đã nạp " + activeAuctions.size() + " phiên đấu giá và gài đồng hồ.");
    }

    // ================= LOGIC ĐẾM NGƯỢC =================
    private void scheduleAuctionEnd(Auction auction) {
        long delaySeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());

        if (delaySeconds <= 0) {
            closeAuction(auction); // Hết giờ rồi thì đóng luôn
            return;
        }

        scheduler.schedule(() -> {
            closeAuction(auction);
        }, delaySeconds, TimeUnit.SECONDS);
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
    public void placeBid(String auctionId, Double amount, User bidder) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            throw new RuntimeException("Phiên đấu giá ID " + auctionId + " không tồn tại.");
        }

        BiddingTransaction tx = auction.placeBid(bidder, amount);

        AuctionEntity entityToSave = mapper.toEntity(auction);
        repository.saveOrUpdate(entityToSave);
        System.out.println("[Service] DB Sync thành công - Auction ID: " + auctionId + " | Price: " + amount);

        AuctionEvent event = new AuctionEvent(
                EventType.BID_PLACED,
                auction,
                tx,
                LocalDateTime.now(),
                "Mức giá mới"
        );
        notifyObservers(event);
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
        // Trả tất cả phiên đang quản lý (RUNNING + OPEN/UPCOMING)
        // Client sẽ tự phân loại theo status
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

    /**
     * Lên lịch phiên đấu giá bắt đầu tại thời điểm cụ thể trong tương lai.
     * Auction được tạo với status=OPEN, sẽ tự chuyển sang RUNNING khi đến startTime.
     */
    public Auction scheduleAuction(Item item, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (isItemInActiveAuction(item.getId())) {
            throw new RuntimeException("Sản phẩm này đang được đấu giá hoặc đã lên lịch rồi!");
        }

        // Tạo Auction với trạng thái OPEN (chưa bắt đầu)
        Auction newAuction = new Auction(null, item, startTime, endTime, 30, 60);
        // Không gọi start() — giữ status=OPEN (UPCOMING)

        AuctionEntity entityToSave = AuctionMapper.INSTANCE.toEntity(newAuction);
        repository.saveOrUpdate(entityToSave);
        newAuction.setId(entityToSave.getId());

        // Nạp lên RAM
        activeAuctions.put(newAuction.getId(), newAuction);

        // Lên lịch tự động START và END
        long delayToStart = java.time.temporal.ChronoUnit.SECONDS.between(java.time.LocalDateTime.now(), startTime);
        if (delayToStart > 0) {
            scheduler.schedule(() -> {
                newAuction.start();
                // Lưu DB khi bắt đầu
                AuctionEntity e2 = AuctionMapper.INSTANCE.toEntity(newAuction);
                repository.saveOrUpdate(e2);
                System.out.println("[Service] Phiên lên lịch đã BẮT ĐẦU: " + newAuction.getId());
                // Gài đồng hồ kết thúc
                scheduleAuctionEnd(newAuction);
            }, delayToStart, java.util.concurrent.TimeUnit.SECONDS);
        } else {
            // Nếu startTime đã qua → bắt đầu ngay
            newAuction.start();
            scheduleAuctionEnd(newAuction);
        }

        System.out.println("[Service] Đã lên lịch phiên đấu giá cho: " + item.getName()
                + " | Bắt đầu: " + startTime + " | Kết thúc: " + endTime);
        return newAuction;
    }
}