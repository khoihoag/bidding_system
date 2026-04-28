package com.bidding.client.scene;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ForgotPassword {

    @FXML private TextField txtContact;
    @FXML private TextField txtOTP;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label errorLabel;

    private String generatedOTP = "";

    @FXML
    void handleSendOTP(ActionEvent event) {
        showError("");
        String contact = txtContact.getText();

        if (contact == null || contact.trim().isEmpty()) {
            showError("Vui lòng nhập email hoặc số điện thoại trước khi nhận mã.");
            return;
        }

        generatedOTP = "123456";
        System.out.println("Đã gửi OTP đến: " + contact + ". OTP: " + generatedOTP);
        showSuccess("Đã gửi mã OTP thành công. Vui lòng kiểm tra email hoặc tin nhắn.");
    }

    @FXML
    void handleResetPassword(ActionEvent event) {
        showError("");

        String contact = txtContact.getText();
        String otp = txtOTP.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (contact == null || contact.trim().isEmpty()) {
            showError("Vui lòng nhập email hoặc số điện thoại.");
            return;
        }
        if (otp == null || otp.trim().isEmpty()) {
            showError("Vui lòng nhập mã OTP.");
            return;
        }
        if (!otp.equals(generatedOTP)) {
            showError("Mã OTP không chính xác.");
            return;
        }
        if (newPass == null || !newPass.matches("^[a-zA-Z0-9]{6,15}$")) {
            showError("Mật khẩu mới từ 6-15 ký tự, chỉ gồm chữ và số.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        showSuccess("Đổi mật khẩu thành công. Đang quay lại màn đăng nhập...");
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> openLoginScene());
        pause.play();
    }

    @FXML
    void handleBackToLogin(ActionEvent event) {
        openLoginScene();
    }

    private void openLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidding/client/scene/Scene1.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) txtContact.getScene().getWindow();
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

    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("status-success", "status-error");
        errorLabel.getStyleClass().add("status-error");
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        errorLabel.getStyleClass().removeAll("status-success", "status-error");
        errorLabel.getStyleClass().add("status-success");
        errorLabel.setText(message);
    }
}
