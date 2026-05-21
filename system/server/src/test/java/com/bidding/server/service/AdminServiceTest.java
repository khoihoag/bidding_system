package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionService auctionService;

    @Mock
    private User mockAdmin;

    @Mock
    private User mockTargetUser;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, auctionService);
    }

    @Test
    @DisplayName("Test 1 (Chặn cửa): Người dùng thường cố dùng quyền Admin và cái kết")
    void testRequireAdminFails() {
        // Giả lập: Người gọi lệnh KHÔNG phải Admin
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(false);

        assertThrows(SecurityException.class, () -> {
            adminService.getAllUsers(mockAdmin);
        });
    }

    @Test
    @DisplayName("Test 2 (Khóa User): Admin thực hiện khóa tài khoản vi phạm")
    void testBanUserSuccess() {
        // Giả lập: Người gọi là Admin, ID là "admin-01"
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(true);
        when(mockAdmin.getId()).thenReturn("admin-01");

        // Mục tiêu là user "user-99"
        String targetId = "user-99";
        when(userRepository.findById(targetId)).thenReturn(mockTargetUser);

        boolean result = adminService.banUser(mockAdmin, targetId);

        assertTrue(result);
        verify(mockTargetUser).setActive(false); // Kiểm tra xem đã set active = false chưa
        verify(userRepository).saveOrUpdate(mockTargetUser); // Kiểm tra xem đã lưu vào DB chưa
    }

    @Test
    @DisplayName("Test 3 (Tự hủy): Admin cố tình khóa chính mình và bị hệ thống chặn")
    void testBanSelfFails() {
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(true);
        when(mockAdmin.getId()).thenReturn("admin-01");

        // Cố tình ban chính mình bằng cách truyền ID của mình vào target
        assertThrows(RuntimeException.class, () -> {
            adminService.banUser(mockAdmin, "admin-01");
        });
    }

    @Test
    @DisplayName("Test 3.1 (Mở khóa User): Admin thực hiện mở khóa tài khoản")
    void testUnbanUserSuccess() {
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(true);

        String targetId = "user-99";
        when(userRepository.findById(targetId)).thenReturn(mockTargetUser);

        boolean result = adminService.unbanUser(mockAdmin, targetId);

        assertTrue(result);
        verify(mockTargetUser).setActive(true);
        verify(userRepository).saveOrUpdate(mockTargetUser);
    }

    @Test
    @DisplayName("Test 4 (Cưỡng chế đóng phiên): Admin dùng quyền lực đóng phiên đấu giá ngay lập tức")
    void testForceCloseAuction() {
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(true);
        String auctionId = "AUC-101";

        adminService.forceCloseAuction(mockAdmin, auctionId);

        // Kiểm tra xem đã gọi sang AuctionService để đóng phiên chưa
        verify(auctionService).forceCloseManual(auctionId);
    }

    @Test
    @DisplayName("Test 5 (Xem danh sách): Admin yêu cầu xem toàn bộ cư dân trong hệ thống")
    void testGetAllUsers() {
        when(mockAdmin.hasRole(UserRole.ADMIN)).thenReturn(true);
        List<User> list = List.of(mockTargetUser);
        when(userRepository.findAll()).thenReturn(list);

        List<User> result = adminService.getAllUsers(mockAdmin);

        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }
}
