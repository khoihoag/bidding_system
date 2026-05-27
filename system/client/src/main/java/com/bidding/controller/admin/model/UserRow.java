package com.bidding.controller.admin.model;

/**
 * Table row model for the admin users panel.
 */
public class UserRow {
    private final String id;
    private final String username;
    private final String email;
    private final String role;
    private final double balance;
    private final boolean active;

    public UserRow(String id, String username, String email, String role, double balance, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public double getBalance() {
        return balance;
    }

    public String getBalanceFormatted() {
        return String.format("VNĐ %,.0f", balance);
    }

    public boolean isActive() {
        return active;
    }
}
