package com.bidding.server.model.user;

import com.bidding.server.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Khởi tạo rỗng (Default Constructor) phải có sẵn ID")
    void testDefaultConstructor() {
        User user = new User();

        // ID được kế thừa từ Entity cha, phải tự sinh ra ngay trên RAM
        assertNotNull(user.getId(), "ID không được để trống");
    }

    @Test
    @DisplayName("Khởi tạo đầy đủ: Gán đúng thông tin và thiết lập giá trị mặc định chuẩn")
    void testParameterizedConstructor() {
        String customId = "U_999";
        User user = new User(
                customId,
                "TrinhPhu",
                "phu@gmail.com",
                "hash123",
                "Trịnh Đông Phú",
                UserRole.USER
        );

        // Kiểm tra thông tin cơ bản
        assertEquals(customId, user.getId());
        assertEquals("TrinhPhu", user.getUsername());
        assertEquals("phu@gmail.com", user.getEmail());
        assertEquals(UserRole.USER, user.getRole());

        // Kiểm tra các giá trị mặc định phải được set tự động
        assertTrue(user.isActive(), "User mới tạo mặc định phải Active");
        assertEquals(0.0, user.getBalance(), "Số dư ban đầu phải là 0");
        assertEquals(0.0, user.getTotalRevenue(), "Doanh thu ban đầu phải là 0");
    }

    @Test
    @DisplayName("hasRole() hoạt động chính xác khi kiểm tra quyền")
    void testHasRole() {
        User user = new User();
        user.setRole(UserRole.ADMIN);

        assertTrue(user.hasRole(UserRole.ADMIN), "Phải trả về true nếu đúng role ADMIN");
        assertFalse(user.hasRole(UserRole.USER), "Phải trả về false nếu truyền sai role");

        // Trường hợp role đang bị null (chưa set)
        User emptyUser = new User();
        assertFalse(emptyUser.hasRole(UserRole.ADMIN), "Phải xử lý an toàn, trả về false nếu role đang null");
    }
}
