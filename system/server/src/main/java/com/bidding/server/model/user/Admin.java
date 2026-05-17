package com.bidding.server.model.user;

import com.bidding.server.enums.UserRole;
import jakarta.persistence.DiscriminatorValue;

@jakarta.persistence.Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    // ==========================================
    // HÀM KHỞI TẠO RỖNG (Cho Hibernate)
    // ==========================================
    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    // ==========================================
    // HÀM KHỞI TẠO ĐẦY ĐỦ (Dùng khi tạo Admin mới)
    // ==========================================
    public Admin(String id, String username, String email, String passwordHash, String fullName) {
        // Truyền thẳng UserRole.ADMIN lên cho hàm super() của thằng cha (User) xử lý
        super(id, username, email, passwordHash, fullName, UserRole.ADMIN);
    }
    // KHÔNG THÊM BẤT KỲ HÀM LOGIC NÀO VÀO ĐÂY NỮA!
    // Mọi thao tác quản trị phải được gọi qua AdminService.
}