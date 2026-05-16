package com.bidding.server.controller;

import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.user.User;
import com.bidding.server.service.AdminService;
import com.bidding.server.service.AuctionService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import com.bidding.server.utils.GsonUtil;
public class AdminController {
    private final ClientHandler client;
    private final AdminService adminService;
    private final AuctionService tongQuan;
    Gson gson = GsonUtil.getInstance();
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
                JsonObject obj = new JsonObject();
                obj.addProperty("id", user.getId());
                obj.addProperty("username", user.getUsername());
                obj.addProperty("email", user.getEmail() != null ? user.getEmail() : "");
                obj.addProperty("role", user.getRole() != null ? user.getRole().name() : "USER");
                obj.addProperty("balance", user.getBalance());
                obj.addProperty("active", user.isActive());
                data.add(obj);
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
}
