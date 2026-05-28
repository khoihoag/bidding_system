package com.bidding.server.model.network;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.ClientManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientManagerTest {

    private ClientManager clientManager;

    // Làm giả 2 khách hàng đang kết nối
    @Mock private ClientHandler mockClient1;
    @Mock private ClientHandler mockClient2;

    @Mock private AuctionEvent mockEvent;
    @Mock private com.bidding.server.model.auction.Auction mockAuction;
    @Mock private com.bidding.server.model.item.Item mockItem;
    @Mock private com.bidding.server.model.user.User mockWinner;

    @BeforeEach
    void setUp() {
        // Khởi tạo ClientManager bằng đúng mã nguồn nguyên bản của bạn
        clientManager = new ClientManager();

        // Thêm 2 "giang hồ" vào danh sách nghe loa
        clientManager.addClient(mockClient1);
        clientManager.addClient(mockClient2);
    }

    @Test
    @DisplayName("Test onAuctionStarted: Ép JSON bằng new Gson() và chạy vòng lặp for thủ công")
    void testOnAuctionStarted() {
        // Cho getAuction() trả về null để Gson dễ ép kiểu thành chữ "null",
        // tránh lỗi thư viện khi ép kiểu một đối tượng Mock ảo.
        when(mockEvent.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getItem()).thenReturn(mockItem);
        when(mockItem.getName()).thenReturn("Test Item");

        // Gọi hàm nguyên bản của bạn
        clientManager.onAuctionStarted(mockEvent);

        // Kiểm tra xem cái vòng lặp `for` thủ công trong hàm có gọi sendMessage cho cả 2 client không
        // Dùng contains để check chữ "NEW_AUCTION_POSTED" bên trong chuỗi JSON
        verify(mockClient1, times(1)).sendMessage(contains("GLOBAL_NOTIFY"));
        verify(mockClient2, times(1)).sendMessage(contains("GLOBAL_NOTIFY"));
    }

    @Test
    @DisplayName("Test onBidPlaced: Gọi hàm broadcast nguyên bản")
    void testOnBidPlaced() {
        when(mockEvent.getAuctionId()).thenReturn("A123");
        when(mockEvent.getNewPrice()).thenReturn(150.0);
        when(mockEvent.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getFollowerIds()).thenReturn(new java.util.concurrent.CopyOnWriteArrayList<>());
        when(mockAuction.getCurrentWinner()).thenReturn(mockWinner);
        when(mockWinner.getUsername()).thenReturn("USER_1");

        clientManager.onBidPlaced(mockEvent);

        // Chuỗi JSON mong đợi từ lệnh String.format của bạn
        verify(mockClient1, times(1)).sendMessage(contains("\"action\":\"NEW_BID\""));
        verify(mockClient1, times(1)).sendMessage(contains("\"auctionId\":\"A123\""));
        verify(mockClient1, times(1)).sendMessage(contains("\"winnerId\":\"USER_1\""));
        verify(mockClient2, times(1)).sendMessage(contains("\"action\":\"NEW_BID\""));
    }

    @Test
    @DisplayName("Test onAuctionClosed: Xử lý vòng lặp for thủ công và thay thế người thắng bằng NONE")
    void testOnAuctionClosed() {
        when(mockEvent.getAuctionId()).thenReturn("A999");
        when(mockEvent.getNewPrice()).thenReturn(500.0);

        // Cố tình cho người thắng bằng null để test đoạn logic 3 ngôi: event.getWinnerId() != null ? ... : "NONE"
        clientManager.onAuctionClosed(mockEvent);

        // Chữ "NONE" phải xuất hiện trong JSON thay vì null
        String expectedJson = "\"action\":\"AUCTION_FINISHED\"";

        // Kiểm tra vòng lặp for
        verify(mockClient1, times(1)).sendMessage(contains(expectedJson));
        verify(mockClient1, times(1)).sendMessage(contains("\"auctionId\":\"A999\""));
        verify(mockClient1, times(1)).sendMessage(contains("\"winnerId\":\"NONE\""));
        verify(mockClient2, times(1)).sendMessage(contains(expectedJson));
    }

    @Test
    @DisplayName("Test removeClient: Giang hồ đã sủi thì không được nhận tin nhắn nữa")
    void testRemoveClient() {
        // Khách 1 sủi
        clientManager.removeClient(mockClient1);

        // Phát loa
        clientManager.broadcast("Tin nhắn hệ thống");

        // Đảm bảo máy của khách 1 không hề bị gọi hàm sendMessage (never)
        verify(mockClient1, never()).sendMessage(anyString());

        // Khách 2 vẫn ở lại nên phải nhận được tin 1 lần
        verify(mockClient2, times(1)).sendMessage("Tin nhắn hệ thống");
    }
}
