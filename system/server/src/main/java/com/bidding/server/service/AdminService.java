package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.UserRepository;

import java.util.List;

public class AdminService {

    private final UserRepository userRepository;
    // Cần thêm các Repo khác như AuctionRepository nếu xử lý Force Close

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void requireAdmin(User actor) {
        if (!actor.hasRole(UserRole.ADMIN)) {
            throw new SecurityException("Yêu cầu quyền Quản trị viên.");
        }
    }

    public List<User> getAllUsers(User actor) {
        requireAdmin(actor);
        // Lưu ý: Bạn cần bổ sung hàm findAll() vào UserRepository nhé!
        // return userRepository.findAll();
        return null;
    }

    public boolean banUser(User actor, String targetUserId) {
        requireAdmin(actor);
        if (actor.getId().equals(targetUserId)) {
            throw new RuntimeException("Không thể tự khóa chính mình.");
        }

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser != null) {
            targetUser.setActive(false);
            userRepository.saveOrUpdate(targetUser); // Gọi DAO cất vào DB
            return true;
        }
        return false;
    }

    public boolean unbanUser(User actor, String targetUserId) {
        requireAdmin(actor);

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser != null) {
            targetUser.setActive(true);
            userRepository.saveOrUpdate(targetUser); // Gọi DAO cất vào DB
            return true;
        }
        return false;
    }

    // Các hàm khác giữ khung
    public void forceCloseAuction(User actor, String auctionId) {
        requireAdmin(actor);
        // Gọi AuctionRepository để đổi Status
    }
}