package com.bidding.server.service;

import com.bidding.server.enums.ItemApprovalStatus;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.user.Admin;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.repository.ItemRepository;
import com.bidding.server.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

public class AdminService {
    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    public AdminService(UserRepository userRepository, AuctionService auctionService) {
        this(userRepository, auctionService, null, null);
    }

    public AdminService(UserRepository userRepository, AuctionService auctionService, ItemRepository itemRepository) {
        this(userRepository, auctionService, auctionService.getAuctionRepository(), itemRepository);
    }

    public AdminService(UserRepository userRepository,
                        AuctionService auctionService,
                        AuctionRepository auctionRepository,
                        ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.auctionService = auctionService;
        this.auctionRepository = auctionRepository;
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
            throw new SecurityException("Chỉ Admin level 2 mới được thực hiện thao tác này.");
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
        if (actor.getId() != null && actor.getId().equals(targetUserId)) {
            throw new RuntimeException("Khong the tu khoa chinh minh.");
        }

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

    public List<BiddingTransactionEntity> getAuctionBidHistory(User actor, String auctionId) {
        requireAdminLevel1(actor);
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("Thiếu ID phiên đấu giá.");
        }
        return auctionRepository().findBidHistoryByAuctionId(auctionId);
    }

    public List<Item> getPendingItems(User actor) {
        requireAdminLevel1(actor);
        return itemRepository().findByApprovalStatus(ItemApprovalStatus.PENDING);
    }

    public Item approveItem(User actor, String itemId) {
        requireAdminLevel1(actor);
        Item item = itemRepository().findById(itemId);
        if (item == null) {
            throw new RuntimeException("Khong tim thay san pham.");
        }

        item.setApprovalStatus(ItemApprovalStatus.APPROVED);
        item.setReviewedByAdminId(actor.getId());
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(null);
        itemRepository().saveOrUpdate(item);
        return item;
    }

    public Item rejectItem(User actor, String itemId, String reason) {
        requireAdminLevel1(actor);
        Item item = itemRepository().findById(itemId);
        if (item == null) {
            throw new RuntimeException("Khong tim thay san pham.");
        }

        item.setApprovalStatus(ItemApprovalStatus.REJECTED);
        item.setReviewedByAdminId(actor.getId());
        item.setReviewedAt(LocalDateTime.now());
        item.setRejectionReason(reason == null || reason.trim().isEmpty()
                ? "San pham khong duoc duyet."
                : reason.trim());
        itemRepository().saveOrUpdate(item);
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
        String normalizedPassword = password.trim();

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new RuntimeException("Email không đúng định dạng.");
        }
        if (!PASSWORD_PATTERN.matcher(normalizedPassword).matches()) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự, gồm cả chữ và số.");
        }

        if (userRepository.findByUsername(normalizedUsername) != null) {
            throw new RuntimeException("Username đã tồn tại.");
        }
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw new RuntimeException("Email đã tồn tại.");
        }

        Admin newAdmin = new Admin(null, normalizedUsername, normalizedEmail, normalizedPassword, fullName.trim(), 1);
        userRepository.saveOrUpdate(newAdmin);
        return newAdmin;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldDisplayName(fieldName) + " không được để trống.");
        }
    }

    private String fieldDisplayName(String fieldName) {
        return switch (fieldName) {
            case "username" -> "Username";
            case "password" -> "Mật khẩu";
            case "email" -> "Email";
            case "fullName" -> "Họ và tên";
            default -> fieldName;
        };
    }

    private AuctionRepository auctionRepository() {
        return auctionRepository != null ? auctionRepository : auctionService.getAuctionRepository();
    }

    private ItemRepository itemRepository() {
        return itemRepository != null ? itemRepository : new ItemRepository();
    }
}
