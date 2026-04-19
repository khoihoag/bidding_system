package com.bidding.server.model.user;

import com.bidding.server.model.core.Entity;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.transaction.BiddingTransaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Vẫn dùng Single Table
@DiscriminatorColumn(name = "user_type") // Cột phân loại trong DB
@DiscriminatorValue("USER") // Định danh mặc định cho user thường
public class User extends Entity { // Đã XÓA chữ abstract

    private String username;
    private String email;
    private String passwordHash;
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    protected UserRole role;

    private boolean isActive;

    // ==========================================
    // TÀI SẢN MUA (Của Bidder cũ)
    // ==========================================
    private double balance;

    @Transient
    private List<BiddingTransaction> bidHistory;

    @Transient
    private List<Auction> watchList;

    @Transient
    private Map<String, Object> autoBidConfigs;

    // ==========================================
    // TÀI SẢN BÁN (Của Seller cũ)
    // ==========================================
    private double totalRevenue;

    // Lưu ý: Nếu trong class Item ông đang dùng biến tên là 'seller',
    // thì để mappedBy = "seller". Nếu đã đổi thành 'owner', thì mappedBy="owner".
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;

    @Transient
    private Map<String, Item> listedItems;

    @Transient
    private Map<String, Auction> activeAuctions;

    // ==========================================
    // HÀM KHỞI TẠO
    // ==========================================
    public User() {
        super();
        initCollections();
    }

    public User(String id, String username, String email, String passwordHash, String fullName, UserRole role) {
        super();
        // --- THÊM 3 DÒNG NÀY ĐỂ FIX LỖI MẤT ID KHI LOAD TỪ DB ---
        if (id != null && !id.isEmpty()) {
            this.setId(id);
        }
        // --------------------------------------------------------
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        initCollections();
    }

    private void initCollections() {
        this.balance = 0.0;
        this.totalRevenue = 0.0;
        this.bidHistory = new ArrayList<>();
        this.watchList = new ArrayList<>();
        this.autoBidConfigs = new HashMap<>();
        this.items = new ArrayList<>();
        this.listedItems = new HashMap<>();
        this.activeAuctions = new HashMap<>();
    }
}
