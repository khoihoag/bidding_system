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
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.util.List;
import com.bidding.server.utils.ApiResponse;
import com.bidding.server.utils.ItemJsonMapper;
public class AuctionController {
    private final ClientHandler client;
    private final AuctionService tongQuan;
    private final ItemService quanLyKho;
    private final ClientManager loaPhuong; // Vẫn giữ để không vỡ cấu trúc gọi hàm từ ClientHandler
    private final UserService baoVe;

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
            int antiSnipingSeconds = getOptionalPositiveInt(request, "antiSnipingSeconds", AuctionService.DEFAULT_ANTI_SNIPING_SECONDS);
            int extensionSeconds = getOptionalPositiveInt(request, "extensionSeconds", AuctionService.DEFAULT_EXTENSION_SECONDS);
            // ==============================================================

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Không tìm thấy sản phẩm này!"); return; }
            if (!item.isApprovedForAuction()) {
                client.sendError("San pham nay chua duoc admin duyet nen khong the mo phien dau gia.");
                return;
            }

            // ================= BỌC THÉP LỖI DỮ LIỆU RÁC =================
            if (item.getSellerId() == null || item.getSellerId().isEmpty()) {
                client.sendError("Món đồ này bị lỗi dữ liệu cũ (không có chủ). Hãy xóa và thêm món đồ mới vào kho.");
                return;
            }
            if (!item.getSellerId().equals(client.getLoggedInUser().getId())) {
                client.sendError("Bạn không thể mang đồ của người khác đi đấu giá!");
                return;
            }
            // ============================================================

            // GỌI HÀM VỚI ĐỦ 4 THAM SỐ MỚI
            Auction newAuction = tongQuan.startAuction(item, duration, isReverse, dropStep, antiSnipingSeconds, extensionSeconds);

            client.sendMessage(ApiResponse.success("START_AUCTION_REPLY", "auctionId", newAuction.getId()).toString());

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
            int antiSnipingSeconds = getOptionalPositiveInt(request, "antiSnipingSeconds", AuctionService.DEFAULT_ANTI_SNIPING_SECONDS);
            int extensionSeconds = getOptionalPositiveInt(request, "extensionSeconds", AuctionService.DEFAULT_EXTENSION_SECONDS);
            // ==============================================================

            Item item = quanLyKho.findById(itemId);
            if (item == null) { client.sendError("Không tìm thấy sản phẩm này!"); return; }
            if (!item.isApprovedForAuction()) {
                client.sendError("San pham nay chua duoc admin duyet nen khong the dat lich dau gia.");
                return;
            }

            // ================= BỌC THÉP LỖI DỮ LIỆU RÁC =================
            if (item.getSellerId() == null || item.getSellerId().isEmpty()) {
                client.sendError("Món đồ này bị lỗi dữ liệu cũ (không có chủ). Hãy xóa và thêm món đồ mới vào kho.");
                return;
            }
            if (!item.getSellerId().equals(client.getLoggedInUser().getId())) {
                client.sendError("Bạn không thể mang đồ của người khác đi đấu giá!");
                return;
            }
            // ============================================================

            // GỌI HÀM VỚI ĐỦ 5 THAM SỐ MỚI
            Auction newAuction = tongQuan.scheduleAuction(item, startTime, endTime, isReverse, dropStep, antiSnipingSeconds, extensionSeconds);

