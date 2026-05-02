package com.bidding.server.model;

import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.InvalidBidAmountException;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransaction;
import com.bidding.server.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionTest {

    private Auction auction;

    @Mock
    private Item mockItem;

    @Mock
    private User mockUser1;

    @Mock
    private User mockUser2;

    @BeforeEach
    void setUp() {
        // Giả lập giá khởi điểm của Item là 100.0
        when(mockItem.getStartingPrice()).thenReturn(100.0);

        // Khởi tạo Auction với trạng thái OPEN
        auction = new Auction("auc-001", mockItem,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                10, 30);
    }

    @Test
    @DisplayName("Khởi tạo Auction thành công với giá khởi điểm đúng")
    void testAuctionInitialization() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(100.0, auction.getCurrentPrice().get());
        assertFalse(auction.hasBids());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    @DisplayName("start() - Chuyển trạng thái sang RUNNING thành công")
    void testStartAuctionSuccess() {
        auction.start();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertNotNull(auction.getStartTime());
    }

    @Test
    @DisplayName("start() - Ném IllegalStateException nếu không ở trạng thái OPEN")
    void testStartAuctionThrowsExceptionWhenNotOpen() {
        auction.start(); // Đã chuyển sang RUNNING

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            auction.start();
        });
        assertTrue(exception.getMessage().contains("Chỉ có thể bắt đầu phiên đấu giá đang ở trạng thái OPEN"));
    }

    @Test
    @DisplayName("end() - Chuyển trạng thái sang FINISHED thành công")
    void testEndAuctionSuccess() {
        auction.start(); // Phải RUNNING mới end được
        auction.end();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertNotNull(auction.getEndTime());
    }

    @Test
    @DisplayName("placeBid() - Đặt giá thành công")
    void testPlaceBidSuccess() {
        auction.start(); // Bắt đầu phiên

        BiddingTransaction transaction = auction.placeBid(mockUser1, 150.0);

        assertNotNull(transaction);
        assertEquals(150.0, auction.getCurrentPrice().get());
        assertEquals(mockUser1, auction.getCurrentWinner());
        assertTrue(auction.hasBids());
        assertEquals(1, auction.getBidHistory().size());
        assertEquals(150.0, auction.getBidHistory().get(0).getBidAmount());
    }

    @Test
    @DisplayName("placeBid() - Ném AuctionClosedException khi phiên chưa RUNNING")
    void testPlaceBidThrowsExceptionWhenNotRunning() {
        // Mặc định auction đang OPEN, chưa start()
        AuctionClosedException exception = assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(mockUser1, 150.0);
        });

        assertEquals("Phiên đấu giá đã đóng hoặc chưa bắt đầu.", exception.getMessage());
    }

    @Test
    @DisplayName("placeBid() - Ném InvalidBidAmountException khi giá đặt thấp hơn hoặc bằng giá hiện tại")
    void testPlaceBidThrowsExceptionWhenAmountIsLower() {
        auction.start();

        InvalidBidAmountException exception = assertThrows(InvalidBidAmountException.class, () -> {
            auction.placeBid(mockUser1, 90.0); // Giá khởi điểm là 100.0
        });

        assertEquals("Giá đặt thấp hơn hoặc bằng giá hiện tại.", exception.getMessage());
    }

    @Test
    @DisplayName("placeBid() - Xử lý nhiều người đặt giá liên tiếp (Luồng cơ bản)")
    void testMultipleBidsSequential() {
        auction.start();

        auction.placeBid(mockUser1, 120.0);
        auction.placeBid(mockUser2, 150.0);
        auction.placeBid(mockUser1, 200.0);

        assertEquals(200.0, auction.getCurrentPrice().get());
        assertEquals(mockUser1, auction.getCurrentWinner());
        assertEquals(3, auction.getBidHistory().size());
    }
}