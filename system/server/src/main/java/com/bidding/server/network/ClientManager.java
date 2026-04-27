package com.bidding.server.network;
import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.google.gson.Gson; // Thêm Gson để ép nguyên cục Auction sang JSON cho nhanh

public class ClientManager implements AuctionObserver {
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson(); // Dùng để biến Object thành chuỗi JSON

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
        // Tận dụng Gson để biến Object Auction thành JSON
        // event.getAuction() sẽ trả về toàn bộ thông tin món hàng đang lên sàn
        String auctionJson = new com.google.gson.Gson().toJson(event.getAuction());

        String jsonMessage = String.format(
                "{\"action\": \"NEW_AUCTION_POSTED\", \"data\": %s}",
                auctionJson
        );

        // Bắn tin cho toàn thể anh em đang online
        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonMessage);
        }
    }

    // =========================================================
    // 2. KHI CÓ NGƯỜI VỪA ĐẶT GIÁ MỚI
    // =========================================================
    @Override
    public void onBidPlaced(AuctionEvent event) {
        String jsonMessage = String.format(
                "{\"action\": \"NEW_BID\", \"auctionId\": \"%s\", \"newPrice\": %f, \"winnerId\": \"%s\"}",
                event.getAuctionId(), event.getNewPrice(), event.getWinnerId()
        );

        broadcast(jsonMessage);
    }

    // =========================================================
    // 3. KHI PHIÊN ĐẤU GIÁ KẾT THÚC (HẾT GIỜ HOẶC ADMIN ĐÓNG)
    // =========================================================
    @Override
    public void onAuctionClosed(AuctionEvent event) {
        // Hoàn thiện luôn hàm này cho ông
        String jsonMessage = String.format(
                "{\"action\": \"AUCTION_CLOSED\", \"auctionId\": \"%s\", \"winnerId\": \"%s\", \"finalPrice\": %f}",
                event.getAuctionId(),
                event.getWinnerId() != null ? event.getWinnerId() : "NONE",
                event.getNewPrice()
        );

        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonMessage);
        }
    }

    @Override
    public void onPriceChanged(AuctionEvent event) {
        // Có thể dùng cho các tính năng cập nhật giá nhanh nếu cần
    }

    // TIỆN ÍCH: Hàm bổ trợ để hú cho tất cả anh em online
    public void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }
}