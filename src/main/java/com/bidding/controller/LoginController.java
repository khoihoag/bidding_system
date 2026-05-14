package com.bidding.controller;

import com.bidding.model.UserSession; // Đã thêm dòng này để hết báo đỏ!
import com.bidding.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final NetworkClient networkClient = NetworkClient.getInstance();

    @FXML
    public void initialize() {
        // Đăng ký handler để nhận JSON từ server
        networkClient.setMessageHandler(this::handleServerMessage);

        // Cho phép nhấn Enter trên PasswordField để login
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

        setLoading(true);

        JsonObject request = new JsonObject();
        request.addProperty("action", "LOGIN");
        request.addProperty("username", username);
        request.addProperty("password", password);

        networkClient.sendJson(request);
    }

    @FXML
    private void handleGoToRegister() {
        // Gọi thuyền trưởng Navigator đưa sang màn Đăng Ký
        AppNavigator.navigate("Register.fxml");
    }

    private void handleServerMessage(JsonObject json) {
        String action = json.has("action") ? json.get("action").getAsString() : "";

        switch (action) {
            case "LOGIN_REPLY" -> handleLoginReply(json);
            case "ERROR"       -> handleError(json);
            default            -> {} // Bỏ qua các action không liên quan
        }
    }

    private void handleLoginReply(JsonObject json) {
        String status = json.has("status") ? json.get("status").getAsString() : "";

        if ("SUCCESS".equals(status)) {
            String myId = json.has("myId") ? json.get("myId").getAsString() : "";
            String role  = json.has("role")  ? json.get("role").getAsString()  : "USER";

            // Lưu session
            UserSession.getInstance().setUserId(myId);
            UserSession.getInstance().setRole(role);
            UserSession.getInstance().setUsername(usernameField.getText().trim());

            // ================= THÊM ĐOẠN NÀY ĐỂ HỨNG TIỀN =================
            if (json.has("balance")) {
                UserSession.getInstance().setBalance(json.get("balance").getAsDouble());
            }
            // ==============================================================

            navigateToMain(role);
        } else {
            setLoading(false);
            showError("Đăng nhập thất bại. Vui lòng kiểm tra lại.");
        }
    }

    private void handleError(JsonObject json) {
        String message = json.has("message") ? json.get("message").getAsString() : "Lỗi không xác định";

        // BỌC THÉP BẰNG PLATFORM.RUNLATER ĐỂ TRẢ VỀ LUỒNG UI CHÍNH
        Platform.runLater(() -> {
            setLoading(false); // Trả lại nút bấm
            showError(message); // Hiện chữ đỏ
        });
    }

    private void navigateToMain(String role) {
        // Đã dọn dẹp FXMLLoader, dùng AppNavigator thần thánh
        if ("ADMIN".equals(role)) {
            showError("Tính năng Admin đang cập nhật"); // Nếu sau này có Admin thì sửa chỗ này
        } else {
            AppNavigator.navigate("Main.fxml");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng Nhập");
        if (!loading) errorLabel.setVisible(false);
    }
}