package com.bidding.server.controller;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.Admin;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.user.User;
import com.bidding.server.service.UserService;
import com.bidding.server.utils.ApiResponse;
import com.google.gson.JsonObject;

public class AuthController {
    private final ClientHandler client;
    private final UserService baoVe;

    public AuthController(ClientHandler client, UserService baoVe) {
        this.client = client;
        this.baoVe = baoVe;
    }

    public void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        try {
            User loggedInUser = baoVe.login(user, pass);
            client.setLoggedInUser(loggedInUser); // Set ngược lại vào Lễ tân

            client.sendMessage(ApiResponse.loginSuccess(
                    loggedInUser.getId(),
                    loggedInUser.getRole().name(),
                    getAdminLevel(loggedInUser),
                    loggedInUser.getBalance()
            ).toString());
        } catch (Exception e) {
            client.sendError(e.getMessage());
        }
    }

    public void handleRegister(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();
        String email = request.get("email").getAsString();
        String fullName = request.get("fullName").getAsString();

        try {
            baoVe.register(user, pass, email, fullName);
            client.sendMessage("{\"action\": \"REGISTER_REPLY\", \"status\": \"SUCCESS\"}");
        } catch (Exception e) {
            client.sendError("Lỗi khi tạo tài khoản: " + e.getMessage());
        }
    }
    // 1. Đổi sang PUBLIC để ClientHandler gọi được
    public void handleDeposit(JsonObject request) {
        // Dùng hàm GETTER công khai thay vì thò tay vào biến private
        User user = client.getLoggedInUser();

        if (user == null) {
            client.sendError("Vui lòng đăng nhập để nạp tiền!");
            return;
        }

        try {
            double amount = request.get("amount").getAsDouble();
            if (amount <= 0) {
                client.sendError("Số tiền nạp phải lớn hơn 0!");
                return;
            }

            // ================= CẬP NHẬT XUỐNG DATABASE =================
            // Gọi bảo vệ (UserService) để cộng tiền thẳng vào DB
            baoVe.addBalance(user, amount);

            // Lấy số tiền tươi rói từ dưới DB lên để đảm bảo đồng bộ 100%
            double freshBal = baoVe.getFreshBalance(user.getId());
            user.setBalance(freshBal); // Cập nhật lại cho RAM
            // ==========================================================

            // Báo tin vui về cho UI
            JsonObject reply = new JsonObject();
            reply.addProperty("action", "DEPOSIT_REPLY");
            reply.addProperty("status", "SUCCESS");

            // ĐÚNG CHỮ "balance" THÌ LƯỚI Ở UI MỚI BẮT ĐƯỢC NHÉ SẾP!
            reply.addProperty("balance", freshBal);

            client.sendMessage(reply.toString());

            System.out.println("[Bank] Đại gia " + user.getUsername() + " vừa nạp: " + amount);
        } catch (Exception e) {
            client.sendError("Lỗi nạp tiền: " + e.getMessage());
        }
    }

    private int getAdminLevel(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return 0;
        }
        if (user instanceof Admin admin) {
            return admin.getAdminLevel();
        }
        return 2;
    }
}
