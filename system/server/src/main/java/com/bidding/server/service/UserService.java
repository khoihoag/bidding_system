package com.bidding.server.service;

import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.repository.UserRepository;
import com.bidding.server.enums.UserRole;
import java.util.List;

public class UserService {
    private final UserRepository repository;

    public UserService() {
        this.repository = new UserRepository();
    }

    // Constructor dùng cho test (Mockito @InjectMocks sẽ dùng cái này)
    UserService(UserRepository repository) {
        this.repository = repository;
    }

    // ================= XỬ LÝ LOGIN =================
    public User login(String username, String password) {
        // Nhờ Kho tìm xem có thằng nào tên pass thế này không
        User user = repository.findByUsernameAndPassword(username, password);

        if (user == null) {
            throw new RuntimeException("Sai tài khoản hoặc mật khẩu!");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản của bạn đã bị Admin khóa mõm!");
        }

        System.out.println("[UserService.java] User đăng nhập thành công: " + username);
        return user;
    }
    // ================= XỬ LÝ ĐĂNG KÝ =================
    public void register(String username, String password, String email, String fullName) {
        // Tạo một Object User mới toanh.
        // Truyền null vào vị trí ID để Entity cha tự động đẻ ra UUID mới.
        // Role mặc định chắc chắn là USER thường rồi.
        User newUser = new User(null, username, email, password, fullName, UserRole.USER);

        // Gọi Thủ kho cất hồ sơ xuống Database
        repository.saveOrUpdate(newUser);

        System.out.println("[UserService] Giang hồ mới gia nhập: " + username);
    }
    // ================= XỬ LÝ LỊCH SỬ =================
    public List<BiddingTransactionEntity> getBidHistory(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("ID người dùng không hợp lệ!");
        }
        return repository.getBidHistoryByUserId(userId);
    }

    // Sau này ông có thể thêm các hàm như: register(User user), changePassword()... vào đây
}