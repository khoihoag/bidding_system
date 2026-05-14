package com.bidding.server.service;

import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.repository.UserRepository;
import com.bidding.server.enums.UserRole;
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
    // ================= XỬ LÝ VÍ TIỀN (BALANCE) =================
    public void deductBalance(User user, double amount) {
        if (user.getBalance() < amount) {
            throw new RuntimeException("Ví của bạn không đủ tiền! Vui lòng nạp thêm.");
        }
        // Trừ tiền trên RAM
        user.setBalance(user.getBalance() - amount);

        // Lưu ngay lập tức xuống Database
        repository.saveOrUpdate(user);
        System.out.println("[Ví Tiền] Đã trừ " + amount + " từ tài khoản " + user.getUsername());
    }

    public void addBalance(User user, double amount) {
        // Cộng tiền trên RAM
        user.setBalance(user.getBalance() + amount);

        // Lưu DB
        repository.saveOrUpdate(user);
        System.out.println("[Ví Tiền] Đã hoàn/cộng " + amount + " vào tài khoản " + user.getUsername());
    }

    // ================= XỬ LÝ ĐỒNG BỘ TIỀN =================
    public double getFreshBalance(String userId) {
        // Nhờ Kho xuống tận Database lấy thông tin user tươi rói lên
        User u = repository.findById(userId);
        return u != null ? u.getBalance() : 0.0;
    }

    // Sau này ông có thể thêm các hàm như: register(User user), changePassword()... vào đây
}