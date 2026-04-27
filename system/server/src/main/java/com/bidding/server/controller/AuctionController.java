package com.bidding.server.controller;

import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.ClientManager;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.service.AuctionService;
import com.bidding.server.service.ItemService;
import com.bidding.server.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionController {
    private final ClientHandler client;
    private final AuctionService tongQuan;
    private final ItemService quanLyKho;
    private final ClientManager loaPhuong;
    private final UserService baoVe;
    private final Gson gson = new Gson();

    public AuctionController(ClientHandler client, AuctionService tongQuan, ItemService quanLyKho, ClientManager loaPhuong, UserService baoVe) {
        this.client = client;
        this.tongQuan = tongQuan;
        this.quanLyKho = quanLyKho;
        this.loaPhuong = loaPhuong;
        this.baoVe = baoVe;
    }

    public void handleStartAuction(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Phải đăng nhập mới được mở phiên đấu giá!");
            return;
        }
        try {
            String itemId = request.get("itemId").getAsString();
            int duration = request.get("durationMinutes").getAsInt();

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Không tìm thấy sản phẩm này!"); return; }
            if (!item.getSeller().getId().equals(client.getLoggedInUser().getId())) { client.sendError("Bạn không thể mang đồ của người khác đi đấu giá!"); return; }

            Auction newAuction = tongQuan.startAuction(item, duration);
            client.sendMessage("{\"action\": \"START_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");
            loaPhuong.broadcast(String.format("{\"action\": \"GLOBAL_NOTIFY\", \"message\": \"Món hàng mới lên sàn: %s!\"}", item.getName()));

        } catch (Exception e) {
            client.sendError("Lỗi khi mở phiên đấu giá: " + e.getMessage());
        }
    }

    public void handleScheduleAuction(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập mới được đặt lịch đấu giá!");
            return;
        }
        try {
            String itemId = request.get("itemId").getAsString();
            LocalDateTime startTime = LocalDateTime.parse(request.get("startTime").getAsString());
            LocalDateTime endTime = LocalDateTime.parse(request.get("endTime").getAsString());

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Sản phẩm không tồn tại trong kho!"); return; }
            if (!item.getSeller().getId().equals(client.getLoggedInUser().getId())) { client.sendError("Chỉ chính chủ mới được mang đồ đi đấu giá!"); return; }

            Auction newAuction = tongQuan.scheduleAuction(item, startTime, endTime);
            client.sendMessage("{\"action\": \"SCHEDULE_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");

        } catch (java.time.format.DateTimeParseException e) {
            client.sendError("Lỗi định dạng thời gian. Vui lòng dùng chuẩn yyyy-MM-ddTHH:mm:ss");
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public void handleGetAuctions() {
        try {
            List<Auction> activeAuctions = tongQuan.getActiveAuctions();
            client.sendMessage("{\"action\": \"AUCTIONS_LIST\", \"data\": " + gson.toJson(activeAuctions) + "}");
        } catch (Exception e) {
            client.sendError("Lỗi khi tải danh sách đấu giá: " + e.getMessage());
        }
    }

    public void handleBid(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Chưa đăng nhập mà đòi đấu giá à?");
            return;
        }
        try {
            String itemId = request.get("itemId").getAsString();
            double amount = request.get("amount").getAsDouble();
            tongQuan.placeBid(itemId, amount, client.getLoggedInUser());
            client.sendMessage("{\"action\": \"BID_REPLY\", \"status\": \"SUCCESS\"}");
        } catch (Exception e) {
            client.sendError(e.getMessage());
        }
    }

    public void handleGetHistory() {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để xem lịch sử!");
            return;
        }
        List<BiddingTransactionEntity> history = baoVe.getBidHistory(client.getLoggedInUser().getId());
        client.sendMessage("{\"action\": \"HISTORY_REPLY\", \"data\": " + gson.toJson(history) + "}");
    }
    public void handleRegisterAutoBid(JsonObject request) {
        // 1. Chặn cửa những thằng chưa login
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập mới được cài đặt đấu giá tự động!");
            return;
        }

        try {
            // 2. Bóc tách dữ liệu từ JSON gửi lên
            String auctionId = request.get("auctionId").getAsString();
            double maxBid = request.get("maxBid").getAsDouble();
            double increment = request.get("increment").getAsDouble();

            // 3. Tìm đúng phiên đấu giá đang chạy trên RAM
            // Chúng ta dùng stream() để lọc nhanh trong danh sách activeAuctions
            Auction auction = tongQuan.getActiveAuctions().stream()
                    .filter(a -> a.getId().equals(auctionId))
                    .findFirst()
                    .orElse(null);

            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này trên sàn!");
                return;
            }

            // 4. Kích hoạt lệnh nạp Bot vào "Chuồng"
            // User chính là người đang giữ kết nối này
            auction.registerAutoBid(client.getLoggedInUser(), maxBid, increment);

            // 5. Báo tin vui về cho Client
            client.sendMessage("{\"action\": \"REGISTER_AUTO_BID_REPLY\", \"status\": \"SUCCESS\"}");
            System.out.println("[Auto-Bid] Đã nạp cấu hình Bot cho User: " + client.getLoggedInUser().getUsername());

        } catch (Exception e) {
            client.sendError("Lỗi cài đặt Auto-Bid: " + e.getMessage());
        }
    }
}