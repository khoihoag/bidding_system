package com.bidding.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppNavigator {
    private static Stage mainStage;
    private static Object currentData;
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }
    // Nạp hành lý
    public static void passData(Object data) {
        currentData = data;
    }
    // Lấy hành lý ra xài
    public static Object getData() {
        return currentData;
    }
    public static void navigate(String fxml) {
        Platform.runLater(() -> {
            try {
                Parent root = FXMLLoader.load(AppNavigator.class.getResource("/fxml/" + fxml));

                // ================= VŨ KHÍ CHỐNG TỤT MÀN HÌNH =================
                if (mainStage.getScene() == null) {
                    // Lần đầu tiên khởi động App (Lúc login xong): Tạo cái Vỏ mới
                    Scene scene = new Scene(root, 1200, 750);

                    // Gắn CSS
                    java.net.URL cssUrl = AppNavigator.class.getResource("/css/style.css");
                    if (cssUrl != null) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                    } else {
                        System.err.println("⚠️ CẢNH BÁO: Không tìm thấy file style.css để trang điểm!");
                    }

                    mainStage.setScene(scene);
                    mainStage.centerOnScreen();
                } else {
                    // Các lần bấm chuyển trang (Vào xem, Quay lại):
                    // CHỈ THAY CÁI RUỘT (Root), GIỮ NGUYÊN VỎ VÀ KÍCH THƯỚC HIỆN TẠI!
                    mainStage.getScene().setRoot(root);
                }
                // ==============================================================

            } catch (Exception e) {
                System.err.println("Lỗi chuyển màn hình: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }}