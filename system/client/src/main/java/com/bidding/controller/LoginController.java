package com.bidding.controller;

import com.bidding.network.NetworkClient;
import com.bidding.model.UserSession;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setMessageHandler(this::handleServerMessage);
        NetworkClient.getInstance().connect();
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);
        UserSession.getInstance().clear();

        JsonObject request = new JsonObject();
        request.addProperty("action", "LOGIN");
        request.addProperty("username", username);
        request.addProperty("password", password);
        NetworkClient.getInstance().sendJson(request);
    }

    @FXML
    private void handleGoToRegister() {
        AppNavigator.navigate("Register.fxml");
    }

    private void handleServerMessage(JsonObject response) {
        if (!response.has("action")) return;

        String action = response.get("action").getAsString();
        Platform.runLater(() -> {
            switch (action) {
                case "LOGIN_REPLY" -> handleLoginReply(response);
                case "ERROR" -> {
                    loginButton.setDisable(false);
                    showError(response.has("message")
                            ? response.get("message").getAsString()
                            : "Đăng nhập thất bại.");
                }
                default -> {}
            }
        });
    }

    private void handleLoginReply(JsonObject response) {
        String status = response.has("status") ? response.get("status").getAsString() : "";
        if (!"SUCCESS".equals(status)) {
            loginButton.setDisable(false);
            showError("Đăng nhập thất bại.");
            return;
        }

        UserSession session = UserSession.getInstance();
        session.setUserId(response.has("myId") ? response.get("myId").getAsString() : "");
        session.setRole(response.has("role") ? response.get("role").getAsString() : "USER");
        session.setAdminLevel(response.has("adminLevel") ? response.get("adminLevel").getAsInt() : 0);
        session.setUsername(response.has("username") ? response.get("username").getAsString() : usernameField.getText().trim());
        session.setFullName(response.has("fullName") ? response.get("fullName").getAsString() : session.getUsername());
        session.setEmail(response.has("email") ? response.get("email").getAsString() : "");
        session.setBalance(response.has("balance") ? response.get("balance").getAsDouble() : 0.0);

        if ("ADMIN".equals(session.getRole())) {
            AppNavigator.navigate("AdminView.fxml");
        } else {
            AppNavigator.navigate("Main.fxml");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
