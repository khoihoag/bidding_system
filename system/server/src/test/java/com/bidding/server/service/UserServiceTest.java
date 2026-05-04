package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User activeUser;

    @BeforeEach
    void setUp() throws Exception {
        // TUYỆT CHIÊU REFLECTION: Ép UserRepository giả vào field private final
        Field field = UserService.class.getDeclaredField("repository");
        field.setAccessible(true);
        field.set(userService, userRepository);

        activeUser = new User("U001", "loi_uet", "loi@vnu.edu.vn", "pass123", "Trần Văn Lợi", UserRole.USER);
    }

    @Test
    @DisplayName("login: Đăng nhập thành công khi đúng tài khoản/mật khẩu và đang Active")
    void testLoginSuccess() {
        when(userRepository.findByUsernameAndPassword("loi_uet", "pass123")).thenReturn(activeUser);

        User result = userService.login("loi_uet", "pass123");

        assertNotNull(result);
        assertEquals("loi_uet", result.getUsername());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("login: Tung lỗi Runtime khi sai tài khoản hoặc mật khẩu")
    void testLoginWrongCredentials() {
        when(userRepository.findByUsernameAndPassword("hacker", "123")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.login("hacker", "123");
        });

        assertEquals("Sai tài khoản hoặc mật khẩu!", ex.getMessage());
    }

    @Test
    @DisplayName("login: Chặn đăng nhập nếu tài khoản đã bị Admin khóa")
    void testLoginInactive() {
        activeUser.setActive(false); // Cho bay màu
        when(userRepository.findByUsernameAndPassword("loi_uet", "pass123")).thenReturn(activeUser);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.login("loi_uet", "pass123");
        });

        assertEquals("Tài khoản của bạn đã bị Admin khóa mõm!", ex.getMessage());
    }

    @Test
    @DisplayName("register: Tạo User mới và lưu xuống DB với quyền USER mặc định")
    void testRegister() {
        userService.register("newbie", "pass", "new@gmail.com", "New Member");

        // Kiểm tra xem lệnh saveOrUpdate có được gọi với một User có role USER không
        verify(userRepository, times(1)).saveOrUpdate(argThat(user ->
                user.getUsername().equals("newbie") && user.getRole() == UserRole.USER
        ));
    }

    @Test
    @DisplayName("getBidHistory: Trả về danh sách lịch sử khi ID hợp lệ")
    void testGetBidHistorySuccess() {
        List<BiddingTransactionEntity> history = new ArrayList<>();
        when(userRepository.getBidHistoryByUserId("U001")).thenReturn(history);

        List<BiddingTransactionEntity> result = userService.getBidHistory("U001");

        assertNotNull(result);
        verify(userRepository, times(1)).getBidHistoryByUserId("U001");
    }

    @Test
    @DisplayName("getBidHistory: Tung lỗi khi ID truyền vào bị null hoặc trống")
    void testGetBidHistoryInvalidId() {
        assertThrows(RuntimeException.class, () -> userService.getBidHistory(""));
        assertThrows(RuntimeException.class, () -> userService.getBidHistory(null));
    }
}