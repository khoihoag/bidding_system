package com.bidding.server.model.user;

import com.bidding.server.model.entity.Entity;
import com.bidding.server.model.enums.UserRole;

import java.time.LocalDateTime;

public abstract class User extends Entity {

    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    protected UserRole role;
    private boolean isActive;

    public User() {
        super();
    }   

    public User(String id, String username, String email, String passwordHash, String fullName, UserRole role) {
        super();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
    }

    // --- Getters ---
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    // --- Setters ---
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }


    //Xác thực đăng nhập
    public boolean login(String pwd) {
        return this.passwordHash != null && this.passwordHash.equals(pwd);
    }

    //Xử lý đăng xuất
    public void logout() {
        // TODO: Implement logout logic
        // Cập nhật thời điểm thao tác cuối cùng của tài khoản thông qua method của Entity cha
        this.setUpdatedAt(LocalDateTime.now());

        // (Tùy chọn) Ghi log nghiệp vụ ở mức độ đối tượng
        // Trong hệ thống thực tế, thông tin này có thể được đẩy vào một Message Queue
        // hoặc bắt bởi Observer để Admin có thể xem qua getAuditLog()
        System.out.println(String.format("[AUDIT] User '%s' (Role: %s) initiated logout sequence at %s",
                this.username, this.role, this.getUpdatedAt()));

        // Trạng thái isActive được giữ nguyên vì nó đại diện cho Account Status, không phải Session Status
    }

    //Trả về trạng thái hoạt động
    public boolean isActive() {
        return isActive;
    }

    //Cập nhật trạng thái hoạt động (dùng cho ban/unban)
    public void setActive(boolean active) {
        this.isActive = active;
    }

    public abstract UserRole getRole();

    @Override
    public void printInfo() {
        System.out.println("User: " + username + " - Email: " + email);
    }
}
