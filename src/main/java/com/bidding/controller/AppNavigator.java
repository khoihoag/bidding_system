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
                Scene scene = new Scene(root, 1200, 750);

                // 1. TÌM FILE CSS (Sếp chú ý đường dẫn nhé)
                // Nếu sếp để file ở src/main/resources/css/style.css thì dùng "/css/style.css"
                // Nếu để ở src/main/resources/style.css thì dùng "/style.css"
                java.net.URL cssUrl = AppNavigator.class.getResource("/css/style.css");

                // 2. KHIÊN CHẮN BẢO VỆ
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("⚠️ CẢNH BÁO: Không tìm thấy file style.css để trang điểm!");
                }

                mainStage.setScene(scene);
                mainStage.centerOnScreen();
            } catch (Exception e) {
                System.err.println("Lỗi chuyển màn hình: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }}