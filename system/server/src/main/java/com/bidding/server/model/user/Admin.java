package com.bidding.server.model.user;

import com.bidding.server.enums.UserRole;
import jakarta.persistence.DiscriminatorValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@jakarta.persistence.Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    public Admin(String id, String username, String email, String passwordHash, String fullName) {
        super(id, username, email, passwordHash, fullName, UserRole.ADMIN);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    // Các hàm của admin (ban, unban, thống kê) giữ nguyên code cũ của ông nhé.
    // ... (Giữ nguyên phần method dưới này)
}
