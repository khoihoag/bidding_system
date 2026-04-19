package com.bidding.server.model.user;

import com.bidding.server.model.core.Entity;
import com.bidding.server.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import javax.management.relation.Role;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
@Setter
public class User extends Entity {

    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private boolean isActive;
    private Set<UserRole> roles = new HashSet<>();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public User() {
        super();
    }   

    public User(String username, String email, String passwordHash, String fullName) {
        super();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isActive = true;
    }

    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    @Override
    public void printInfo() {
        System.out.println("User: " + username + " - Email: " + email);
    }

    public void validate() {


        // 2. Validate Username
        if (this.username == null || this.username.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: Tên đăng nhập không được để trống.");
        }
        if (!USERNAME_PATTERN.matcher(this.username).matches()) {
            throw new IllegalArgumentException("Validation failed: Tên đăng nhập chỉ từ 4-20 ký tự, gồm chữ, số và dấu gạch dưới.");
        }

        // 3. Validate Email
        if (this.email == null || this.email.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: Email không được để trống.");
        }
        if (!EMAIL_PATTERN.matcher(this.email).matches()) {
            throw new IllegalArgumentException("Validation failed: Định dạng email không hợp lệ.");
        }

        // 4. Validate PasswordHash (Chỉ kiểm tra có dữ liệu hay không, format do BCrypt quyết định)
        if (this.passwordHash == null || this.passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: Mật khẩu chưa được thiết lập (Hash is null/empty).");
        }
    }
}
