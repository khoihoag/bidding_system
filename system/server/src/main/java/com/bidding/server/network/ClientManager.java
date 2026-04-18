package com.bidding.server.network;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.bidding.server.events.AuctionEvent;
public class ClientManager implements AuctionObserver {

    // Dùng cái này thay cho ArrayList để chống nổ khi có thằng rút dây mạng
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    // Khách mới vào quán thì gọi hàm này
    public void addClient(ClientHandler client) {
        activeClients.add(client);
        System.out.println("Loa phường: Đã thêm 1 giang hồ. Tổng số: " + activeClients.size());
    }

    // Khách thoát ra thì gọi hàm này (Trong ClientHandler nhớ gọi lúc catch Exception)
    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
        System.out.println("Loa phường: 1 giang hồ đã sủi. Còn lại: " + activeClients.size());
    }

    // =========================================================
    // HỢP ĐỒNG HÓNG TIN TỪ TỔNG QUẢN (AUCTION SERVICE)
    // =========================================================
    @Override
    public void onBidPlaced(AuctionEvent event) {
        // FIX: Dùng \"%s\" (chuỗi có bọc ngoặc kép) cho các trường ID
        String jsonMessage = String.format(
                "{\"action\": \"NEW_BID\", \"auctionId\": \"%s\", \"newPrice\": %f, \"winnerId\": \"%s\"}",
                event.getAuctionId(), event.getNewPrice(), event.getWinnerId()
        );

        // 2. Bắn tin cho toàn thể anh em đang kết nối
        for (ClientHandler client : activeClients) {
            client.sendMessage(jsonMessage);
        }
    }

    @Override
    public void onAuctionClosed(AuctionEvent event) {
        // Tương tự, nếu Tổng quản báo hết giờ thì gửi JSON đóng phiên
    }

    @Override
    public void onPriceChanged(AuctionEvent event) {
        // ...
    }
}