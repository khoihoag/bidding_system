package com.bidding.server.network;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.auction.AuctionMapper;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.model.transaction.BiddingTransaction;
import com.bidding.server.events.AuctionEvent;
import com.bidding.server.enums.EventType;
import com.bidding.server.model.user.User;

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

    // ================= LOGIC ĐẾM NGƯỢC (HẤP THỤ TỪ BẠN ÔNG) =================
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
}