package com.bidding.sever.model;

import java.time.LocalDateTime;

public abstract class Entity {

    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Entity() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }   

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public abstract void validate();

    public abstract void printInfo();

}
