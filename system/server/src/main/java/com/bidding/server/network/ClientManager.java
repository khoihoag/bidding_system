package com.bidding.server.network;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.google.gson.JsonObject; // Xài JsonObject để bọc chuỗi cho an toàn

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientManager implements AuctionObserver {
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    // ĐÃ XÓA: private final Gson gson = new Gson(); (Bom nổ chậm)

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
        // ĐÃ FIX: Bỏ Gson chay. Bắn GLOBAL_NOTIFY vừa nhẹ Server vừa khỏe UI
        JsonObject notifyJson = new JsonObject();
        notifyJson.addProperty("action", "GLOBAL_NOTIFY");
        // Nếu event.getMessage() từ Service có nội dung thì xài, không thì tự ghép:
        String msg = (event.getMessage() != null && !event.getMessage().isEmpty())
                ? event.getMessage()
                : "Món hàng mới lên sàn: " + event.getAuction().getItem().getName() + "!";
        notifyJson.addProperty("message", msg);

        broadcast(notifyJson.toString());
    }

    // =========================================================
    // 2. KHI CÓ NGƯỜI VỪA ĐẶT GIÁ MỚI (PHỤC VỤ VẼ LINE CHART)
    // =========================================================
    @Override
    public void onBidPlaced(AuctionEvent event) {
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

        broadcast(json.toString());
    }
    // =========================================================
    // 3. KHI PHIÊN ĐẤU GIÁ KẾT THÚC (HẾT GIỜ HOẶC ADMIN ĐÓNG)
    // =========================================================
    @Override
    public void onAuctionClosed(AuctionEvent event) {
        // 1. Vẫn giữ cái thông báo chung cho cả làng cùng biết[cite: 10]
        JsonObject notifyJson = new JsonObject();
        notifyJson.addProperty("action", "GLOBAL_NOTIFY");
        notifyJson.addProperty("message", "Phiên đấu giá [" + event.getAuction().getItem().getName() + "] đã kết thúc!");
        broadcast(notifyJson.toString());

        // 2. Bắn thêm gói tin CHI TIẾT để UI "khóa sổ" và hiện người thắng
        JsonObject endJson = new JsonObject();
        endJson.addProperty("action", "AUCTION_FINISHED");
        endJson.addProperty("auctionId", event.getAuctionId());

        // Lấy tên người thắng cuối cùng từ Auction
        String winnerName = (event.getAuction().getCurrentWinner() != null)
                ? event.getAuction().getCurrentWinner().getUsername()
                : "Không có người đặt giá";

        endJson.addProperty("winnerId", winnerName);
        broadcast(endJson.toString());
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
}