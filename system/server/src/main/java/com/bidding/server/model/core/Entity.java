package com.bidding.server.model.core;

import java.time.LocalDateTime;

public abstract class Entity {

    private final String id;

    private final LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Entity() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }   
    //getter
    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }

    //setter
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public abstract void validate();

    public abstract void printInfo();

}
