package com.bidding.controller;

import com.bidding.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^\\S{6,32}$");

    @FXML
    public void initialize() {
        // Lắng nghe phản hồi từ server
        networkClient.setMessageHandler(this::handleServerMessage);

        // Nhấn Enter ở ô cuối cùng (xác nhận MK) thì chạy luôn đăng ký
        confirmPasswordField.setOnAction(e -> handleRegister());
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Check rỗng toàn bộ các trường
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ các thông tin bắt buộc (*).");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            showError("M\u1eadt kh\u1ea9u ph\u1ea3i d\u00e0i 6-32 k\u00fd t\u1ef1 v\u00e0 kh\u00f4ng ch\u1ee9a kho\u1ea3ng tr\u1eafng.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Email không đúng định dạng!");
            return;
        }

        setLoading(true);

        // Gói cục JSON chuẩn chỉnh theo đúng Auth.txt
        JsonObject request = new JsonObject();
        request.addProperty("action", "REGISTER");
        request.addProperty("username", username);
        request.addProperty("password", password);
        request.addProperty("email", email);
        request.addProperty("fullName", fullName);

        networkClient.sendJson(request);
    }

    private void handleServerMessage(JsonObject json) {
        String action = json.has("action") ? json.get("action").getAsString() : "";

        if ("REGISTER_REPLY".equals(action)) {
            String status = json.has("status") ? json.get("status").getAsString() : "ERROR";

            Platform.runLater(() -> {
                setLoading(false);
                if ("SUCCESS".equals(status)) {
                    // Báo thành công và đá về màn đăng nhập
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Tạo tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
                    alert.showAndWait();

                    handleBackToLogin();
                } else {
                    String message = json.has("message") ? json.get("message").getAsString() : "Tên đăng nhập đã tồn tại hoặc email không hợp lệ.";
                    showError(message);
                }
            });
        }
    }

    @FXML
    private void handleBackToLogin() {
        AppNavigator.navigate("Login.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void setLoading(boolean loading) {
        registerButton.setDisable(loading);
        registerButton.setText(loading ? "Đang xử lý..." : "Đăng Ký");
        if (!loading) errorLabel.setVisible(false);
    }
}
