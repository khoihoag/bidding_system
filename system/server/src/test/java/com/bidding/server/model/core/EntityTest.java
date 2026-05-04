package com.bidding.server.model.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    /**
     * Lớp giả để test vì Entity là abstract class.
     */
    static class DummyEntity extends Entity {
        // Không cần thêm code gì, chỉ mượn vỏ để test lớp cha
    }

    private DummyEntity entity;

    @BeforeEach
    void setUp() {
        entity = new DummyEntity();
    }

    @Test
    @DisplayName("Khởi tạo Entity thành công: Có sẵn UUID và thời gian tạo")
    void testConstructorGeneratesInitialValues() {
        assertNotNull(entity.getId(), "ID không được null");
        assertFalse(entity.getId().isEmpty(), "ID không được rỗng");

        // Kiểm tra xem ID sinh ra có đúng chuẩn cấu trúc UUID không (36 ký tự)
        assertEquals(36, entity.getId().length(), "ID phải là chuỗi UUID chuẩn");

        assertNotNull(entity.getCreatedAt(), "Thời gian tạo không được null");
        assertNotNull(entity.getUpdatedAt(), "Thời gian cập nhật không được null");
    }

    @Test
    @DisplayName("Setters và Getters hoạt động trơn tru (Dành cho MapStruct và Hibernate)")
    void testGettersAndSetters() {
        String newId = "custom-id-12345";
        LocalDateTime customTime = LocalDateTime.of(2026, 5, 2, 10, 30);

        entity.setId(newId);
        entity.setCreatedAt(customTime);
        entity.setUpdatedAt(customTime);

        assertEquals(newId, entity.getId());
        assertEquals(customTime, entity.getCreatedAt());
        assertEquals(customTime, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("@PrePersist: Giữ nguyên createdAt nếu đã có, chỉ làm mới updatedAt")
    void testOnCreateWhenCreatedAtAlreadyExists() throws InterruptedException {
        LocalDateTime initialCreatedAt = entity.getCreatedAt();

        // Ngủ 10ms để giả lập thời gian trôi qua, đảm bảo updatedAt mới sẽ khác initialCreatedAt
        Thread.sleep(10);

        // Giả lập Hibernate gọi hàm trước khi INSERT
        entity.onCreate();

        assertEquals(initialCreatedAt, entity.getCreatedAt(), "createdAt phải được giữ nguyên");
        assertTrue(entity.getUpdatedAt().isAfter(initialCreatedAt), "updatedAt phải được làm mới và diễn ra sau createdAt");
    }

    @Test
    @DisplayName("@PrePersist: Tự động điền createdAt nếu đang bị null")
    void testOnCreateWhenCreatedAtIsNull() {
        entity.setCreatedAt(null); // Cố tình làm rỗng

        entity.onCreate(); // Giả lập Hibernate gọi

        assertNotNull(entity.getCreatedAt(), "Hibernate phải tự động điền createdAt nếu bị null");
        assertNotNull(entity.getUpdatedAt(), "updatedAt phải được cập nhật");
    }

    @Test
    @DisplayName("@PreUpdate: Làm mới thời gian updatedAt khi có thay đổi")
    void testOnUpdate() throws InterruptedException {
        LocalDateTime initialUpdatedAt = entity.getUpdatedAt();

        // Ngủ 10ms để đảm bảo mốc thời gian chênh lệch
        Thread.sleep(10);

        // Giả lập Hibernate gọi hàm trước khi UPDATE
        entity.onUpdate();

        assertTrue(entity.getUpdatedAt().isAfter(initialUpdatedAt), "updatedAt phải mang mốc thời gian mới hơn initialUpdatedAt");
    }
}