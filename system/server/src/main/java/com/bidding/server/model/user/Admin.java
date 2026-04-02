package com.bidding.server.model.user;

import com.bidding.server.managers.SystemManager;
import com.bidding.server.model.SystemStats;
import com.bidding.server.model.enums.UserRole;

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
        // TODO: Implement unbanUser logic
        return false;
    }

    // Buộc đóng một phiên đấu giá
    public void forceCloseAuction(String id) {
        // TODO: Implement forceCloseAuction logic
    }

    // Lấy thống kê hệ thống
    public SystemStats getSystemStats() {
        // TODO: Implement getSystemStats logic
        return new SystemStats();
    }
    
    public List<String> getAuditLog() {
        // TODO: Implement getAuditLog logic
        return new ArrayList<>();
    }

    @Override
    public void validate() {
        // Validation logic for Admin
    }
}
