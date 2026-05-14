package com.bidding.server.controller;
import com.bidding.server.model.transaction.BiddingTransaction;
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
import com.bidding.server.utils.GsonUtil;
public class AuctionController {
    private final ClientHandler client;
    private final AuctionService tongQuan;
    private final ItemService quanLyKho;
    private final ClientManager loaPhuong; // Vẫn giữ để không vỡ cấu trúc gọi hàm từ ClientHandler
    private final UserService baoVe;
    Gson gson = GsonUtil.getInstance();

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

            // ================= BẮT THÔNG SỐ ĐẤU GIÁ NGƯỢC =================
            // Dùng has() để tránh lỗi nếu UI cũ chưa kịp cập nhật gửi lên
            boolean isReverse = request.has("isReverse") ? request.get("isReverse").getAsBoolean() : false;
            double dropStep = request.has("dropStep") ? request.get("dropStep").getAsDouble() : 0.0;
            // ==============================================================

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Không tìm thấy sản phẩm này!"); return; }

            // ================= BỌC THÉP LỖI DỮ LIỆU RÁC =================
            if (item.getSeller() == null) {
                client.sendError("Món đồ này bị lỗi dữ liệu cũ (Không có chủ). Sếp hãy xóa đi và Add 1 món đồ mới tinh vào kho nhé!");
                return;
            }
            if (!item.getSeller().getId().equals(client.getLoggedInUser().getId())) {
                client.sendError("Bạn không thể mang đồ của người khác đi đấu giá!");
                return;
            }
            // ============================================================

            // GỌI HÀM VỚI ĐỦ 4 THAM SỐ MỚI
            Auction newAuction = tongQuan.startAuction(item, duration, isReverse, dropStep);

            client.sendMessage("{\"action\": \"START_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");

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

            // ================= BẮT THÔNG SỐ ĐẤU GIÁ NGƯỢC =================
            boolean isReverse = request.has("isReverse") ? request.get("isReverse").getAsBoolean() : false;
            double dropStep = request.has("dropStep") ? request.get("dropStep").getAsDouble() : 0.0;
            // ==============================================================

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Không tìm thấy sản phẩm này!"); return; }

            // ================= BỌC THÉP LỖI DỮ LIỆU RÁC =================
            if (item.getSeller() == null) {
                client.sendError("Món đồ này bị lỗi dữ liệu cũ (Không có chủ). Sếp hãy xóa đi và Add 1 món đồ mới tinh vào kho nhé!");
                return;
            }
            if (!item.getSeller().getId().equals(client.getLoggedInUser().getId())) {
                client.sendError("Bạn không thể mang đồ của người khác đi đấu giá!");
                return;
            }
            // ============================================================

            // GỌI HÀM VỚI ĐỦ 5 THAM SỐ MỚI
            Auction newAuction = tongQuan.scheduleAuction(item, startTime, endTime, isReverse, dropStep);

