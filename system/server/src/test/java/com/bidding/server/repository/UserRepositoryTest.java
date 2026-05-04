package com.bidding.server.repository;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
    }

    @Test
    void should_save_and_findById() {
        // Arrange
        String uniqueUsername = "testuser_" + UUID.randomUUID().toString();
        User user = new User(null, uniqueUsername, "test@test.com", "pass123", "Test User", UserRole.USER);

        // Act
        userRepository.saveOrUpdate(user);
        
        // Assert
        assertNotNull(user.getId());
        
        User foundUser = userRepository.findById(user.getId());
        assertNotNull(foundUser);
        assertEquals(uniqueUsername, foundUser.getUsername());
        assertEquals("Test User", foundUser.getFullName());
        assertEquals(UserRole.USER, foundUser.getRole());
    }

    @Test
    void should_update_existing_user() {
        // Arrange
        String uniqueUsername = "updateuser_" + UUID.randomUUID().toString();
        User user = new User(null, uniqueUsername, "update@test.com", "pass123", "Old Name", UserRole.USER);
        userRepository.saveOrUpdate(user);
        
        // Act
        user.setFullName("New Name");
        user.setActive(false);
        userRepository.saveOrUpdate(user);
        
        // Assert
        User updatedUser = userRepository.findById(user.getId());
        assertNotNull(updatedUser);
        assertEquals("New Name", updatedUser.getFullName());
        assertFalse(updatedUser.isActive());
    }

    @Test
    void should_findByUsernameAndPassword() {
        // Arrange
        String username = "loginuser_" + UUID.randomUUID().toString();
        String password = "securepassword";
        User user = new User(null, username, "login@test.com", password, "Login User", UserRole.USER);
        userRepository.saveOrUpdate(user);

        // Act
        User foundUser = userRepository.findByUsernameAndPassword(username, password);
        User notFoundUser = userRepository.findByUsernameAndPassword(username, "wrongpass");

        // Assert
        assertNotNull(foundUser);
        assertEquals(username, foundUser.getUsername());
        
        assertNull(notFoundUser);
    }

    @Test
    void should_findAll_returnsList() {
        // Arrange
        String username1 = "findall1_" + UUID.randomUUID().toString();
        String username2 = "findall2_" + UUID.randomUUID().toString();
        userRepository.saveOrUpdate(new User(null, username1, "1@test.com", "pass", "Name1", UserRole.USER));
        userRepository.saveOrUpdate(new User(null, username2, "2@test.com", "pass", "Name2", UserRole.USER));

        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertNotNull(users);
        assertTrue(users.size() >= 2);
        
        boolean found1 = users.stream().anyMatch(u -> username1.equals(u.getUsername()));
        boolean found2 = users.stream().anyMatch(u -> username2.equals(u.getUsername()));
        
        assertTrue(found1);
        assertTrue(found2);
    }
}
