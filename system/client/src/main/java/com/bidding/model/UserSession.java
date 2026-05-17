package com.bidding.model;

public class UserSession {

    private static UserSession instance;
    private double balance;
    private String userId;
    private String username;
    private String role;

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
    public String getRole()     { return role; }

    public void setUserId(String userId)     { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role)         { this.role = role; }

    public void clear() {
        userId = null;
        username = null;
        role = null;
    }
}
