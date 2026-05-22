package com.bidding.server.model.item;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.core.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "items") // 1. ÉP CỨNG TÊN BẢNG LÀ CHỮ THƯỜNG (SỐ NHIỀU)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Ép chung 1 bảng
@DiscriminatorColumn(
        name = "item_type", // Tên cột phân biệt dưới DB
        discriminatorType = DiscriminatorType.STRING
)
public abstract class Item extends Entity {

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "starting_price")
    private double startingPrice;

    // 2. CẤU HÌNH RÕ RÀNG BẢNG CHỨA ẢNH ĐỂ KHÔNG BỊ LỖI KHÓA NGOẠI
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "seller_full_name")
    private String sellerFullName;

    // 3. ĐỔI TÊN CỘT TRÁNH TỪ KHÓA CẤM "CONDITION" CỦA MYSQL 8
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ItemApprovalStatus approvalStatus = ItemApprovalStatus.APPROVED;

    @Column(name = "reviewed_by_admin_id")
    private String reviewedByAdminId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public Item() {
        super();
    }

    public Item(String id, String name, String description, double startingPrice, List<String> images, String sellerId, String sellerFullName, ItemCondition condition) {
        super();
        if (id != null && !id.isEmpty()) {
            this.setId(id);
        }
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.images = images;
        this.sellerId = sellerId;
        this.sellerFullName = sellerFullName;
        this.condition = condition;
        this.approvalStatus = ItemApprovalStatus.APPROVED;
    }

    public ItemApprovalStatus getEffectiveApprovalStatus() {
        return approvalStatus != null ? approvalStatus : ItemApprovalStatus.APPROVED;
    }

    public boolean isApprovedForAuction() {
        return getEffectiveApprovalStatus() == ItemApprovalStatus.APPROVED;
    }

    public abstract String getCategory();

    public abstract Map<String, String> getSpecifications();

    public void printInfo() {
        System.out.println("Item: " + name + " - Description: " + description + " - Starting Price: " + startingPrice + " - Condition: " + condition);
    }
}
