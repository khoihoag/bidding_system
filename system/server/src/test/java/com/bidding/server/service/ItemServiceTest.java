package com.bidding.server.service;

import com.bidding.server.enums.UserRole;
import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.model.item.Art;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private User owner;
    private User hacker;
    private Item testItem;

    @BeforeEach
    void setUp() {
        // Khởi tạo User theo đúng Enum USER/ADMIN của hệ thống
        owner = new User("U001", "owner", "owner@uet.vnu.edu.vn", "hash", "Chủ hàng", UserRole.USER);
        hacker = new User("U666", "hacker", "hacker@gmail.com", "hash", "Kẻ gian", UserRole.USER);

        // Sử dụng Art làm đối tượng Item cụ thể để test
        testItem = new Art();
        testItem.setId("ITEM_123");
        testItem.setName("Bức tranh gốc");
        testItem.setDescription("Mô tả cũ");
        testItem.setSellerId(owner.getId());
        testItem.setSellerFullName(owner.getFullName());
    }

    @Test
    @DisplayName("createItem: Gán đúng người bán và lưu vào kho")
    void testCreateItem() {
        Item requestItem = new Art();

        Item result = itemService.createItem(owner, requestItem);

        assertEquals(owner.getId(), result.getSellerId());
        assertEquals(owner.getFullName(), result.getSellerFullName());
        assertEquals(ItemApprovalStatus.PENDING, result.getApprovalStatus());
        verify(itemRepository, times(1)).saveOrUpdate(requestItem);
    }

    @Test
    @DisplayName("updateItem: Sửa thông tin thành công khi đúng chủ sở hữu")
    void testUpdateItemSuccess() {
        // Giả lập tìm thấy item trong DB
        when(itemRepository.findById("ITEM_123")).thenReturn(testItem);

        Item updateData = new Art();
        updateData.setName("Tên mới");
        updateData.setDescription("Mô tả mới");

        Item result = itemService.updateItem(owner, "ITEM_123", updateData);

        assertEquals("Tên mới", result.getName());
        assertEquals("Mô tả mới", result.getDescription());
        verify(itemRepository, times(1)).saveOrUpdate(testItem);
    }

    @Test
    @DisplayName("updateItem: Chặn đứng hành vi sửa đồ của người khác")
    void testUpdateItemSecurityException() {
        when(itemRepository.findById("ITEM_123")).thenReturn(testItem);
        Item updateData = new Art();

        assertThrows(SecurityException.class, () -> {
            itemService.updateItem(hacker, "ITEM_123", updateData);
        });

        verify(itemRepository, never()).saveOrUpdate(any());
    }

    @Test
    @DisplayName("deleteItem: Xóa thành công và trả về true")
    void testDeleteItemSuccess() {
        when(itemRepository.findById("ITEM_123")).thenReturn(testItem);

        boolean isDeleted = itemService.deleteItem(owner, "ITEM_123");

        assertTrue(isDeleted);
        verify(itemRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("getMyItems: Lấy đúng danh sách đồ của người dùng")
    void testGetMyItems() {
        List<Item> expectedItems = Arrays.asList(testItem);
        when(itemRepository.findBySellerId(owner.getId())).thenReturn(expectedItems);

        List<Item> result = itemService.getMyItems(owner);

        assertEquals(1, result.size());
        assertEquals("ITEM_123", result.get(0).getId());
        verify(itemRepository, times(1)).findBySellerId(owner.getId());
    }
}
