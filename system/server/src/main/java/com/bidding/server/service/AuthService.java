package com.bidding.server.service;

import com.bidding.server.model.user.User;
import com.bidding.server.repository.UserRepository;

public class AuthService {

    private final UserRepository userRepository;

    // Constructor Injection (Pure Java)
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String login(String username, String rawPassword) {
        // Trong thực tế, bạn nên hash rawPassword trước khi truyền vào hàm này.
        // Giả sử ở đây password truyền vào DB đang là bản rõ (hoặc đã được hash ở Client).
        User user = userRepository.findByUsernameAndPassword(username, rawPassword);

        if (user == null) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa.");
        }

        // Tạo JWT Token (Logic băm JWT tạm giả lập)
        return "jwt_token_for_" + user.getId();
    }

    public void logout(User actor, String token) {
        // Logic vô hiệu hóa token trên RAM hoặc Redis (nếu có)
        System.out.println("User " + actor.getUsername() + " đã đăng xuất.");
    }

    public boolean changePassword(User actor, String oldPwd, String newPwd) {
        // Kéo user từ DB lên cho chắc chắn
        User user = userRepository.findById(actor.getId());

        // Đối chiếu mật khẩu cũ (Giả sử so sánh chuỗi đơn giản)
        if (!user.getPasswordHash().equals(oldPwd)) {
            throw new RuntimeException("Mật khẩu cũ không chính xác.");
        }

        // Lưu mật khẩu mới xuống DB
        user.setPasswordHash(newPwd); // Thực tế cần: hash(newPwd)
        userRepository.saveOrUpdate(user);

        return true;
    }
}