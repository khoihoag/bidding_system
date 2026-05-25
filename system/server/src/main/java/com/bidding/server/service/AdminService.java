package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.repository.UserRepository;
import java.util.List;

public class AdminService {
    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    // Cần thêm các Repo khác như AuctionRepository nếu xử lý Force Close

    public AdminService(UserRepository userRepository, AuctionService auctionService) {
        this.userRepository = userRepository;
        this.auctionService = auctionService;
        this.auctionRepository = auctionService.getAuctionRepository();
    }

    private void requireAdmin(User actor) {
        if (!actor.hasRole(UserRole.ADMIN)) {
            throw new SecurityException("Yêu cầu quyền Quản trị viên.");
        }
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
        // 1. Kiểm tra xem thằng gọi lệnh có phải Admin class không (như ông đề xuất)
        requireAdmin(actor);
        // 2. Nhờ Tổng quản đấu giá thực hiện đóng phiên ngay lập tức
        auctionService.forceCloseManual(auctionId);
        System.out.println("[Admin] " + actor.getUsername() + " đã cưỡng chế đóng phiên: " + auctionId);
    }
    // Thêm vào AdminService.java
    public List<User> getAllUsers(User actor) {
        // Chặn cửa: Chỉ Admin class mới được đi tiếp
        requireAdmin(actor);

        // Gọi cái hàm mình vừa thêm ở trên
        return userRepository.findAll();
    }

    public List<BiddingTransactionEntity> getAuctionBidHistory(User actor, String auctionId) {
        requireAdmin(actor);
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("Thiếu ID phiên đấu giá.");
        }
        return auctionRepository.findBidHistoryByAuctionId(auctionId);
    }
}