            client.sendMessage(ApiResponse.success("SCHEDULE_AUCTION_REPLY", "auctionId", newAuction.getId()).toString());

        } catch (java.time.format.DateTimeParseException e) {
            client.sendError("Lỗi định dạng thời gian. Vui lòng dùng chuẩn yyyy-MM-ddTHH:mm:ss");
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private int getOptionalPositiveInt(JsonObject request, String propertyName, int defaultValue) {
        if (!request.has(propertyName) || request.get(propertyName).isJsonNull()) {
            return defaultValue;
        }

        int value = request.get(propertyName).getAsInt();
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " phải lớn hơn 0.");
        }
        return value;
    }

    public void handlePayAuction(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập để thanh toán.");
            return;
        }

        try {
            String auctionId = request.get("auctionId").getAsString();
            tongQuan.payWonAuction(auctionId, client.getLoggedInUser());
            double freshBal = baoVe.getFreshBalance(client.getLoggedInUser().getId());
            client.getLoggedInUser().setBalance(freshBal);

            JsonObject reply = ApiResponse.success("PAY_AUCTION_REPLY", "auctionId", auctionId);
            reply.addProperty("balance", freshBal);
            client.sendMessage(reply.toString());
        } catch (Exception e) {
            client.sendError("Lỗi khi thanh toán phiên đấu giá: " + e.getMessage());
        }
    }

    public void handleGetAuctions() {
        try {
            List<Auction> activeAuctions = tongQuan.getAllAuctionsForDisplay();
            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();

            for (Auction auc : activeAuctions) {
                JsonObject aucObj = new JsonObject();

                // 1. Thông tin cơ bản
                aucObj.addProperty("id", auc.getId());
                aucObj.addProperty("currentPrice", auc.getCurrentPrice().get());
                aucObj.addProperty("status", auc.getStatus() != null ? auc.getStatus().toString() : "UNKNOWN");

                // 2. Thời gian
                aucObj.addProperty("startTime", auc.getStartTime() != null ? auc.getStartTime().toString() : LocalDateTime.now().toString());
                aucObj.addProperty("endTime", auc.getEndTime() != null ? auc.getEndTime().toString() : LocalDateTime.now().plusHours(1).toString());

                // 3. Cờ đấu giá ngược & Follow
                aucObj.addProperty("isReverse", auc.isReverse());
                aucObj.addProperty("dropStep", auc.getDropStep());
                Item displayItem = auc.getItem();
                if (displayItem != null && displayItem.getBidStep() <= 0 && displayItem.getId() != null) {
                    Item freshItem = quanLyKho.findById(displayItem.getId());
                    if (freshItem != null) {
                        displayItem = freshItem;
                        auc.getItem().setBidStep(freshItem.getBidStep());
                    }
                }
                aucObj.addProperty("bidStep", displayItem != null ? displayItem.getBidStep() : 0.0);

                boolean isFollowing = false;
                if (client.getLoggedInUser() != null) {
                    isFollowing = auc.getFollowerIds().contains(client.getLoggedInUser().getId());
                }
                aucObj.addProperty("isFollowing", isFollowing);

                // 4. Người chiến thắng
                if (auc.getCurrentWinner() != null) {
                    aucObj.addProperty("winnerId", auc.getCurrentWinner().getUsername());
                } else {
                    aucObj.addProperty("winnerId", "---");
                }

                // 5. Đóng gói Item
                if (displayItem != null) {
                    aucObj.add("item", ItemJsonMapper.toJson(displayItem));
                }

                // CHỈ ADD DUY NHẤT 1 LẦN Ở ĐÂY
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

            client.sendMessage(reply.toString());

        } catch (Exception e) {
            System.err.println("Lỗi đóng gói JSON sảnh đấu giá: " + e.getMessage());
            client.sendError("Lỗi khi tải danh sách đấu giá: " + e.getMessage());
        }
    }

    public void handleGetSellerProfile(JsonObject request) {
        JsonObject reply = new JsonObject();
        reply.addProperty("action", "SELLER_PROFILE_REPLY");

        try {
            String sellerId = request.has("sellerId") && !request.get("sellerId").isJsonNull()
                    ? request.get("sellerId").getAsString()
                    : "";

            if (sellerId.isBlank()) {
                reply.addProperty("status", "ERROR");
                reply.addProperty("message", "Thiếu ID người bán.");
                client.sendMessage(reply.toString());
                return;
            }

            com.bidding.server.model.user.User seller = baoVe.findById(sellerId);
            if (seller == null) {
                reply.addProperty("status", "ERROR");
                reply.addProperty("message", "Không tìm thấy hồ sơ người bán.");
                client.sendMessage(reply.toString());
                return;
            }

            reply.addProperty("status", "SUCCESS");
            reply.addProperty("sellerId", seller.getId());
            reply.addProperty("fullName", seller.getFullName());
            reply.addProperty("email", seller.getEmail());
            reply.addProperty("createdAuctions", tongQuan.countAuctionsCreatedBySeller(sellerId));
            reply.addProperty("successfulAuctions", tongQuan.countSuccessfulAuctionsBySeller(sellerId));
            reply.addProperty("canceledAuctions", tongQuan.countCanceledAuctionsBySeller(sellerId));
            reply.addProperty("wonItems", tongQuan.countWonItemsByUser(sellerId));
            reply.addProperty("auctionProducts", quanLyKho.countItemsBySellerId(sellerId));
            client.sendMessage(reply.toString());
        } catch (Exception e) {
            reply.addProperty("status", "ERROR");
            reply.addProperty("message", "Không thể tải hồ sơ người bán: " + e.getMessage());
            client.sendMessage(reply.toString());
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

            client.sendMessage(ApiResponse.success("BID_REPLY", "balance", freshBal).toString());
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
            client.sendMessage(ApiResponse.success("REGISTER_AUTO_BID_REPLY").toString());
            System.out.println("[Auto-Bid] Đã nạp cấu hình Bot cho User: " + client.getLoggedInUser().getUsername());

        } catch (Exception e) {
            client.sendError("Lỗi cài đặt Auto-Bid: " + e.getMessage());
        }
    }
    public void handleGetAuctionHistory(JsonObject request) {
        try {
            String auctionId = request.get("auctionId").getAsString();
            Auction auction = tongQuan.findAuctionById(auctionId);

            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này!");
                return;
            }

            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();
            if (auction.getBidHistory() != null) {
                auction.getBidHistory().stream()
                        .filter(tx -> tx.getBidTime() != null)
                        .sorted(java.util.Comparator.comparing(com.bidding.server.model.transaction.BiddingTransaction::getBidTime))
                        .forEach(tx -> {
                            JsonObject point = new JsonObject();
                            point.addProperty("timestamp", tx.getBidTime().toString());
                            point.addProperty("price", tx.getBidAmount());
                            point.addProperty("bidder", tx.getBidder() != null ? tx.getBidder().getUsername() : "---");
                            point.addProperty("type", tx.isAutoBid() ? "AUTO" : "MANUAL");
                            point.addProperty("status", tx.getStatus() != null ? tx.getStatus().toString() : "---");
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
    // ================= XỬ LÝ LỆNH THEO DÕI TỪ UI =================
    public void handleFollowAuction(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để theo dõi phiên đấu giá!");
            return;
        }
        try {
            String auctionId = request.get("auctionId").getAsString();
            String userId = client.getLoggedInUser().getId();

            Auction auction = tongQuan.findAuctionById(auctionId);
            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này!");
                return;
            }

            // Gọi logic thêm follower (Sếp phải tự viết hàm addFollower trong Model hoặc Service nhé)
            auction.addFollower(userId);
            // Nếu dùng Database, sếp gọi: tongQuan.saveFollowerToDB(auctionId, userId);

            // Phản hồi về UI cho vui (Thực ra UI mình đã tự đổi chữ thành ĐÃ THEO DÕI rồi)
            client.sendMessage(ApiResponse.success("FOLLOW_AUCTION_REPLY").toString());
            System.out.println("[Theo dõi] User " + client.getLoggedInUser().getUsername() + " đang hóng phiên: " + auctionId);

        } catch (Exception e) {
            client.sendError("Lỗi hệ thống khi theo dõi: " + e.getMessage());
        }
    }

    public void handleUnfollowAuction(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để hủy theo dõi phiên đấu giá!");
            return;
        }
        try {
            String auctionId = request.get("auctionId").getAsString();
            String userId = client.getLoggedInUser().getId();

            Auction auction = tongQuan.findAuctionById(auctionId);
            if (auction == null) {
                client.sendError("Không tìm thấy phiên đấu giá này!");
                return;
            }

            auction.removeFollower(userId);
            client.sendMessage(ApiResponse.success("UNFOLLOW_AUCTION_REPLY").toString());
            System.out.println("[Theo dõi] User " + client.getLoggedInUser().getUsername() + " đã hủy theo dõi phiên: " + auctionId);
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống khi hủy theo dõi: " + e.getMessage());
        }
    }

    // Sếp dán hàm này vào AuctionController.java
    public void handleGetNotifications(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập!");
            return;
        }

        try {
            String userId = client.getLoggedInUser().getId();
            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();

            // Lấy thông báo từ Hòm Thư của ClientManager
            List<JsonObject> myNotifs = com.bidding.server.network.ClientManager.mailbox.get(userId);

            if (myNotifs != null && !myNotifs.isEmpty()) {
                // Đảo ngược danh sách để thông báo mới nhất hiện lên trên
                for (int i = myNotifs.size() - 1; i >= 0; i--) {
                    dataArray.add(myNotifs.get(i));
                }
            } else {
                // Nếu chưa có thông báo nào thì hiện câu chào mừng
                JsonObject fakeNotif = new JsonObject();
                fakeNotif.addProperty("message", "Chưa có thông báo mới nào. Hãy theo dõi các phiên đấu giá nhé!");
                fakeNotif.addProperty("time", java.time.LocalTime.now().toString());
                dataArray.add(fakeNotif);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "NOTIFICATIONS_LIST");
            reply.add("data", dataArray);

            client.sendMessage(reply.toString());
        } catch (Exception e) {
            client.sendError("Lỗi tải thông báo!");
        }
    }
}
