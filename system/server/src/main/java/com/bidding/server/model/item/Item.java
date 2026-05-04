package com.bidding.server.model.item;
import com.bidding.server.model.user.User;
import java.util.List;
import java.util.Map;
import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.core.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Ép chung 1 bảng
@DiscriminatorColumn(
        name = "item_type", // Tên cột phân biệt dưới DB
        discriminatorType = DiscriminatorType.STRING // Kiểu dữ liệu của cột phân biệt
)

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image")
    private List<String> images;
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private ItemCondition condition;

    public Item() {
        super();
    }

    // Đã thay Seller thành User
    public Item(String id, String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition) {
        super();
        if (id != null && !id.isEmpty()) {
            this.setId(id); // Dùng setter của class Entity cha
        }
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.images = images;
        this.seller = seller;
        this.condition = condition;
    }

    public abstract String getCategory();

    public abstract Map<String, String> getSpecifications();

    public void printInfo() {
        System.out.println("Item: " + name + " - Description: " + description + " - Starting Price: " + startingPrice + " - Condition: " + condition);
    }
}