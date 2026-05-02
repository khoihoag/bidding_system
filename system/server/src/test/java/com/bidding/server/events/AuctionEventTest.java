package com.bidding.server.events;

import com.bidding.server.model.Auction;
import com.bidding.server.model.transaction.BiddingTransaction;
import com.bidding.server.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionEventTest {

    @Mock
    private Auction mockAuction;

    @Mock
    private BiddingTransaction mockTransaction;

    @Mock
    private User mockUser;

    @Test
    @DisplayName("Test 1 (Trích xuất ID): Kiểm tra xem có lấy đúng ID của phòng đấu giá ra không")
    void testGetAuctionId() {
        // Giả lập: Phòng đấu giá có ID là "AUC-123"
        when(mockAuction.getId()).thenReturn("AUC-123");

        AuctionEvent event = new AuctionEvent();
        event.setAuction(mockAuction);

        assertEquals("AUC-123", event.getAuctionId());
    }

    @Test
    @DisplayName("Test 2 (Giá mới từ Giao dịch): Ưu tiên lấy giá từ lượt đặt giá mới nhất")
    void testGetNewPriceFromTransaction() {
        // Giả lập: Giao dịch đặt giá là 500.0
        when(mockTransaction.getBidAmount()).thenReturn(500.0);

        AuctionEvent event = new AuctionEvent();
        event.setBiddingTransaction(mockTransaction);

        assertEquals(500.0, event.getNewPrice());
    }

    @Test
    @DisplayName("Test 3 (Giá mới từ Phòng): Nếu không có giao dịch, phải lấy giá hiện tại từ phòng")
    void testGetNewPriceFromAuctionFallback() {
        // Giả lập: Giao dịch null, nhưng phòng đang có giá 300.0
        // Lưu ý: currentPrice trong Auction là AtomicReference
        AtomicReference<Double> priceRef = new AtomicReference<>(300.0);
        when(mockAuction.getCurrentPrice()).thenReturn(priceRef);

        AuctionEvent event = new AuctionEvent();
        event.setAuction(mockAuction);
        event.setBiddingTransaction(null);

        assertEquals(300.0, event.getNewPrice());
    }

    @Test
    @DisplayName("Test 4 (Ai đang thắng?): Kiểm tra xem có trích xuất đúng ID người đặt giá không")
    void testGetWinnerId() {
        // Giả lập: Khách hàng có ID 999
        when(mockUser.getId()).thenReturn("999");
        when(mockTransaction.getBidder()).thenReturn(mockUser);

        AuctionEvent event = new AuctionEvent();
        event.setBiddingTransaction(mockTransaction);

        assertEquals("999", event.getWinnerId());
    }

    @Test
    @DisplayName("Test 5 (An toàn là trên hết): Dữ liệu rỗng (null) thì không được lăn đùng ra chết (NPE)")
    void testNullSafety() {
        AuctionEvent event = new AuctionEvent();

        // Không set gì cả, kiểm tra xem có trả về null/0.0 thay vì báo lỗi không
        assertNull(event.getAuctionId());
        assertNull(event.getWinnerId());
        assertEquals(0.0, event.getNewPrice());
    }
}