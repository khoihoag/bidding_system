package com.bidding.server.model.user;

import com.bidding.server.model.entity.Entity;

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
    // thay đổi thông tin cá nhân
    public void changePassword(String newPassword) {
        this.passwordHash = newPassword;
    }

    public void changeEmail(String newEmail) {
        this.email = newEmail;
    }

    public void changeFullName(String newFullName) {
        this.fullName = newFullName;
    }
    
    public void changeUsername(String newUsername) {
        this.username = newUsername;
    }

    //Xác thực đăng nhập
    public boolean login(String pwd) {
        return this.passwordHash != null && this.passwordHash.equals(pwd);
    }

    //Xử lý đăng xuất
    public void logout() {
    }

    public abstract UserRole getRole();

    //Trả về trạng thái hoạt động
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void printInfo() {
        System.out.println("User: " + username + " - Email: " + email);
    }
}
