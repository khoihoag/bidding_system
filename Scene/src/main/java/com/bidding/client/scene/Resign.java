package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Resign {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtGmail;
    @FXML private TextField txtPasswordShow;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label errorLabel;
    @FXML private CheckBox checkShowPass;

    // Khoi tao listener cho register reply.
    @FXML
    public void initialize() {
        NetworkClient.registerListener = this::handleRegisterReply;
    }

    // Doi qua hien/an mat khau.
    @FXML
    void togglePass(ActionEvent event) {
        if (checkShowPass.isSelected()) {
            txtPasswordShow.setText(txtPassword.getText());
            txtPasswordShow.setVisible(true);
            txtPassword.setVisible(false);
        } else {
            txtPassword.setText(txtPasswordShow.getText());
            txtPassword.setVisible(true);
            txtPasswordShow.setVisible(false);
        }
    }

    // Kiem tra form va gui REGISTER len server.
    @FXML
    void handleRegister(ActionEvent event) {
        showError("");

        String user = safeText(txtUsername);
        String fullName = safeText(txtFullName);
        String gmail = safeText(txtGmail);
        String pass = checkShowPass.isSelected() ? safeText(txtPasswordShow) : safeText(txtPassword);
        String confirmPass = safeText(txtConfirmPassword);

        if (user.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập.");
            return;
        }
        if (!user.matches("^[a-zA-Z][a-zA-Z0-9]{5,14}$")) {
            showError("Tên đăng nhập phải bắt đầu bằng chữ, từ 6 đến 15 ký tự và không có ký tự đặc biệt.");
            return;
        }
        if (fullName.isEmpty()) {
            showError("Vui lòng nhập họ và tên đầy đủ.");
            return;
        }
        if (gmail.isEmpty()) {
            showError("Vui lòng nhập địa chỉ Gmail.");
            return;
        }
        if (!gmail.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            showError("Email không hợp lệ. Vui lòng dùng @gmail.com.");
            return;
        }
        if (pass.isEmpty()) {
            showError("Vui lòng nhập mật khẩu.");
            return;
        }
        if (!pass.matches("^[a-zA-Z0-9]{6,15}$")) {
            showError("Mật khẩu gồm chữ và số, từ 6-15 ký tự.");
            return;
        }
        if (!pass.equals(confirmPass)) {
            showError("Mật khẩu nhập lại không khớp.");
            return;
        }

        String json = String.format(
                "{\"action\":\"REGISTER\",\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\"}",
                escapeJson(user),
                escapeJson(pass),
                escapeJson(gmail),
                escapeJson(fullName)
        );
        NetworkClient.send(json);
        showSuccess("Đang gửi yêu cầu đăng ký...");
    }

    // Quay lai scene login.
    @FXML
    void handleBackToLogin(ActionEvent event) {
        openLoginScene();
    }

    // Xu ly REGISTER_REPLY tra ve tu server.
    private void handleRegisterReply(JsonObject response) {
        String action = getString(response, "action");
        if (!"REGISTER_REPLY".equals(action) && !"ERROR".equals(action)) {
            return;
        }

        if ("ERROR".equals(action)) {
            showError(getString(response, "message", "Đăng ký thất bại."));
            return;
        }

        String status = getString(response, "status");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            showSuccess("Đăng ký thành công. Đang quay lại màn đăng nhập...");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> openLoginScene());
            pause.play();
        } else {
            showError(getString(response, "message", "Đăng ký thất bại."));
        }
    }

    // Mo lai man dang nhap.
    private void openLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidding/client/scene/Scene1.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) txtUsername.getScene().getWindow();
            currentStage.setTitle("Đăng nhập");
            currentStage.setScene(new Scene(root, 1200, 760));
            currentStage.setResizable(true);
            currentStage.centerOnScreen();
            currentStage.show();
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Không mở được `Scene1.fxml`. Lỗi: " + exception.getClass().getSimpleName());
        }
    }

    // Doc text an toan tu input.
    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // Escape ky tu de chen vao JSON.
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Lay string tu JsonObject.
    private String getString(JsonObject response, String key) {
        return getString(response, key, "");
    }

    // Lay string tu JsonObject voi fallback.
    private String getString(JsonObject response, String key, String fallback) {
        if (response == null || !response.has(key) || response.get(key).isJsonNull()) {
            return fallback;
        }
        return response.get(key).getAsString();
    }

    // Hien thi trang thai loi.
    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("status-success", "status-error");
        errorLabel.getStyleClass().add("status-error");
        errorLabel.setText(message);
    }

    // Hien thi trang thai thanh cong.
    private void showSuccess(String message) {
        errorLabel.getStyleClass().removeAll("status-success", "status-error");
        errorLabel.getStyleClass().add("status-success");
        errorLabel.setText(message);
    }
}
