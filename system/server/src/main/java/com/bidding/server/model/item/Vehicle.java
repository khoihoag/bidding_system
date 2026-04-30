package com.bidding.server.model.item;
import com.bidding.server.model.user.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bidding.server.enums.ItemCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
@Entity
public class Vehicle extends Item {
    private String make;
    private String model;
    @Column(name = "vehicle_year")
    private int year;
    private int mileage;
    private String fuelType;

    public Vehicle() {
        super();
    }

    public Vehicle(String id, String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition, String make, String model, int year, int mileage, String fuelType) {
        super(id, name, description, startingPrice, images, seller, condition);
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


}

    
