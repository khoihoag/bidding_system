package com.bidding.model;

public class UserSession {

    private static UserSession instance;
    private double balance;
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private int adminLevel;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public String getUserId()   { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }
    public String getRole()     { return role; }
    public int getAdminLevel()  { return adminLevel; }

    public void setUserId(String userId)     { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email)       { this.email = email; }
    public void setRole(String role)         { this.role = role; }
    public void setAdminLevel(int adminLevel) { this.adminLevel = adminLevel; }

    public void clear() {
        userId = null;
        username = null;
        fullName = null;
        email = null;
        role = null;
        adminLevel = 0;
    }
}
