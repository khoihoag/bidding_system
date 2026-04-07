package com.bidding.server.service;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.user.User;
import com.bidding.server.model.SystemStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Lớp giả lập hệ thống quản lý backend để Admin thử nghiệm
public class SystemManager {
    private static SystemManager instance;

    private Map<String, User> users;
    private Map<String, Auction> auctions;
    private List<String> auditLog;

    private SystemManager() {
        this.users = new HashMap<>();
        this.auctions = new HashMap<>();
        this.auditLog = new ArrayList<>();
        this.auditLog.add("Hệ thống khởi tạo thành công.");
    }

    public static SystemManager getInstance() {
        if (instance == null) {
            instance = new SystemManager();
        }
        return instance;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public User getUserById(String id) {
        return users.get(id);
    }

    public Auction getAuctionById(String id) {
        return auctions.get(id);
    }

    public SystemStats getStats() {
        // Trả về thống kê tính toán từ users và auctions (giả định)
        return new SystemStats();
    }

    public List<String> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    public void addAuditLog(String log) {
        this.auditLog.add(log);
    }
}
