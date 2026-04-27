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
            showError("Vui long nhap email hoac so dien thoai truoc khi nhan ma.");
            return;
        }

        generatedOTP = "123456";
        System.out.println("Da gui OTP den: " + contact + ". OTP: " + generatedOTP);
        showSuccess("Da gui ma OTP thanh cong. Vui long kiem tra email/tin nhan.");
    }

    @FXML
    void handleResetPassword(ActionEvent event) {
        showError("");

        String contact = txtContact.getText();
        String otp = txtOTP.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (contact == null || contact.trim().isEmpty()) {
            showError("Vui long nhap email hoac so dien thoai.");
            return;
        }
        if (otp == null || otp.trim().isEmpty()) {
            showError("Vui long nhap ma OTP.");
            return;
        }
        if (!otp.equals(generatedOTP)) {
            showError("Ma OTP khong chinh xac.");
            return;
        }
        if (newPass == null || !newPass.matches("^[a-zA-Z0-9]{6,15}$")) {
            showError("Mat khau moi tu 6-15 ky tu, chi gom chu va so.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showError("Mat khau xac nhan khong khop.");
            return;
        }

        showSuccess("Doi mat khau thanh cong. Dang quay lai man dang nhap...");
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
            currentStage.setTitle("Dang nhap");
            currentStage.setScene(new Scene(root, 1200, 760));
            currentStage.setResizable(true);
            currentStage.centerOnScreen();
            currentStage.show();
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Khong mo duoc Scene1.fxml. Loi: " + exception.getClass().getSimpleName());
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