            client.sendMessage("{\"action\": \"SCHEDULE_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");

        } catch (java.time.format.DateTimeParseException e) {
            client.sendError("Lỗi định dạng thời gian. Vui lòng dùng chuẩn yyyy-MM-ddTHH:mm:ss");
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public void handleGetAuctions() {
        try {
            // Lấy danh sách bằng đúng hàm của sếp
            List<Auction> activeAuctions = tongQuan.getActiveAuctions();

            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();

            for (Auction auc : activeAuctions) {
                JsonObject aucObj = new JsonObject();
                aucObj.addProperty("id", auc.getId());

                aucObj.addProperty("currentPrice", auc.getCurrentPrice().get());
                aucObj.addProperty("status", auc.getStatus() != null ? auc.getStatus().toString() : "UNKNOWN");
                // Trong AuctionController.java
                // ================= BỌC THÉP LỖI THỜI GIAN =================
                aucObj.addProperty("startTime", auc.getStartTime() != null ? auc.getStartTime().toString() : LocalDateTime.now().toString());
                aucObj.addProperty("endTime", auc.getEndTime() != null ? auc.getEndTime().toString() : LocalDateTime.now().plusHours(1).toString());
                // ==========================================================
                // ================= BƠM THÊM CỜ ĐẤU GIÁ NGƯỢC XUỐNG UI =================
                aucObj.addProperty("isReverse", auc.isReverse());
                aucObj.addProperty("dropStep", auc.getDropStep());
                if (auc.getCurrentWinner() != null) {
                    aucObj.addProperty("winnerId", auc.getCurrentWinner().getUsername());
                } else {
                    aucObj.addProperty("winnerId", "---");
                }
                // Đóng gói an toàn thằng Item (NÉ User seller ra)
                // Đóng gói an toàn thằng Item (NÉ User seller ra)
                // Đóng gói an toàn thằng Item (NÉ User seller ra)
                if (auc.getItem() != null) {
                    // Tạo biến item ngắn gọn cho dễ gọi
                    com.bidding.server.model.item.Item item = auc.getItem();
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", item.getId());
                    itemObj.addProperty("name", item.getName());

                    // ================= BỔ SUNG CÁC TRƯỜNG BỊ THIẾU =================
                    itemObj.addProperty("startingPrice", item.getStartingPrice());
                    itemObj.addProperty("condition", item.getCondition() != null ? item.getCondition().toString() : "NEW");
                    itemObj.addProperty("type", item.getCategory());
                    itemObj.addProperty("description", item.getDescription());

                    // Lấy mảng đường dẫn ảnh
                    if (item.getImages() != null && !item.getImages().isEmpty()) {
                        com.google.gson.JsonArray imgArray = new com.google.gson.JsonArray();
                        for (String img : item.getImages()) {
                            imgArray.add(img);
                        }
                        itemObj.add("images", imgArray);
                    }

                    // ================= VŨ KHÍ QUÉT THÔNG SỐ ĐẶC BIỆT =================
                    JsonObject specsObj = new JsonObject();
                    if (item.getSpecifications() != null) {
                        for (java.util.Map.Entry<String, String> entry : item.getSpecifications().entrySet()) {
                            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                                specsObj.addProperty(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    itemObj.add("specifications", specsObj);
                    // ===============================================================

                    aucObj.add("item", itemObj);
                }

                dataArray.add(aucObj);
            }

            // Đóng gói cục Reply
            JsonObject reply = new JsonObject();
            reply.addProperty("action", "AUCTIONS_LIST");
            reply.add("data", dataArray);
            if (client.getLoggedInUser() != null) {
                double freshBal = baoVe.getFreshBalance(client.getLoggedInUser().getId());
                client.getLoggedInUser().setBalance(freshBal);
                reply.addProperty("balance", freshBal);
            }
            // Gửi chuỗi JSON siêu an toàn về cho UI
            client.sendMessage(reply.toString());

        } catch (Exception e) {
            System.err.println("Lỗi đóng gói JSON sảnh đấu giá: " + e.getMessage());
            client.sendError("Lỗi khi tải danh sách đấu giá: " + e.getMessage());
        }
    }

    public void handleBid(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Chưa đăng nhập mà đòi đấu giá à?");
            return;
        }
        try {
            String auctionId = request.get("auctionId").getAsString();
            double amount = request.get("amount").getAsDouble();

            // 1. Chốt đơn và trừ tiền (Hàm này chạy cực mượt)
            tongQuan.placeBid(auctionId, amount, client.getLoggedInUser());

            // ================= BỌC THÉP HIBERNATE =================
            // Lấy tạm số dư hiện tại trên RAM (vì placeBid đã trừ tiền rồi)
            double freshBal = client.getLoggedInUser().getBalance();

            try {
                // Cố gắng đồng bộ với DB. Nếu DB kẹt vì vừa đóng phiên xong -> Bỏ qua!
                freshBal = baoVe.getFreshBalance(client.getLoggedInUser().getId());
                client.getLoggedInUser().setBalance(freshBal); // Đồng bộ lại RAM
            } catch (Exception ex) {
                System.out.println("[Cảnh báo] Bỏ qua đồng bộ số dư do DB đóng phiên: " + ex.getMessage());
            }
            // ======================================================

            client.sendMessage("{\"action\": \"BID_REPLY\", \"status\": \"SUCCESS\", \"balance\": " + freshBal + "}");
        } catch (Exception e) {
            client.sendError(e.getMessage());
        }
    }

    public void handleGetHistory() {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để xem lịch sử!");
            return;
        }
        try {
            List<BiddingTransactionEntity> history = baoVe.getBidHistory(client.getLoggedInUser().getId());

            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();
            for (BiddingTransactionEntity tx : history) {
                JsonObject obj = new JsonObject();
                // Bóc từng trường cơ bản ra, TUYỆT ĐỐI KHÔNG ép nguyên cục Entity
                obj.addProperty("id", tx.getId());

                // Lấy ID phiên (nếu nó null thì để trống tránh NullPointer)
                obj.addProperty("auctionId", tx.getAuction() != null ? tx.getAuction().getId() : "N/A");

                obj.addProperty("amount", tx.getBidAmount());
                obj.addProperty("timestamp", tx.getBidTime() != null ? tx.getBidTime().toString() : "");
                obj.addProperty("isAuto", tx.isAutoBid());
                obj.addProperty("status", tx.getStatus() != null ? tx.getStatus().name() : "UNKNOWN");

                dataArray.add(obj);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "HISTORY_REPLY");
            reply.add("data", dataArray);
            System.out.println("[Server] Chuẩn bị gửi lịch sử về cho Client: " + reply.toString());
            client.sendMessage(reply.toString());

        } catch (Exception e) {
            System.err.println("Lỗi đóng gói Lịch sử: " + e.getMessage());
            client.sendError("Không thể lấy lịch sử đấu giá lúc này.");
        }
    }

    public void handleRegisterAutoBid(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập mới được cài đặt đấu giá tự động!");
            return;
        }

        try {
            String auctionId = request.get("auctionId").getAsString();
            double maxBid = request.get("maxBid").getAsDouble();
            double increment = request.get("increment").getAsDouble();

            Auction auction = tongQuan.getActiveAuctions().stream()
                    .filter(a -> a.getId().equals(auctionId))
                    .findFirst()
                    .orElse(null);

            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này trên sàn!");
                return;
            }

            auction.registerAutoBid(client.getLoggedInUser(), maxBid, increment);
            tongQuan.triggerAutoBids(auction);
            // Controller gửi Reply
            client.sendMessage("{\"action\": \"REGISTER_AUTO_BID_REPLY\", \"status\": \"SUCCESS\"}");
            System.out.println("[Auto-Bid] Đã nạp cấu hình Bot cho User: " + client.getLoggedInUser().getUsername());

        } catch (Exception e) {
            client.sendError("Lỗi cài đặt Auto-Bid: " + e.getMessage());
        }
    }
    public void handleGetAuctionHistory(JsonObject request) {
        try {
            String auctionId = request.get("auctionId").getAsString();
            Auction auction = tongQuan.getActiveAuctions().stream()
                    .filter(a -> a.getId().equals(auctionId))
                    .findFirst().orElse(null);

            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này!");
                return;
            }

            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();
            if (auction.getBidHistory() != null) {
                // ================= VŨ KHÍ FIX BIỂU ĐỒ CHỮ V =================
                // Sắp xếp lịch sử theo thời gian TĂNG DẦN (Cũ trước -> Mới sau)
                auction.getBidHistory().stream()
                        .sorted((tx1, tx2) -> tx1.getBidTime().compareTo(tx2.getBidTime()))
                        .forEach(tx -> {
                            JsonObject point = new JsonObject();
                            point.addProperty("timestamp", tx.getBidTime().toString());
                            point.addProperty("price", tx.getBidAmount());
                            dataArray.add(point);
                        });
                // ============================================================
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "AUCTION_HISTORY_REPLY");
            reply.add("data", dataArray);
            client.sendMessage(reply.toString());
        } catch (Exception e) {
            client.sendError("Lỗi tải lịch sử: " + e.getMessage());
        }
    }
}