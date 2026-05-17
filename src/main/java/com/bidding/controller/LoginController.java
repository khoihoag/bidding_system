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
            showError("Vui long nhap day du ten dang nhap va mat khau.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

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
                            : "Dang nhap that bai.");
                }
                default -> {}
            }
        });
    }

    private void handleLoginReply(JsonObject response) {
        String status = response.has("status") ? response.get("status").getAsString() : "";
        if (!"SUCCESS".equals(status)) {
            loginButton.setDisable(false);
            showError("Dang nhap that bai.");
            return;
        }

        UserSession session = UserSession.getInstance();
        session.setUserId(response.has("myId") ? response.get("myId").getAsString() : "");
        session.setRole(response.has("role") ? response.get("role").getAsString() : "USER");
        session.setUsername(usernameField.getText().trim());
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
