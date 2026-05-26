package com.bidding.server.model.user;

import com.bidding.server.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;

@jakarta.persistence.Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Column(name = "admin_level")
    private Integer adminLevel;

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
        this.adminLevel = 1;
    }

    public Admin(String id, String username, String email, String passwordHash, String fullName) {
        this(id, username, email, passwordHash, fullName, 1);
    }

    public Admin(String id, String username, String email, String passwordHash, String fullName, int adminLevel) {
        super(id, username, email, passwordHash, fullName, UserRole.ADMIN);
        setAdminLevel(adminLevel);
    }

    public int getAdminLevel() {
        return adminLevel != null ? adminLevel : 2;
    }

    public void setAdminLevel(Integer adminLevel) {
        if (adminLevel == null) {
            this.adminLevel = null;
            return;
        }
        if (adminLevel != 1 && adminLevel != 2) {
            throw new IllegalArgumentException("Admin level chi hop le la 1 hoac 2.");
        }
        this.adminLevel = adminLevel;
    }
}
