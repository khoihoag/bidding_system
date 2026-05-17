package com.bidding.server.model.user;

import com.bidding.server.model.core.Entity;
import com.bidding.server.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Vẫn dùng Single Table
@DiscriminatorColumn(name = "user_type") // Cột phân loại trong DB
@DiscriminatorValue("USER") // Định danh mặc định cho user thường
public class User extends Entity {

    private String username;
    private String email;
    private String passwordHash;
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    protected UserRole role;

    private boolean isActive;

    private double balance;
    private double totalRevenue;

    // ==========================================
    // HÀM KHỞI TẠO
    // ==========================================
    public User() {
        super();
    }

    public User(String id, String username, String email, String passwordHash, String fullName, UserRole role) {
        super();
        if (id != null && !id.isEmpty()) {
            this.setId(id);
        }
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        this.balance = 0.0;
        this.totalRevenue = 0.0;
    }

    // ==========================================
    // HÀM TIỆN ÍCH KIỂM TRA QUYỀN
    // ==========================================
    public boolean hasRole(UserRole targetRole) {
        return this.role != null && this.role == targetRole;
    }
}
