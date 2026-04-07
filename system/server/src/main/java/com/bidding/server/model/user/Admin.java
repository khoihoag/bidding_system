package com.bidding.server.model.user;

import com.bidding.server.model.SystemStats;
import com.bidding.server.enums.UserRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Admin extends User {
    public Admin() {
        super();
    }

    public Admin(String id, String username, String email, String passwordHash, String fullName) {
        super(id, username, email, passwordHash, fullName, UserRole.ADMIN);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    // Các tính năng đặc quyền của Admin
    public List<User> getAllUsers() {
        return new ArrayList<>();
    }

    // Ban một người dùng
    public boolean banUser(String userId) {
        return false;
    }

    // Mở khóa một người dùng
    public boolean unbanUser(String userId) {
        // chỉ để test sua ghi vào log
        System.out.println(String.format("[AUDIT] Admin '%s' requested to UNBAN user ID: %s at %s",
                this.getUsername(), userId, LocalDateTime.now()));

        // TODO: Thực tế cần gọi tầng Service/DAO.
        // Ví dụ: return UserService.getInstance().unbanUser(userId);
        return true;
    }

    // Buộc đóng một phiên đấu giá
    public void forceCloseAuction(String id) {
        // chỉ để test sua ghi vào log
        System.out.println(String.format("[AUDIT] Admin '%s' FORCE CLOSED auction ID: %s at %s",
                this.getUsername(), id, LocalDateTime.now()));

        // Theo tài liệu[cite: 41], chúng ta ủy quyền cho AuctionManager xử lý thread-safe
        // AuctionManager.getInstance().forceCloseAuction(id);

    }

    // Lấy thống kê hệ thống
    public SystemStats getSystemStats() {
        // TODO: Gọi SystemMonitorService hoặc tổng hợp từ các DAO
        return new SystemStats();
    }

    public List<String> getAuditLog() {
        // TODO: Đọc từ file log hoặc bảng Audit trong Database
        return new ArrayList<>();
    }

    @Override
    public void validate() {
        super.validate();
    }
}
