package com.bidding.server.model.item;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Electronics extends Item {

    private String brand;
    private String model;
    private int warrantyMonths;
    private int powerWatts;

    public Electronics() {
        super();
    }

    public Electronics(String id, String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition, String brand, String model, int warrantyMonths, int powerWatts) {
        super(id, name, description, startingPrice, images, seller, condition);
        this.brand = brand;
        this.model = model;
        this.warrantyMonths = warrantyMonths;
        this.powerWatts = powerWatts;
    }
    //getter
    public int getPowerWatts() {
        return powerWatts;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public String getModel() {
        return model;
    }

    public String getBrand() {
        return brand;
    }

    //setter
    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public void setPowerWatts(int powerWatts) {
        this.powerWatts = powerWatts;
    }
    
    @Override
    public String getCategory() {
        return "Electronics";
    }
    
    @Override
    public Map<String, String> getSpecifications() {
        Map<String, String> specifications = new HashMap<>();
        specifications.put("Brand", brand);
        specifications.put("Model", model);
        specifications.put("Warranty Months", String.valueOf(warrantyMonths));
        specifications.put("Power Watts", String.valueOf(powerWatts));
        return specifications;
    }

    @Override
    public void validate() {
        // TODO: Implement validate logic
    }
}
