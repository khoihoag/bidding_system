package com.bidding.server.model.user;

import com.bidding.server.exception.AuctionException;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.bid.BidTransaction;
import com.bidding.server.model.enums.UserRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bidder extends User {

    private double balance;
    private List<BidTransaction> bidHistory;
    private List<Auction> watchList;
    private Map<String, Object> autoBidConfigs; // Sử dụng Object cho config auto-bid tạm thời

    public Bidder() {
        super();
        this.balance = 0.0;
        this.bidHistory = new ArrayList<>();
        this.watchList = new ArrayList<>();
        this.autoBidConfigs = new HashMap<>();
    }

    public Bidder(String id, String username, String email, String passwordHash, String fullName) {
        super(id, username, email, passwordHash, fullName, UserRole.BIDDER);
        this.balance = 0.0;
        this.bidHistory = new ArrayList<>();
        this.watchList = new ArrayList<>();
        this.autoBidConfigs = new HashMap<>();
    }

    // --- Getters & Setters ---
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public List<Auction> getWatchList() {
        return watchList;
    }

    public void setWatchList(List<Auction> watchList) {
        this.watchList = watchList;
    }

    public Map<String, Object> getAutoBidConfigs() {
        return autoBidConfigs;
    }

    public void setAutoBidConfigs(Map<String, Object> autoBidConfigs) {
        this.autoBidConfigs = autoBidConfigs;
    }

    // --- Các phương thức chức năng cốt lõi ---
    /**
     * Đấu giá thủ công
     */
    public BidTransaction placeBid(Auction auction, double amount) {
        // Đặt giá thủ công: Cần kiểm tra logic số dư, auction status, v.v.
        BidTransaction transaction = new BidTransaction(this, auction, amount);
        this.bidHistory.add(transaction);
        return transaction;
    }

    /**
     * Đấu giá tự động
     */
    public void setAutoBid(Auction auction, Object config) {
        if (auction != null && auction.getId() != null) {
            this.autoBidConfigs.put(auction.getId(), config);
        }
    }

    /**
     * Hủy cấu hình đấu giá tự động.
     */
    public void cancelAutoBid(String auctionId) {
        this.autoBidConfigs.remove(auctionId);
    }

    /**
     * Thêm phiên đấu giá vào danh sách theo dõi.
     */
    public void watchAuction(Auction auction) {
        if (auction != null && !this.watchList.contains(auction)) {
            this.watchList.add(auction);
        }
    }

    public List<BidTransaction> getActiveBids() {
        // TODO: Logic lấy danh sách các bid đang dẫn đầu
        // Hiện tại trả về toàn bộ bidHistory như placeholder
        return new ArrayList<>(this.bidHistory);
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    @Override
    public void printInfo() {
        System.out.println("ID: " + this.getId());
        System.out.println("Username: " + this.getUsername());
        System.out.println("Balance: $" + this.balance);
        System.out.println("Active AutoBids: " + this.autoBidConfigs.size());
    }

    @Override
    public void validate() {
        // 1. Validate dữ liệu kế thừa từ User (Nên đưa vào User.validate() và gọi super.validate())
        if (this.getUsername() == null || this.getUsername().trim().isEmpty()) {
            throw new AuctionException("Username không được để trống.");
        }
        if (this.getEmail() == null || !this.getEmail().contains("@")) {
            throw new AuctionException("Định dạng Email không hợp lệ.");
        }

        // 2. Validate dữ liệu riêng của Bidder
        if (this.balance < 0) {
            throw new AuctionException("Số dư tài khoản (balance) không được âm.");
        }
    }
}
