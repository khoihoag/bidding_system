package com.bidding.server.network;

import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

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

        System.out.println("[UserService] User đăng nhập thành công: " + username);
        return user;
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