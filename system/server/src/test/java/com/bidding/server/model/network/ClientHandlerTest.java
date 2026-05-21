package com.bidding.server.model.network;

import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.ClientManager;
import com.bidding.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

// Báo cho JUnit biết chúng ta sẽ sử dụng sức mạnh của Mockito ở class này
@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

    // 1. LÀM GIẢ CÁC THÀNH PHẦN PHỤ THUỘC (MOCK)
    @Mock private Socket socket;
    @Mock private AuctionService tongQuan;
    @Mock private ClientManager loaPhuong;
    @Mock private UserService baoVe;
    @Mock private ItemService quanLyKho;
    @Mock private AdminService adminService;

    private ClientHandler clientHandler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws Exception {
        // Chuẩn bị một cái ống bơ (OutputStream) giả để hứng dữ liệu Server trả về cho Client
        outputStream = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(outputStream);
    }

    /**
     * Hàm tiện ích: Giả lập việc Client ném một chuỗi JSON qua đường truyền Socket.
     */
    private void simulateClientSending(String jsonInput) throws Exception {
        // Thêm \n vào cuối vì in.readLine() bên trong ClientHandler cần đọc tới khi xuống dòng
        ByteArrayInputStream inputStream = new ByteArrayInputStream((jsonInput + "\n").getBytes());
        when(socket.getInputStream()).thenReturn(inputStream);

        // Khởi tạo Handler với các Service đã được làm giả
        clientHandler = new ClientHandler(socket, tongQuan, loaPhuong, baoVe, quanLyKho, adminService);

        // Chạy trực tiếp hàm run() trên luồng hiện tại để test, không cần start Thread mới
        clientHandler.run();
    }

    @Test
    @DisplayName("Test luồng REGISTER: Nhận đúng JSON và gọi đúng UserService")
    void testHandleRegisterSuccess() throws Exception {
        // Chuẩn bị gói tin giả từ Client
        String requestJson = "{\"action\": \"REGISTER\", \"username\": \"testuser\", \"password\": \"123456\", \"email\": \"test@gmail.com\", \"fullName\": \"Test User\"}";

        // Bắn gói tin vào Handler
        simulateClientSending(requestJson);

        // KIỂM TRA 1: Đảm bảo ClientHandler đã nhờ "Bảo Vệ" (UserService) tạo tài khoản
        // Hàm verify của Mockito giúp kiểm tra xem một hàm có được gọi đúng 1 lần (times(1)) với đúng các tham số này không
        verify(baoVe, times(1)).register("testuser", "123456", "test@gmail.com", "Test User");

        // KIỂM TRA 2: Đảm bảo ClientHandler gửi tin nhắn báo thành công về cho Client
        String response = outputStream.toString().trim();
        assertTrue(response.contains("REGISTER_REPLY"));
        assertTrue(response.contains("SUCCESS"));
    }

    @Test
    @DisplayName("Test luồng Ngoại lệ: Client gửi action không tồn tại")
    void testHandleInvalidAction() throws Exception {
        // Action BAY_MAU không có trong khối switch-case
        String requestJson = "{\"action\": \"BAY_MAU\"}";

        simulateClientSending(requestJson);

        // Phải nhận được tin nhắn báo lỗi
        String response = outputStream.toString().trim();
        assertTrue(response.contains("ERROR"));
        assertTrue(response.contains("Lệnh action không tồn tại trên Server!"));
    }

    @Test
    @DisplayName("Test luồng Ngoại lệ: Client gửi JSON rách/sai format")
    void testHandleBadJsonFormat() throws Exception {
        // Thiếu dấu ngoặc nhọn đóng và cấu trúc hỏng bét
        String requestJson = "{\"action\": \"REGISTER\", \"username\": \"hacker\"";

        simulateClientSending(requestJson);

        // Khối try-catch tổng trong ClientHandler phải bắt được và báo lỗi
        String response = outputStream.toString().trim();
        assertTrue(response.contains("ERROR"));
        assertTrue(response.contains("Gửi JSON sai format hoặc lỗi Server!"));
    }

    @Test
    @DisplayName("Test luồng UPDATE_ITEM: Chặn sửa đồ khi chưa đăng nhập")
    void testUpdateItemWithoutLogin() throws Exception {
        String requestJson = "{\"action\": \"UPDATE_ITEM\", \"itemId\": \"item123\", \"name\": \"Tranh mới\", \"description\": \"Đẹp lắm\"}";

        // Bắn gói tin luôn mà chưa Login (loggedInUser = null)
        simulateClientSending(requestJson);

        String response = outputStream.toString().trim();
        assertTrue(response.contains("ERROR"));
        assertTrue(response.contains("Phải đăng nhập mới được sửa đồ!"));

        // Đảm bảo không có hàm updateItem nào của quanLyKho bị gọi lén
        verify(quanLyKho, never()).updateItem(any(), anyString(), any());
    }
}
