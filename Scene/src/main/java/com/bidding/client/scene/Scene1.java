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
        showError("");

        String username = usernameField.getText();

        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            showError("Bạn đang để trống tên đăng nhập.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Vui lòng nhập mật khẩu.");
            return;
        }
        if (username.length() < 6 || username.length() > 15) {
            showError("Tên đăng nhập từ 6 đến 15 ký tự.");
            return;
        }
        if (!username.matches("^[a-zA-Z][a-zA-Z0-9]+$")) {
            showError("Tên đăng nhập phải bắt đầu bằng chữ và không có ký tự đặc biệt.");
            return;
        }
        if (password.length() < 6 || password.length() > 15) {
            showError("Mật khẩu từ 6 đến 15 ký tự.");
            return;
        }
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            showError("Mật khẩu chỉ gồm chữ và số.");
            return;
        }

        try {
            String json = String.format("{\"action\":\"LOGIN\", \"username\":\"%s\", \"password\":\"%s\"}",username, password);
            NetworkClient.send(json);
            showSuccess("Đang đăng nhập...");

        } catch (Exception exception) {
            exception.printStackTrace();
            showError("Không mở được màn hình sau đăng nhập. Lỗi: " + exception.getClass().getSimpleName());
            return;
        }
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        openEntryScene("/com/bidding/client/scene/ForgotPassword.fxml", "Quên mật khẩu");
    }

    @FXML
    void handleRegisterClick(ActionEvent event) {
        openEntryScene("/com/bidding/client/scene/Resign.fxml", "Đăng ký tài khoản");
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
            showError("Không mở được giao diện. Lỗi: " + exception.getClass().getSimpleName());
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
            String status = response.get("status").getAsString();

            Platform.runLater(() -> {
                if (status.equals("SUCCESS")) {
                    myname = usernameField.getText(); // Chỉ gán khi đăng nhập thành công
                    showSuccess("Đăng nhập thành công.");

                    try {
                        openPrimaryScene("/com/bidding/client/scene/SceneBidder1.fxml",
                                "BidViet - Nen tang dau gia truc tuyen");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    showError("Sai tên đăng nhập hoặc mật khẩu.");
                }
            });
        };
    }
    
    }





// cd C:\Users\PC\IdeaProjects\Scene
// $env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
// $env:Path='C:\Program Files\Java\jdk-25.0.2\bin;' + $env:Path
// mvn -q javafx:run
