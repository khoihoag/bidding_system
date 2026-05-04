package com.bidding.server.repository;

import com.bidding.server.enums.ItemCondition;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Electronics;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemRepositoryTest {

    private ItemRepository itemRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        itemRepository = new ItemRepository();
        userRepository = new UserRepository();
    }

    private User createAndSaveTestUser() {
        String uniqueUsername = "seller_" + UUID.randomUUID().toString();
        User seller = new User(null, uniqueUsername, "seller@test.com", "pass", "Seller", UserRole.USER);
        userRepository.saveOrUpdate(seller);
        return seller;
    }

    @Test
    void should_save_and_findById() {
        // Arrange
        User seller = createAndSaveTestUser();
        Item item = new Electronics(null, "Test Laptop", "A gaming laptop", 1500.0, 
                Collections.emptyList(), seller, ItemCondition.NEW, "Dell", "Alienware", 16, 500);

        // Act
        itemRepository.saveOrUpdate(item);
        
        // Assert
        assertNotNull(item.getId());
        
        Item foundItem = itemRepository.findById(item.getId());
        assertNotNull(foundItem);
        assertEquals("Test Laptop", foundItem.getName());
        assertEquals(seller.getId(), foundItem.getSeller().getId());
    }

    @Test
    void should_update_existing_item() {
        // Arrange
        User seller = createAndSaveTestUser();
        Item item = new Electronics(null, "Old Laptop", "Old desc", 500.0, 
                Collections.emptyList(), seller, ItemCondition.USED, "HP", "Pavilion", 8, 256);
        itemRepository.saveOrUpdate(item);
        
        // Act
        item.setName("Updated Laptop");
        item.setStartingPrice(450.0);
        itemRepository.saveOrUpdate(item);
        
        // Assert
        Item updatedItem = itemRepository.findById(item.getId());
        assertNotNull(updatedItem);
        assertEquals("Updated Laptop", updatedItem.getName());
        assertEquals(450.0, updatedItem.getStartingPrice());
    }

    @Test
    void should_findBySellerId() {
        // Arrange
        User seller1 = createAndSaveTestUser();
        User seller2 = createAndSaveTestUser();
        
        Item item1 = new Electronics(null, "Item 1", "Desc", 100.0, Collections.emptyList(), seller1, ItemCondition.NEW, "B", "M", 8, 256);
        Item item2 = new Electronics(null, "Item 2", "Desc", 200.0, Collections.emptyList(), seller1, ItemCondition.NEW, "B", "M", 8, 256);
        Item item3 = new Electronics(null, "Item 3", "Desc", 300.0, Collections.emptyList(), seller2, ItemCondition.NEW, "B", "M", 8, 256);
        
        itemRepository.saveOrUpdate(item1);
        itemRepository.saveOrUpdate(item2);
        itemRepository.saveOrUpdate(item3);

        // Act
        List<Item> seller1Items = itemRepository.findBySellerId(seller1.getId());
        List<Item> seller2Items = itemRepository.findBySellerId(seller2.getId());

        // Assert
        assertNotNull(seller1Items);
        assertTrue(seller1Items.size() >= 2);
        boolean hasItem1 = seller1Items.stream().anyMatch(i -> i.getId().equals(item1.getId()));
        boolean hasItem2 = seller1Items.stream().anyMatch(i -> i.getId().equals(item2.getId()));
        assertTrue(hasItem1);
        assertTrue(hasItem2);
        
        assertNotNull(seller2Items);
        boolean hasItem3 = seller2Items.stream().anyMatch(i -> i.getId().equals(item3.getId()));
        assertTrue(hasItem3);
    }

    @Test
    void should_findAll_returnsList() {
        // Arrange
        User seller = createAndSaveTestUser();
        Item item1 = new Electronics(null, "FindAll Item 1", "Desc", 100.0, Collections.emptyList(), seller, ItemCondition.NEW, "B", "M", 8, 256);
        Item item2 = new Electronics(null, "FindAll Item 2", "Desc", 200.0, Collections.emptyList(), seller, ItemCondition.NEW, "B", "M", 8, 256);
        
        itemRepository.saveOrUpdate(item1);
        itemRepository.saveOrUpdate(item2);

        // Act
        List<Item> items = itemRepository.findAll();

        // Assert
        assertNotNull(items);
        assertTrue(items.size() >= 2);
        boolean found1 = items.stream().anyMatch(i -> i.getId().equals(item1.getId()));
        boolean found2 = items.stream().anyMatch(i -> i.getId().equals(item2.getId()));
        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    void should_delete_item() {
        // Arrange
        User seller = createAndSaveTestUser();
        Item item = new Electronics(null, "Item to Delete", "Desc", 100.0, Collections.emptyList(), seller, ItemCondition.NEW, "B", "M", 8, 256);
        itemRepository.saveOrUpdate(item);
        
        String itemId = item.getId();
        assertNotNull(itemRepository.findById(itemId));

        // Act
        itemRepository.delete(item);

        // Assert
        Item deletedItem = itemRepository.findById(itemId);
        assertNull(deletedItem);
    }
}
