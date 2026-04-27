package com.bidding.server.controller;

import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.user.User;
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
            client.setLoggedInUser(loggedInUser); // Set ngược lại vào Lễ tân
            client.sendMessage(String.format(
                    "{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\", \"myId\": \"%s\", \"role\": \"%s\"}",
                    loggedInUser.getId(),
                    loggedInUser.getRole().name()
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
            client.sendError("Lỗi khi tạo tài khoản: " + e.getMessage());
        }
    }
}