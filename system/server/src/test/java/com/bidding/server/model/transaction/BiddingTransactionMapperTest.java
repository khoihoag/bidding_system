package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.Auction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Bật tính năng Mockito cho file test này
@ExtendWith(MockitoExtension.class)
class BiddingTransactionMapperTest {

    // 1. Tạo 2 đối tượng ảo (Né được lệnh 'new' gây lỗi)
    @Mock
    private BiddingTransaction mockModel;

    @Mock
    private Auction mockAuction;

    @Test
    @DisplayName("Test Mapper: Dùng Mockito lách luật không cần sửa Model")
    void testMappingWithUses() {
        // 2. "Dạy" cho đối tượng ảo trả về dữ liệu (Né được hàm 'set')
        // Lưu ý: ID mình để 123L cho đúng với kiểu Long trong class của bạn
        when(mockModel.getId()).thenReturn(123L);
        when(mockModel.getBidAmount()).thenReturn(500.0);
        when(mockModel.getBidTime()).thenReturn(LocalDateTime.now());

        // "Dạy" cho Auction ảo trả về ID
        when(mockAuction.getId()).thenReturn("AUC_999");

        // Nhét Auction ảo vào trong Model ảo
        when(mockModel.getAuction()).thenReturn(mockAuction);

        // 3. Tiến hành map (MapStruct sẽ tự động gọi các hàm get...() ở trên)
        BiddingTransactionEntity entity = BiddingTransactionMapper.INSTANCE.toEntity(mockModel);

        // 4. Kiểm tra kết quả xem MapStruct làm việc chuẩn không
        assertNotNull(entity);
        assertEquals(123L, entity.getId());
        assertEquals(500.0, entity.getBidAmount());
        assertNotNull(entity.getAuction(), "MapStruct phải tự dịch được Auction sang AuctionEntity");
        assertEquals("AUC_999", entity.getAuction().getId());
    }
}