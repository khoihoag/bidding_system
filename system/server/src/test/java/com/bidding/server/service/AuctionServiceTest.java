package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.events.AuctionObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    // 1. "Ảo thuật": Mockito sẽ dùng Reflection để nhét bản giả này vào
    // trường 'repository' đang bị private final trong AuctionService.
    @Mock
    private AuctionRepository repository;

    @Mock
    private AuctionObserver observer;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuctionService auctionService;

    private User seller;
    private Item testItem;

    @BeforeEach
    void setUp() {
        seller = new User("U1", "seller", "email", "pass", "Seller Name", UserRole.USER);
        testItem = new com.bidding.server.model.item.Art();
        testItem.setId("ITEM_001");
        testItem.setName("Bức tranh quý");
        testItem.setSellerId(seller.getId());
        testItem.setSellerFullName(seller.getFullName());
        testItem.setStartingPrice(1000.0);
        testItem.setBidStep(100.0);
        auctionService.setUserService(userService);
    }

    @Test
    @DisplayName("startAuction: Mở phiên thành công và lưu vào Database")
    void testStartAuctionSuccess() {
        // Thực hiện lệnh
        Auction result = auctionService.startAuction(testItem, 10);

        // Kiểm tra
        assertNotNull(result);
        assertEquals(testItem.getId(), result.getItem().getId());

        // Xác nhận AuctionService đã gọi lệnh saveOrUpdate xuống Repository đúng 1 lần
        verify(repository, times(1)).saveOrUpdate(any());

        // Kiểm tra xem nó đã nằm trong danh sách Active chưa
        assertTrue(auctionService.isItemInActiveAuction("ITEM_001"));
    }

    @Test
    @DisplayName("startAuction: Không cho phép mở 2 phiên cho cùng 1 món hàng")
    void testStartAuctionDuplicateItem() {
        // Mở phiên lần 1
        auctionService.startAuction(testItem, 10);

        // Mở phiên lần 2 với cùng 1 item phải tung ra ngoại lệ
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            auctionService.startAuction(testItem, 5);
        });

        assertEquals("Sản phẩm này đang được đấu giá rồi!", exception.getMessage());
    }

    @Test
    @DisplayName("placeBid: Đặt giá thành công và thông báo cho Observer")
    void testPlaceBidSuccess() {
        // Đăng ký "Loa phường" giả để theo dõi
        auctionService.addObserver(observer);

        // Tạo phiên trước
        Auction auction = auctionService.startAuction(testItem, 10);
        String auctionId = auction.getId();

        // Người mua đặt giá
        User bidder = new User("U2", "bidder", "mail", "pass", "Buyer", UserRole.USER);
        auctionService.placeBid(auctionId, 1500.0, bidder);

        // Kiểm tra: Repository phải được update giá mới
        verify(repository, atLeastOnce()).saveOrUpdate(any());

        // Kiểm tra: Observer phải nhận được sự kiện BID_PLACED
        verify(observer, times(1)).onBidPlaced(any());
        verify(userService, never()).deductBalance(any(), anyDouble());
        verify(userService, never()).addBalance(any(), anyDouble());
    }

    @Test
    @DisplayName("placeBid: Tung lỗi khi đấu giá phiên không tồn tại")
    void testPlaceBidNotFound() {
        User bidder = new User();
        assertThrows(RuntimeException.class, () -> {
            auctionService.placeBid("ID_MA_MA_NAO_DO", 100.0, bidder);
        });
    }

    @Test
    @DisplayName("forceCloseManual: Đóng phiên thủ công")
    void testForceCloseManual() {
        auctionService.addObserver(observer);
        Auction auction = auctionService.startAuction(testItem, 10);

        auctionService.forceCloseManual(auction.getId());

        // Kiểm tra: Observer phải nhận được sự kiện AUCTION_CLOSED
        verify(observer, times(1)).onAuctionClosed(any());
        // Repository phải lưu trạng thái đóng
        verify(repository, atLeastOnce()).saveOrUpdate(any());
    }
}
