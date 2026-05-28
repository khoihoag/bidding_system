package com.bidding.server.network;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.repository.UserRepository;
import com.google.gson.JsonObject; // Xài JsonObject để bọc chuỗi cho an toàn

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientManager implements AuctionObserver {
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> announcedClosedAuctions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final UserRepository userRepository;
    // ĐÃ XÓA: private final Gson gson = new Gson(); (Bom nổ chậm)

    public ClientManager() {
        this(null);
    }

    public ClientManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void addClient(ClientHandler client) {
        activeClients.add(client);
        System.out.println("Loa phường: Đã thêm 1 giang hồ. Tổng số: " + activeClients.size());
    }

    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
        System.out.println("Loa phường: 1 giang hồ đã sủi. Còn lại: " + activeClients.size());
    }

    // =========================================================
    // 1. KHI CÓ PHIÊN ĐẤU GIÁ MỚI LÊN SÀN (VỪA START)
    // =========================================================
    @Override
    public void onAuctionStarted(AuctionEvent event) {
        String itemName = event.getAuction() != null && event.getAuction().getItem() != null
                ? event.getAuction().getItem().getName()
                : "Vat pham dau gia";
        String msg = (event.getMessage() != null && !event.getMessage().isEmpty())
                ? event.getMessage()
                : "Mon hang moi len san: " + itemName + "!";

        notifyAllUsers(createNotification("Phiên đấu giá mới", msg));
    }

    @Override
    public void onBidPlaced(AuctionEvent event) {
        System.out.println("[Debug] BidPlaced event received. Auction: " + event.getAuctionId());
        if (event.getAuction().getFollowerIds() != null) {
            System.out.println("[Debug] Danh sách follower: " + event.getAuction().getFollowerIds().size());
        } else {
            System.out.println("[Debug] followerIds đang là NULL!");
        }
        JsonObject json = new JsonObject();
        json.addProperty("action", "NEW_BID");

        // Dùng luôn hàm tiện ích sếp đã viết
        json.addProperty("auctionId", event.getAuctionId());

        // ================= BỌC THÉP CHỐNG LỖI ĐẤU GIÁ NGƯỢC =================
        // Dùng luôn hàm getNewPrice() thiên tài của sếp, nó đã lo hết logic rồi!
        json.addProperty("newPrice", event.getNewPrice());

        // Đổi getTransaction() thành getBiddingTransaction() cho khớp với Lombok của sếp
        boolean isSystemDrop = (event.getBiddingTransaction() == null);
        json.addProperty("isSystemDrop", isSystemDrop);
        // ====================================================================

        // Dùng chuẩn ISO cho thời gian để UI dễ parse
        json.addProperty("timestamp", event.getTimestampStr());

        // Lấy tên người chiến thắng
        String winner = (event.getAuction().getCurrentWinner() != null)
                ? event.getAuction().getCurrentWinner().getUsername()
                : "---";
        json.addProperty("winnerId", winner);

        // ================= VŨ KHÍ ANTI-SNIPING =================
        if (event.getAuction() != null && event.getAuction().getEndTime() != null) {
            json.addProperty("newEndTime", event.getAuction().getEndTime().toString());
        }
        // =======================================================

        // ... (Code cũ của sếp ở trên)
        broadcast(json.toString());

        // ================= GỬI THÔNG BÁO CHO NGƯỜI THEO DÕI =================
        if (event.getAuction() != null) {
            // LƯU Ý: Sếp cần tự thêm thuộc tính followerIds (List<String>) vào Model Auction của sếp nhé!
            List<String> followers = event.getAuction().getFollowerIds();

            if (followers != null && !followers.isEmpty()) {
                JsonObject notif = new JsonObject();
                notif.addProperty("action", "NEW_NOTIFICATION");

                JsonObject notifData = new JsonObject();
                notifData.addProperty("time", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                // Format lại giá tiền cho đẹp
                java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
                String priceStr = fmt.format(event.getNewPrice());

                notifData.addProperty("message", "🔥 Phiên đấu giá [" + event.getAuction().getItem().getName() + "] vừa có giá mới: " + priceStr + " đ!");
                notif.add("data", notifData);

                // Gửi thông báo đến những người đang follow (ngoại trừ ông vừa đặt giá)
                List<String> notifyList = new java.util.ArrayList<>(followers);
                if (event.getAuction().getCurrentWinner() != null) {
                    notifyList.remove(event.getAuction().getCurrentWinner().getId()); // Đừng gửi cho người vừa đặt giá
                }

                sendToUsers(notifyList, notif.toString());
            }
        }}
    // =========================================================
    // 3. KHI PHIÊN ĐẤU GIÁ KẾT THÚC (HẾT GIỜ HOẶC ADMIN ĐÓNG)
    // =========================================================
    @Override
    public void onAuctionClosed(AuctionEvent event) {
        var auction = event.getAuction();

        // 1. Vẫn giữ cái thông báo chung cho cả làng cùng biết (null-safe để khỏi văng NPE khi test/mock)
        String itemName = "Vật phẩm đấu giá";
        if (auction != null && auction.getItem() != null && auction.getItem().getName() != null) {
            itemName = auction.getItem().getName();
        }
        String auctionStatus = auction != null && auction.getStatus() != null
                ? auction.getStatus().name()
                : "UNKNOWN";
        boolean canceled = "CANCELED".equals(auctionStatus);
        String auctionId = event.getAuctionId() != null ? event.getAuctionId() : "UNKNOWN";
        if (!announcedClosedAuctions.add(auctionId + ":" + auctionStatus)) {
            return;
        }

        java.util.LinkedHashSet<String> followerIds = new java.util.LinkedHashSet<>();
        if (auction != null && auction.getFollowerIds() != null) {
            followerIds.addAll(auction.getFollowerIds());
        }

        String sellerId = auction != null && auction.getItem() != null && auction.getItem().getSellerId() != null
                ? auction.getItem().getSellerId()
                : "";
        String winnerUserId = "";
        String winnerUsername = "NONE";
        String winnerDisplay = "Kh\u00f4ng c\u00f3 ng\u01b0\u1eddi \u0111\u1eb7t gi\u00e1";
        if (auction != null && auction.getCurrentWinner() != null) {
            var winner = auction.getCurrentWinner();
            winnerUserId = winner.getId() != null ? winner.getId() : "";
            winnerUsername = winner.getUsername() != null ? winner.getUsername() : "NONE";
            winnerDisplay = winner.getFullName() != null && !winner.getFullName().isBlank()
                    ? winner.getFullName()
                    : winnerUsername;
        }

        String closeMessage = canceled
                ? "Phi\u00ean \u0111\u1ea5u gi\u00e1 [" + itemName + "] \u0111\u00e3 b\u1ecb admin bu\u1ed9c d\u1eebng."
                : "Phi\u00ean \u0111\u1ea5u gi\u00e1 [" + itemName + "] \u0111\u00e3 k\u1ebft th\u00fac. Ng\u01b0\u1eddi th\u1eafng: " + winnerDisplay + ".";

        JsonObject notifyJson = new JsonObject();
        notifyJson.addProperty("action", "AUCTION_STATUS_CHANGED");
        notifyJson.addProperty("auctionId", auctionId);
        notifyJson.addProperty("status", auctionStatus);
        notifyJson.addProperty("winnerId", winnerUsername);
        notifyJson.addProperty("message", "Phiên đấu giá [" + itemName + "] đã kết thúc!");
        if (canceled) {
            notifyJson.addProperty("message", "Phiên đấu giá [" + itemName + "] đã bị admin buộc dừng.");
        }
        notifyJson.addProperty("message", closeMessage);
        broadcast(notifyJson.toString());

        if (!followerIds.isEmpty()) {
            JsonObject followerNotice = createNotification(
                    "Phi\u00ean \u0111\u00e3 theo d\u00f5i k\u1ebft th\u00fac",
                    closeMessage
            );
            sendToUsers(new java.util.ArrayList<>(followerIds), wrapNotification(followerNotice).toString());
        }

        // 2. Bắn thêm gói tin CHI TIẾT để UI hiển thị kết quả
        JsonObject endJson = new JsonObject();
        endJson.addProperty("action", "AUCTION_FINISHED");
        endJson.addProperty("auctionId", auctionId);
        endJson.addProperty("status", auctionStatus);

        // Giá chốt: ưu tiên dùng convenience getter để lấy đúng giá từ RAM model (AtomicReference)
        double finalPrice = event.getNewPrice() != null
                ? event.getNewPrice()
                : auction != null && auction.getCurrentPrice() != null ? auction.getCurrentPrice().get() : 0.0;

        // Người thắng cuối cùng: lấy từ Auction nếu có, fallback để chạy test/mock
        String winnerName;
        if (auction == null) {
            winnerName = "NONE";
        } else {
            winnerName = (auction.getCurrentWinner() != null)
                    ? auction.getCurrentWinner().getUsername()
                    : "Không có người đặt giá";
        }

        endJson.addProperty("winnerId", winnerName);
        if (auction != null && auction.getCurrentWinner() != null) {
            var winner = auction.getCurrentWinner();
            endJson.addProperty("winnerUserId", winner.getId() != null ? winner.getId() : "");
            endJson.addProperty("winnerUsername", winner.getUsername() != null ? winner.getUsername() : "NONE");
            endJson.addProperty("winnerName",
                    winner.getFullName() != null && !winner.getFullName().isBlank()
                            ? winner.getFullName()
                            : (winner.getUsername() != null ? winner.getUsername() : "NONE"));
        } else {
            endJson.addProperty("winnerUserId", "");
            endJson.addProperty("winnerUsername", "NONE");
            endJson.addProperty("winnerName", "NONE");
        }
        endJson.addProperty("winnerId", winnerUsername);
        endJson.addProperty("winnerUserId", winnerUserId);
        endJson.addProperty("winnerUsername", winnerUsername);
        endJson.addProperty("winnerName", winnerDisplay);
        endJson.addProperty("itemName", itemName);
        endJson.addProperty("finalPrice", finalPrice);
        java.util.LinkedHashSet<String> dialogRecipients = new java.util.LinkedHashSet<>(followerIds);
        if (!winnerUserId.isBlank()) {
            dialogRecipients.add(winnerUserId);
        }
        if (!sellerId.isBlank()) {
            dialogRecipients.add(sellerId);
        }
        sendToUsersOneConnectionPerAccount(new java.util.ArrayList<>(dialogRecipients), endJson.toString());
    }

    @Override
    public void onPriceChanged(AuctionEvent event) {
        // Hàm này dư thừa, để trống
    }

    // TIỆN ÍCH: Hàm bổ trợ để hú cho tất cả anh em online
    public void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }

    private void broadcastOneConnectionPerAccount(String message) {
        java.util.Set<String> sentAccounts = new java.util.HashSet<>();
        for (ClientHandler client : activeClients) {
            String accountKey = client.getLoggedInUser() != null && client.getLoggedInUser().getId() != null
                    ? client.getLoggedInUser().getId()
                    : "connection:" + System.identityHashCode(client);
            if (sentAccounts.add(accountKey)) {
                client.sendMessage(message);
            }
        }
    }

    private void sendToUsersOneConnectionPerAccount(List<String> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        java.util.Set<String> allowedAccounts = new java.util.HashSet<>(userIds);
        java.util.Set<String> sentAccounts = new java.util.HashSet<>();
        for (ClientHandler client : activeClients) {
            if (client.getLoggedInUser() == null || client.getLoggedInUser().getId() == null) {
                continue;
            }

            String accountKey = client.getLoggedInUser().getId();
            if (allowedAccounts.contains(accountKey) && sentAccounts.add(accountKey)) {
                client.sendMessage(message);
            }
        }
    }
    // =========================================================
    // HÚ RIÊNG CHO MỘT NHÓM ANH EM (DÙNG CHO THÔNG BÁO THEO DÕI)
    // =========================================================
    // =========================================================
    // KHAI BÁO HÒM THƯ (RAM) ĐỂ LƯU THÔNG BÁO CHO TỪNG USER
    // =========================================================
    public static final java.util.concurrent.ConcurrentHashMap<String, java.util.List<JsonObject>> mailbox = new java.util.concurrent.ConcurrentHashMap<>();

    // HÚ RIÊNG VÀ NHÉT VÀO HÒM THƯ
    public void notifyItemOwner(String userId, String title, String message) {
        sendToUsers(java.util.List.of(userId), wrapNotification(createNotification(title, message)).toString());
    }

    public void notifyBalanceChanged(String userId, double balance) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("action", "BALANCE_UPDATE");
        json.addProperty("balance", balance);

        for (ClientHandler client : activeClients) {
            if (client.getLoggedInUser() != null && userId.equals(client.getLoggedInUser().getId())) {
                client.getLoggedInUser().setBalance(balance);
                client.sendMessage(json.toString());
            }
        }
    }

    private void notifyAllUsers(JsonObject notificationData) {
        sendToUsers(allKnownUserIds(), wrapNotification(notificationData).toString());
    }

    private java.util.List<String> allKnownUserIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (userRepository != null) {
            try {
                for (com.bidding.server.model.user.User user : userRepository.findAll()) {
                    if (user.getId() != null && !user.getId().isBlank()) {
                        ids.add(user.getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("Khong the lay danh sach user de gui thong bao: " + e.getMessage());
            }
        }
        for (ClientHandler client : activeClients) {
            if (client.getLoggedInUser() != null && client.getLoggedInUser().getId() != null) {
                ids.add(client.getLoggedInUser().getId());
            }
        }
        return new java.util.ArrayList<>(ids);
    }

    private JsonObject createNotification(String title, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("title", title);
        data.addProperty("message", message);
        data.addProperty("time", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        data.addProperty("read", false);
        return data;
    }

    private JsonObject wrapNotification(JsonObject data) {
        JsonObject notif = new JsonObject();
        notif.addProperty("action", "NEW_NOTIFICATION");
        notif.add("data", data);
        return notif;
    }

    private String getString(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : fallback;
    }

    public void sendToUsers(List<String> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) return;

        // 1. Tách lấy phần 'data' (chứa message và time) để cất vào hòm thư
        try {
            JsonObject msgObj = com.google.gson.JsonParser.parseString(message).getAsJsonObject();
            if (msgObj.has("data")) {
                JsonObject dataObj = msgObj.getAsJsonObject("data");
                // Nhét vào hòm thư của từng ông
                for(String uid : userIds) {
                    mailbox.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>()).add(dataObj.deepCopy());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi lưu hòm thư: " + e.getMessage());
        }

        // 2. Vẫn gửi Live cho những ông đang online
        for (ClientHandler client : activeClients) {
            if (client.getLoggedInUser() != null && userIds.contains(client.getLoggedInUser().getId())) {
                client.sendMessage(message);
            }
        }
    }

    public void notifyUserAccountBanned(String userId) {
        if (userId == null || userId.isEmpty()) return;

        JsonObject json = new JsonObject();
        json.addProperty("action", "ACCOUNT_BANNED");
        json.addProperty("message", "Tai khoan cua ban da bi admin khoa.");

        for (ClientHandler client : activeClients) {
            if (client.getLoggedInUser() != null && userId.equals(client.getLoggedInUser().getId())) {
                client.setLoggedInUser(null);
                client.sendMessage(json.toString());
            }
        }
    }
}
