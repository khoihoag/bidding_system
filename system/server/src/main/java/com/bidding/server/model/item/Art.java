package com.bidding.server.model.item;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.model.user.User;

public class Art extends Item {

    private String artist;
    private String medium;
    private int yearCreated;
    private String dimensions;
    
    public Art() {
        super();
    }

    public Art(String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition, String artist, String medium, int yearCreated, String dimensions) {
        super(name, description, startingPrice, images, seller.getId(), condition);
        this.artist = artist;
        this.medium = medium;
        this.yearCreated = yearCreated;
        this.dimensions = dimensions;
    }
    //getter

    public String getMedium() {
        return medium;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    //setter
    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    

    @Override
    public String getCategory() {
        return "Art";
    }
    
    @Override
    public Map<String, String> getSpecifications() {
        Map<String, String> specifications = new HashMap<>();
        specifications.put("Artist", artist);
        specifications.put("Medium", medium);
        specifications.put("Year Created", String.valueOf(yearCreated));
        specifications.put("Dimensions", dimensions);
        return specifications;
    }

    @Override
    public void validate() {
        if (this.artist == null || this.artist.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tác giả/Nghệ sĩ không được để trống.");
        }
        if (this.yearCreated > LocalDateTime.now().getYear()) {
            throw new IllegalArgumentException("Năm sáng tác không thể nằm trong tương lai.");
        }
        if (this.dimensions == null || !this.dimensions.matches("\\d+x\\d+(x\\d+)?.*")) {
            // Validate định dạng kích thước cơ bản (VD: 100x200 hoặc 100x200x50 cm)
            throw new IllegalArgumentException("Kích thước (Dimensions) sai định dạng.");
        }
    }
}
