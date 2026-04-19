package com.bidding.server.model.core;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class Entity {

    @Id
    // ĐÃ XÓA @GeneratedValue để không bị đánh nhau với Constructor
    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Entity() {
        // Tự sinh ID ngay trên RAM để các Service có cái ID xài lập tức
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // VŨ KHÍ TỰ ĐỘNG CỦA HIBERNATE
    // ==========================================

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // GETTERS & SETTERS ĐẦY ĐỦ CHO MAPSTRUCT
    // ==========================================

    public String getId() { return id; }
    // THÊM: Cực kỳ quan trọng để DB mapping ngược lên RAM
    public void setId(String id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
