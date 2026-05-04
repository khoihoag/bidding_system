package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.application.Platform;



public class Scene1 {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
 public static String myname = "";   

    @FXML
    void handleLogin(ActionEvent event) {

        String username = usernameField.getText().trim();

        String password = passwordField.getText();

        if (username.isEmpty()) {
            showError("Ban dang de trong Username.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Vui long nhap Password.");
            return;
        }

        if (!NetworkClient.isConnected()) {
            showError("Chua ket noi server. Hay chay server truoc khi dang nhap.");
            return;
        }

        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("action", "LOGIN");
            json.addProperty("username", username);
            json.addProperty("password", password);

            if (NetworkClient.send(json.toString())) {
                showSuccess("Dang dang nhap...");
            } else {
                showError("Khong gui duoc yeu cau dang nhap. Kiem tra server.");
            }

        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Khong gui duoc yeu cau dang nhap. Loi: " + exception.getClass().getSimpleName());
        }
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        openEntryScene("/com/bidding/client/scene/ForgotPassword.fxml", "Forgot Password");
    }

    @FXML
    void handleRegisterClick(ActionEvent event) {
        openEntryScene("/com/bidding/client/scene/Resign.fxml", "Dang Ky Tai Khoan");
    }

    private void openPrimaryScene(String resourcePath, String title) throws Exception {
        Stage currentStage = (Stage) usernameField.getScene().getWindow();
        AppNavigator.openPrimary(currentStage, resourcePath, title);
    }

    private void openEntryScene(String resourcePath, String title) {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            AppNavigator.openEntry(currentStage, resourcePath, title);
        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Khong mo duoc giao dien. Loi: " + exception.getClass().getSimpleName());
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


@FXML
public void initialize() {
    NetworkClient.loginListener = response -> {
        
        System.out.println("Server phản hồi Login: " + response.toString());

        Platform.runLater(() -> {
            // if (response == null) {
            //     showError("Nhận phản hồi rỗng từ server.");
            //     return;
            // }

            String action = response.has("action") ? response.get("action").getAsString() : "";
            String status = response.has("status") ? response.get("status").getAsString() : "";

            if ("LOGIN_REPLY".equals(action)) {
                if ("SUCCESS".equals(status)) {
                    myname = usernameField.getText().trim();
                    
                    showSuccess("Đăng nhập thành công.");
                    
                    try {
                        openPrimaryScene("/com/bidding/client/scene/SceneBidder1.fxml",
                                        "BidViet - Nền tảng đấu giá trực tuyến");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Đăng nhập thành công nhưng không chuyển được màn hình.");
                    }
                } else {
                    // Trường hợp LOGIN_REPLY nhưng status không phải SUCCESS
                    showError("Đăng nhập thất bại.");
                }
            } 
            else if ("ERROR".equals(action)) {
                String serverMessage = response.has("message") 
                    ? response.get("message").getAsString() 
                    : "Lỗi không xác định từ Server.";
                showError(serverMessage);
            } 
            else {
                showError("Phản hồi không xác định từ hệ thống: " + action);
            }
        });
    };
}
}




// cd C:\Users\PC\IdeaProjects\bidding_system\Scene
// $env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
// $env:Path='C:\Program Files\Java\jdk-25.0.2\bin;' + $env:Path
// mvn -q javafx:run
