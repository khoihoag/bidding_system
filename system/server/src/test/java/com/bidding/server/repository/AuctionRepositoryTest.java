package com.bidding.server.repository;

import com.bidding.server.enums.AuctionStatus;
import com.bidding.server.enums.ItemCondition;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.item.Electronics;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRepositoryTest {

    private AuctionRepository auctionRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        auctionRepository = new AuctionRepository();
        itemRepository = new ItemRepository();
        userRepository = new UserRepository();
    }

    private User createAndSaveTestUser(String prefix) {
        String uniqueUsername = prefix + "_" + UUID.randomUUID().toString();
        User user = new User(null, uniqueUsername, prefix + "@test.com", "pass", "Name", UserRole.USER);
        userRepository.saveOrUpdate(user);
        return user;
    }

    private Item createAndSaveTestItem(User seller) {
        Item item = new Electronics(null, "Auction Item " + UUID.randomUUID(), "Desc", 100.0, 
                Collections.emptyList(), seller, ItemCondition.NEW, "Brand", "Model", 8, 256);
        itemRepository.saveOrUpdate(item);
        return item;
    }

    @Test
    void should_save_and_findById() {
        // Arrange
        User seller = createAndSaveTestUser("seller");
        Item item = createAndSaveTestItem(seller);

        AuctionEntity auction = new AuctionEntity();
        auction.setItem(item);
        auction.setStatus(AuctionStatus.OPEN);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setCurrentPrice(150.0);
        auction.setAntiSnipingSeconds(30);
        auction.setExtensionSeconds(60);

        // Act
        auctionRepository.saveOrUpdate(auction);

        // Assert
        assertNotNull(auction.getId());

        AuctionEntity foundAuction = auctionRepository.findById(auction.getId());
        assertNotNull(foundAuction);
        assertEquals(AuctionStatus.OPEN, foundAuction.getStatus());
        assertEquals(150.0, foundAuction.getCurrentPrice());
        assertEquals(item.getId(), foundAuction.getItem().getId());
    }

    @Test
    void should_update_existing_auction() {
        // Arrange
        User seller = createAndSaveTestUser("seller");
        Item item = createAndSaveTestItem(seller);

        AuctionEntity auction = new AuctionEntity();
        auction.setItem(item);
        auction.setStatus(AuctionStatus.OPEN);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setCurrentPrice(100.0);
        auctionRepository.saveOrUpdate(auction);

        User winner = createAndSaveTestUser("winner");

        // Act
        auction.setStatus(AuctionStatus.FINISHED);
        auction.setCurrentPrice(500.0);
        auction.setCurrentWinner(winner);
        auctionRepository.saveOrUpdate(auction);

        // Assert
        AuctionEntity updatedAuction = auctionRepository.findById(auction.getId());
        assertNotNull(updatedAuction);
        assertEquals(AuctionStatus.FINISHED, updatedAuction.getStatus());
        assertEquals(500.0, updatedAuction.getCurrentPrice());
        assertEquals(winner.getId(), updatedAuction.getCurrentWinner().getId());
    }

    @Test
    void should_findAll_returnsList() {
        // Arrange
        User seller = createAndSaveTestUser("seller");
        Item item1 = createAndSaveTestItem(seller);
        Item item2 = createAndSaveTestItem(seller);

        AuctionEntity auction1 = new AuctionEntity();
        auction1.setItem(item1);
        auction1.setStatus(AuctionStatus.OPEN);
        auctionRepository.saveOrUpdate(auction1);

        AuctionEntity auction2 = new AuctionEntity();
        auction2.setItem(item2);
        auction2.setStatus(AuctionStatus.OPEN);
        auctionRepository.saveOrUpdate(auction2);

        // Act
        List<AuctionEntity> auctions = auctionRepository.findAll();

        // Assert
        assertNotNull(auctions);
        assertTrue(auctions.size() >= 2);
        
        boolean found1 = auctions.stream().anyMatch(a -> a.getId().equals(auction1.getId()));
        boolean found2 = auctions.stream().anyMatch(a -> a.getId().equals(auction2.getId()));
        
        assertTrue(found1);
        assertTrue(found2);
    }
}
