package com.bidding.server.model.item;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.user.User;



public class Vehicle extends Item {
    private String make;
    private String model;
    private int year;
    private int mileage;
    private String fuelType;

    public Vehicle() {
        super();
    }

    public Vehicle(String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition, String make, String model, int year, int mileage, String fuelType) {
        super(name, description, startingPrice, images, seller.getId(), condition);
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
    }

    //getter
    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public int getMileage() {
        return mileage;
    }

    public String getFuelType() {
        return fuelType;
    }

    //setter
    public void setMake(String make) {
        this.make = make;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
    
    public String getCategory() {
        return "Vehicle";
    }

    public Map<String, String> getSpecifications() {
        Map<String, String> specifications = new HashMap<>();
        specifications.put("Make", make);
        specifications.put("Model", model);
        specifications.put("Year", String.valueOf(year));
        specifications.put("Mileage", String.valueOf(mileage));
        specifications.put("Fuel Type", fuelType);
        return specifications;
    }

    @Override
    public void validate() {
        // Validate cấu trúc nội tại của Vehicle
        if (this.make == null || this.make.trim().isEmpty()) {
            throw new IllegalArgumentException("Hãng xe (Make) không được để trống.");
        }
        if (this.year < 1886 || this.year > LocalDateTime.now().getYear() + 1) {
            // Xe ô tô đầu tiên ra đời năm 1886
            throw new IllegalArgumentException("Năm sản xuất không hợp lệ: " + this.year);
        }
        if (this.mileage < 0) {
            throw new IllegalArgumentException("Số KM đã đi không thể là số âm.");
        }
    }
}
    