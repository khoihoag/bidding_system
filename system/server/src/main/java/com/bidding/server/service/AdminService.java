package com.bidding.server.service;

import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.Admin;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.ItemRepository;
import com.bidding.server.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class AdminService {
    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public AdminService(UserRepository userRepository, AuctionService auctionService) {
        this(userRepository, auctionService, new ItemRepository());
    }

    public AdminService(UserRepository userRepository, AuctionService auctionService, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.auctionService = auctionService;
        this.itemRepository = itemRepository;
    }

    public int getAdminLevel(User actor) {
        if (actor == null || actor.getRole() != UserRole.ADMIN) {
            return 0;
        }
        if (actor instanceof Admin admin) {
            return admin.getAdminLevel();
        }
        return 2;
    }

    private void requireAdminLevel1(User actor) {
        if (getAdminLevel(actor) < 1) {
            throw new SecurityException("Yeu cau quyen Admin.");
        }
    }

    private void requireAdminLevel2(User actor) {
        if (getAdminLevel(actor) < 2) {
            throw new SecurityException("Chi Admin level 2 moi duoc thuc hien thao tac nay.");
        }
    }

    public User requireActiveAccount(User user) {
        if (user == null) {
            throw new SecurityException("Admin chua dang nhap.");
        }

        User freshUser = userRepository.findById(user.getId());
        if (freshUser == null || !freshUser.isActive()) {
            throw new SecurityException("Tai khoan admin nay da bi khoa.");
        }
        return freshUser;
    }

    private void requireCanModerateUser(User actor, User targetUser) {
        int actorLevel = getAdminLevel(actor);
        int targetLevel = getAdminLevel(targetUser);

        if (actor.getId().equals(targetUser.getId())) {
            throw new RuntimeException("Khong the tu khoa chinh minh.");
        }
        if (targetLevel >= 2) {
            throw new SecurityException("Khong the khoa Admin level 2.");
        }
        if (actorLevel == 1 && targetLevel > 0) {
            throw new SecurityException("Admin level 1 khong the khoa tai khoan admin khac.");
        }
    }

    public boolean banUser(User actor, String targetUserId) {
        requireAdminLevel1(actor);

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            return false;
        }

        requireCanModerateUser(actor, targetUser);
        targetUser.setActive(false);
        userRepository.saveOrUpdate(targetUser);
        return true;
    }

    public boolean unbanUser(User actor, String targetUserId) {
        requireAdminLevel1(actor);

        User targetUser = userRepository.findById(targetUserId);
        if (targetUser == null) {
            return false;
        }

        requireCanModerateUser(actor, targetUser);
        targetUser.setActive(true);
        userRepository.saveOrUpdate(targetUser);
        return true;
    }

    public void forceCloseAuction(User actor, String auctionId) {
        requireAdminLevel1(actor);
        auctionService.forceCloseManual(auctionId);
        System.out.println("[Admin] " + actor.getUsername() + " force closed auction: " + auctionId);
    }

    public List<User> getAllUsers(User actor) {
        requireAdminLevel1(actor);
        return userRepository.findAll();
    }

    public List<Item> getPendingItems(User actor) {
        requireAdminLevel1(actor);
        return itemRepository.findByApprovalStatus(ItemApprovalStatus.PENDING);
    }

    public Item approveItem(User actor, String itemId) {
        requireAdminLevel1(actor);
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new RuntimeException("Khong tim thay san pham.");
        }

        item.setApprovalStatus(ItemApprovalStatus.APPROVED);
        item.setReviewedByAdminId(actor.getId());
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(null);
        itemRepository.saveOrUpdate(item);
        return item;
    }

    public Item rejectItem(User actor, String itemId, String reason) {
        requireAdminLevel1(actor);
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new RuntimeException("Khong tim thay san pham.");
        }

        item.setApprovalStatus(ItemApprovalStatus.REJECTED);
        item.setReviewedByAdminId(actor.getId());
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(reason == null || reason.trim().isEmpty()
                ? "San pham khong duoc duyet."
                : reason.trim());
        itemRepository.saveOrUpdate(item);
        return item;
    }

    public Admin createAdminLevel1(User actor, String username, String password, String email, String fullName) {
        requireAdminLevel2(actor);
        validateRequired(username, "username");
        validateRequired(password, "password");
        validateRequired(email, "email");
        validateRequired(fullName, "fullName");

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();

        if (userRepository.findByUsername(normalizedUsername) != null) {
            throw new RuntimeException("Username da ton tai.");
        }
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw new RuntimeException("Email da ton tai.");
        }

        Admin newAdmin = new Admin(null, normalizedUsername, normalizedEmail, password.trim(), fullName.trim(), 1);
        userRepository.saveOrUpdate(newAdmin);
        return newAdmin;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " khong duoc de trong.");
        }
    }
}
