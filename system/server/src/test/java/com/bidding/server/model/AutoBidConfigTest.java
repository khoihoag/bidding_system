package com.bidding.server.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {

    @Test
    @DisplayName("Khởi tạo cấu hình thành công với các giá trị hợp lệ")
    void testConstructorSuccess() {
        AutoBidConfig config = new AutoBidConfig(500.0, 10.0);

        assertEquals(500.0, config.getMaxBid());
        assertEquals(10.0, config.getIncrement());
        assertTrue(config.isActive());
        assertNotNull(config.getRegisteredAt());
    }

    @Test
    @DisplayName("Khởi tạo ném Exception nếu maxBid hoặc increment <= 0")
    void testConstructorThrowsExceptionWhenValuesInvalid() {
        // Trường hợp maxBid <= 0
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(0.0, 10.0);
        });
        assertTrue(exception1.getMessage().contains("Giá tối đa và bước giá phải lớn hơn 0"));

        // Trường hợp increment <= 0
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(500.0, -5.0);
        });
        assertTrue(exception2.getMessage().contains("Giá tối đa và bước giá phải lớn hơn 0"));
    }

    @Test
    @DisplayName("getNextBidAmount() tính toán đúng mức giá đặt tiếp theo")
    void testGetNextBidAmount() {
        AutoBidConfig config = new AutoBidConfig(500.0, 15.0);

        // Giá hiện tại 100, bước giá 15 -> Giá tiếp theo: 115
        assertEquals(115.0, config.getNextBidAmount(100.0));
    }

    @Test
    @DisplayName("canBid() trả về true nếu còn active và chưa chạm đỉnh maxBid")
    void testCanBidSuccess() {
        AutoBidConfig config = new AutoBidConfig(500.0, 50.0);

        // Giá hiện tại 400 + 50 = 450 (Vẫn nhỏ hơn 500) -> Hợp lệ
        assertTrue(config.canBid(400.0));

        // Giá hiện tại 450 + 50 = 500 (Vừa đúng bằng maxBid) -> Hợp lệ
        assertTrue(config.canBid(450.0));
    }

    @Test
    @DisplayName("canBid() trả về false nếu giá tiếp theo vượt quá maxBid")
    void testCanBidFailsWhenMaxBidExceeded() {
        AutoBidConfig config = new AutoBidConfig(500.0, 50.0);

        // Giá hiện tại 460 + 50 = 510 (Vượt ngưỡng 500) -> Không hợp lệ
        assertFalse(config.canBid(460.0));
    }

    @Test
    @DisplayName("canBid() trả về false nếu cấu hình đã bị hủy (deactivate)")
    void testCanBidFailsWhenDeactivated() {
        AutoBidConfig config = new AutoBidConfig(500.0, 50.0);
        config.deactivate(); // Hủy cấu hình

        // Mặc dù giá hợp lệ (100 + 50 = 150 < 500) nhưng cấu hình không còn active
        assertFalse(config.canBid(100.0));
        assertFalse(config.isActive());
    }

    @Test
    @DisplayName("compareTo() so sánh chuẩn xác thời gian: Ai đăng ký trước được ưu tiên")
    void testCompareTo() throws InterruptedException {
        // Người thứ 1 tạo cấu hình
        AutoBidConfig config1 = new AutoBidConfig(500.0, 10.0);

        // Dừng (ngủ) luồng 10 milliseconds để đảm bảo người thứ 2 chắn chắn đăng ký sau
        Thread.sleep(10);

        // Người thứ 2 tạo cấu hình
        AutoBidConfig config2 = new AutoBidConfig(600.0, 20.0);

        // config1 đăng ký trước nên có giá trị nhỏ hơn (ưu tiên đứng trước trong hàng đợi)
        assertTrue(config1.compareTo(config2) < 0, "Cấu hình 1 phải đứng trước cấu hình 2");

        // config2 đăng ký sau
        assertTrue(config2.compareTo(config1) > 0, "Cấu hình 2 phải đứng sau cấu hình 1");

        // So sánh với chính nó
        assertEquals(0, config1.compareTo(config1));
    }
}