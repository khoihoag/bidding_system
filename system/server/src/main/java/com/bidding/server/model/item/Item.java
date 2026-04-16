package com.bidding.server.model.item;

import java.util.List;
import java.util.Map;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.user.User;
import com.bidding.server.model.core.Entity;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public abstract class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;
    private List<String> images;
    private User seller;
    private ItemCondition condition;

    public Item() {
        super();
    }

    public Item(String id, String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition) {
        super();
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