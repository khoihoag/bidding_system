package com.bidding.server.model.item;

import java.util.List;
import java.util.Map;

import com.bidding.server.model.enums.ItemCondition;
import com.bidding.server.model.user.User;
import com.bidding.server.model.entity.Entity;


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
    //getter
    public User getSeller() {
        return seller;
    }
    public List<String> getImages() {
        return images;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public String getDescription() {
        return description;
    }
    public String getName() {
        return name;
    }

    //setter
    public void setDescription(String description) {
        this.description = description;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    public void setImages(List<String> images) {
        this.images = images;
    }
    public void setSeller(User seller) {
        this.seller = seller;
    }
    public void setCondition(ItemCondition condition) {
        this.condition = condition;
    }

    public abstract String getCategory();
    
    public abstract Map<String, String> getSpecifications();
    
    public void printInfo() {
        System.out.println("Item: " + name + " - Description: " + description + " - Starting Price: " + startingPrice + " - Condition: " + condition);
    }       

}