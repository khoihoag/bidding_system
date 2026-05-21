package com.bidding.server.controller;

import com.bidding.server.enums.UserRole;
import com.bidding.server.model.user.Admin;
import com.bidding.server.model.user.User;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.service.UserService;
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
            client.setLoggedInUser(loggedInUser);
            int adminLevel = getAdminLevel(loggedInUser);

            client.sendMessage(String.format(
                    "{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\", \"myId\": \"%s\", \"role\": \"%s\", \"adminLevel\": %d, \"balance\": %s}",
                    loggedInUser.getId(),
                    loggedInUser.getRole().name(),
                    adminLevel,
                    loggedInUser.getBalance()
            ));
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
            client.sendError("Loi khi tao tai khoan: " + e.getMessage());
        }
    }

    public void handleDeposit(JsonObject request) {
        User user = client.getLoggedInUser();

        if (user == null) {
            client.sendError("Vui long dang nhap de nap tien!");
            return;
        }

        try {
            double amount = request.get("amount").getAsDouble();
            if (amount <= 0) {
                client.sendError("So tien nap phai lon hon 0!");
                return;
            }

            baoVe.addBalance(user, amount);
            double freshBal = baoVe.getFreshBalance(user.getId());
            user.setBalance(freshBal);

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "DEPOSIT_REPLY");
            reply.addProperty("status", "SUCCESS");
            reply.addProperty("balance", freshBal);

            client.sendMessage(reply.toString());

            System.out.println("[Bank] " + user.getUsername() + " deposited: " + amount);
        } catch (Exception e) {
            client.sendError("Loi nap tien: " + e.getMessage());
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
