package com.bidding.server.model.user;

import com.bidding.server.exception.AuctionException;
import com.bidding.server.exception.InvalidBidAmountException;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.BidTransaction;
import com.bidding.server.enums.UserRole;

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
        if (amount > this.balance) {
            throw new InvalidBidAmountException("Số dư không đủ để thực hiện giao dịch.");
        }
        // Gọi hàm thread-safe placeBid từ class Auction
        return auction.placeBid(this, amount);
    }

    /**
     * Đấu giá tự động

    public void setAutoBid(Auction auction, AutoBidConfig config) {
        if (config == null || config.getMaxBid() <= 0) {
            throw new AuctionException("Cấu hình AutoBid không hợp lệ.");
        }
        this.autoBidConfigs.put(auction.getId(), config);
    }*/

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
        if (!this.watchList.contains(auction)) {
            this.watchList.add(auction);
        }
    }

    public List<BidTransaction> getActiveBids() {
        List<BidTransaction> activeWinningBids = new ArrayList<>();
        for (BidTransaction tx : bidHistory) {
            if (tx.isWinning()) {
                activeWinningBids.add(tx);
            }
        }
        return activeWinningBids;
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
        // 1. Gọi logic kiểm tra của lớp cha (username, email...)
        super.validate();

        // 2. Validate dữ liệu đặc thù của Bidder
        if (this.balance < 0) {
            throw new IllegalArgumentException("Lỗi dữ liệu: Số dư tài khoản (balance) không được âm.");
        }

        // Đảm bảo các Collection không bị null
        if (this.bidHistory == null) {
            throw new IllegalStateException("Lỗi hệ thống: Danh sách bidHistory chưa được khởi tạo.");
        }
        if (this.watchList == null) {
            throw new IllegalStateException("Lỗi hệ thống: Danh sách watchList chưa được khởi tạo.");
        }
    }
}
