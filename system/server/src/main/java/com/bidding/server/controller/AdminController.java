package com.bidding.server.controller;

import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.user.User;
import com.bidding.server.service.AdminService;
import com.bidding.server.service.AuctionService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
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
        if (client.getLoggedInUser() == null) {
            client.sendError("Admin vui lòng đăng nhập!");
            return;
        }
        try {
            List<User> userList = adminService.getAllUsers(client.getLoggedInUser());
            JsonArray data = new JsonArray();
            for (User user : userList) {
                JsonObject userJson = new JsonObject();
                userJson.addProperty("id", user.getId());
                userJson.addProperty("username", user.getUsername());
                userJson.addProperty("email", user.getEmail());
                userJson.addProperty("role", user.getRole() != null ? user.getRole().name() : "");
                userJson.addProperty("balance", user.getBalance());
                userJson.addProperty("active", user.isActive());
                data.add(userJson);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "USERS_LIST");
            reply.add("data", data);
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError("Cảnh báo: Bạn không có quyền truy cập danh sách người dùng!");
        } catch (Exception e) {
            client.sendError("Lỗi khi tải danh sách người dùng: " + e.getMessage());
        }
    }

    public void handleBanUser(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập tài khoản Admin!");
            return;
        }
        try {
            String targetUserId = request.get("targetUserId").getAsString();
            boolean success = adminService.banUser(client.getLoggedInUser(), targetUserId);

            if (success) {
                client.sendMessage("{\"action\": \"BAN_USER_REPLY\", \"status\": \"SUCCESS\", \"message\": \"Đã khóa tài khoản thành công!\"}");
                System.out.println("[Admin] " + client.getLoggedInUser().getUsername() + " đã thực thi lệnh cấm với ID: " + targetUserId);
            } else {
                client.sendError("Không tìm thấy người dùng có ID này.");
            }
        } catch (SecurityException se) {
            client.sendError("Cảnh báo: Bạn không có quyền quản trị viên!");
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống khi thực hiện lệnh cấm: " + e.getMessage());
        }
    }

    public void handleUnbanUser(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Bạn phải đăng nhập tài khoản Admin!");
            return;
        }
        try {
            String targetUserId = request.get("targetUserId").getAsString();
            boolean success = adminService.unbanUser(client.getLoggedInUser(), targetUserId);

            if (success) {
                JsonObject reply = new JsonObject();
                reply.addProperty("action", "UNBAN_USER_REPLY");
                reply.addProperty("status", "SUCCESS");
                reply.addProperty("message", "Đã mở khóa tài khoản thành công!");
                client.sendMessage(reply.toString());
                System.out.println("[Admin] " + client.getLoggedInUser().getUsername() + " đã mở khóa user ID: " + targetUserId);
            } else {
                client.sendError("Không tìm thấy người dùng có ID này.");
            }
        } catch (SecurityException se) {
            client.sendError("Cảnh báo: Bạn không có quyền quản trị viên!");
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống khi thực hiện lệnh mở khóa: " + e.getMessage());
        }
    }

    public void handleForceClose(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Admin chưa đăng nhập!");
            return;
        }
        try {
            String auctionId = request.get("auctionId").getAsString();
            adminService.forceCloseAuction(client.getLoggedInUser(), auctionId);
            client.sendMessage("{\"action\": \"FORCE_CLOSE_REPLY\", \"status\": \"SUCCESS\"}");
        } catch (SecurityException se) {
            client.sendError("Bạn không có quyền cưỡng chế phiên đấu giá này!");
        } catch (Exception e) {
            client.sendError("Lỗi khi đóng phiên: " + e.getMessage());
        }
    }

    public void handleGetAuctionBidHistory(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Admin chưa đăng nhập!");
            return;
        }
        try {
            String auctionId = request.get("auctionId").getAsString();
            List<BiddingTransactionEntity> history =
                    adminService.getAuctionBidHistory(client.getLoggedInUser(), auctionId);

            JsonArray data = new JsonArray();
            for (BiddingTransactionEntity tx : history) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", tx.getId());
                obj.addProperty("auctionId", tx.getAuction() != null ? tx.getAuction().getId() : auctionId);
                obj.addProperty("amount", tx.getBidAmount());
                obj.addProperty("timestamp", tx.getBidTime() != null ? tx.getBidTime().toString() : "");
                obj.addProperty("isAuto", tx.isAutoBid());
                obj.addProperty("status", tx.getStatus() != null ? tx.getStatus().name() : "UNKNOWN");

                User bidder = tx.getBidder();
                obj.addProperty("bidderId", bidder != null ? bidder.getId() : "");
                obj.addProperty("bidderUsername", bidder != null ? bidder.getUsername() : "N/A");
                obj.addProperty("bidderFullName", bidder != null ? bidder.getFullName() : "");
                obj.addProperty("bidderEmail", bidder != null ? bidder.getEmail() : "");
                data.add(obj);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "ADMIN_AUCTION_BID_HISTORY_REPLY");
            reply.addProperty("auctionId", auctionId);
            reply.add("data", data);
            client.sendMessage(reply.toString());
        } catch (SecurityException se) {
            client.sendError("Bạn không có quyền xem lịch sử đấu giá.");
        } catch (Exception e) {
            client.sendError("Lỗi khi tải lịch sử đấu giá: " + e.getMessage());
        }
    }
}
