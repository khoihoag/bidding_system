package com.bidding.server.controller;

import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.Admin;
import com.bidding.server.model.user.User;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.service.AdminService;
import com.bidding.server.service.AuctionService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public class AdminController {
    private final ClientHandler client;
    private final AdminService adminService;
    private final AuctionService tongQuan;

    public AdminController(ClientHandler client, AdminService adminService, AuctionService tongQuan) {
        this.client = client;
        this.adminService = adminService;
        this.tongQuan = tongQuan;
    }

    public void handleGetAllUsers() {
        if (!requireLogin("Admin vui long dang nhap!")) return;
        try {
            List<User> userList = adminService.getAllUsers(client.getLoggedInUser());
            JsonArray data = new JsonArray();
            for (User user : userList) {
                JsonObject userJson = new JsonObject();
                userJson.addProperty("id", user.getId());
                userJson.addProperty("username", user.getUsername());
                userJson.addProperty("email", user.getEmail());
                userJson.addProperty("role", user.getRole() != null ? user.getRole().name() : "");
                userJson.addProperty("adminLevel", adminService.getAdminLevel(user));
                userJson.addProperty("balance", user.getBalance());
                userJson.addProperty("active", user.isActive());
                data.add(userJson);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "USERS_LIST");
            reply.add("data", data);
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi tai danh sach nguoi dung: " + e.getMessage());
        }
    }

    public void handleBanUser(JsonObject request) {
        if (!requireLogin("Ban phai dang nhap tai khoan Admin!")) return;
        try {
            String targetUserId = request.get("targetUserId").getAsString();
            boolean success = adminService.banUser(client.getLoggedInUser(), targetUserId);

            if (success) {
                sendStatus("BAN_USER_REPLY", "SUCCESS", "Da khoa tai khoan thanh cong!");
                System.out.println("[Admin] " + client.getLoggedInUser().getUsername() + " banned user ID: " + targetUserId);
            } else {
                client.sendError("Khong tim thay nguoi dung co ID nay.");
            }
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi he thong khi thuc hien lenh cam: " + e.getMessage());
        }
    }

    public void handleUnbanUser(JsonObject request) {
        if (!requireLogin("Ban phai dang nhap tai khoan Admin!")) return;
        try {
            String targetUserId = request.get("targetUserId").getAsString();
            boolean success = adminService.unbanUser(client.getLoggedInUser(), targetUserId);

            if (success) {
                sendStatus("UNBAN_USER_REPLY", "SUCCESS", "Da mo khoa tai khoan thanh cong!");
                System.out.println("[Admin] " + client.getLoggedInUser().getUsername() + " unbanned user ID: " + targetUserId);
            } else {
                client.sendError("Khong tim thay nguoi dung co ID nay.");
            }
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi he thong khi thuc hien lenh mo khoa: " + e.getMessage());
        }
    }

    public void handleForceClose(JsonObject request) {
        if (!requireLogin("Admin chua dang nhap!")) return;
        try {
            String auctionId = request.get("auctionId").getAsString();
            adminService.forceCloseAuction(client.getLoggedInUser(), auctionId);
            sendStatus("FORCE_CLOSE_REPLY", "SUCCESS", "Da dong phien thanh cong.");
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi dong phien: " + e.getMessage());
        }
    }

    public void handleGetPendingItems() {
        if (!requireLogin("Admin chua dang nhap!")) return;
        try {
            JsonArray data = new JsonArray();
            for (Item item : adminService.getPendingItems(client.getLoggedInUser())) {
                data.add(toItemJson(item));
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "PENDING_ITEMS_LIST");
            reply.add("data", data);
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi tai danh sach san pham cho duyet: " + e.getMessage());
        }
    }

    public void handleApproveItem(JsonObject request) {
        if (!requireLogin("Admin chua dang nhap!")) return;
        try {
            String itemId = request.get("itemId").getAsString();
            Item item = adminService.approveItem(client.getLoggedInUser(), itemId);

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "APPROVE_ITEM_REPLY");
            reply.addProperty("status", "SUCCESS");
            reply.addProperty("message", "Da duyet san pham.");
            reply.add("item", toItemJson(item));
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi duyet san pham: " + e.getMessage());
        }
    }

    public void handleRejectItem(JsonObject request) {
        if (!requireLogin("Admin chua dang nhap!")) return;
        try {
            String itemId = request.get("itemId").getAsString();
            String reason = request.has("reason") && !request.get("reason").isJsonNull()
                    ? request.get("reason").getAsString()
                    : "";
            Item item = adminService.rejectItem(client.getLoggedInUser(), itemId, reason);

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "REJECT_ITEM_REPLY");
            reply.addProperty("status", "SUCCESS");
            reply.addProperty("message", "Da tu choi san pham.");
            reply.add("item", toItemJson(item));
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi tu choi san pham: " + e.getMessage());
        }
    }

    public void handleCreateAdminLevel1(JsonObject request) {
        if (!requireLogin("Admin chua dang nhap!")) return;
        try {
            Admin admin = adminService.createAdminLevel1(
                    client.getLoggedInUser(),
                    request.get("username").getAsString(),
                    request.get("password").getAsString(),
                    request.get("email").getAsString(),
                    request.get("fullName").getAsString()
            );

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "CREATE_ADMIN_LEVEL1_REPLY");
            reply.addProperty("status", "SUCCESS");
            reply.addProperty("message", "Da tao admin level 1.");
            reply.addProperty("id", admin.getId());
            reply.addProperty("username", admin.getUsername());
            reply.addProperty("adminLevel", admin.getAdminLevel());
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError(se.getMessage());
        } catch (Exception e) {
            client.sendError("Loi khi tao admin level 1: " + e.getMessage());
        }
    }

    private boolean requireLogin(String message) {
        if (client.getLoggedInUser() == null) {
            client.sendError(message);
            return false;
        }
        return true;
    }

    private void sendStatus(String action, String status, String message) {
        JsonObject reply = new JsonObject();
        reply.addProperty("action", action);
        reply.addProperty("status", status);
        reply.addProperty("message", message);
        client.sendMessage(reply.toString());
    }

    private JsonObject toItemJson(Item item) {
        JsonObject itemObj = new JsonObject();
        itemObj.addProperty("id", item.getId());
        itemObj.addProperty("name", item.getName());
        itemObj.addProperty("startingPrice", item.getStartingPrice());
        itemObj.addProperty("condition", item.getCondition() != null ? item.getCondition().toString() : "NEW");
        itemObj.addProperty("type", item.getCategory());
        itemObj.addProperty("description", item.getDescription());
        itemObj.addProperty("sellerId", item.getSellerId());
        itemObj.addProperty("sellerFullName", item.getSellerFullName());
        itemObj.addProperty("approvalStatus", item.getEffectiveApprovalStatus().name());
        itemObj.addProperty("reviewedByAdminId", item.getReviewedByAdminId());
        itemObj.addProperty("reviewedAt", item.getReviewedAt() != null ? item.getReviewedAt().toString() : "");
        itemObj.addProperty("rejectionReason", item.getRejectionReason());

        if (item.getImages() != null && !item.getImages().isEmpty()) {
            JsonArray imgArray = new JsonArray();
            for (String img : item.getImages()) {
                imgArray.add(img);
            }
            itemObj.add("images", imgArray);
        }

        JsonObject specsObj = new JsonObject();
        Map<String, String> specs = item.getSpecifications();
        if (specs != null) {
            for (Map.Entry<String, String> entry : specs.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    specsObj.addProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        itemObj.add("specifications", specsObj);
        return itemObj;
    }
}
