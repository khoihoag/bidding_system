package com.bidding.client.scene;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.bidding.client.network.NetworkClient;

public class HelloApplication1 extends Application {
    @Override
    public void start(Stage stage) {
        try {
            // Thử kết nối Server trước
            System.out.println("[INFO] Đang kết nối tới Server...");
            NetworkClient.connect("127.0.0.1", 8080);

            // Nạp FXML chính
            System.out.println("[INFO] Đang tải giao diện đăng nhập...");
            FXMLLoader loader = new FXMLLoader(HelloApplication1.class.getResource("/com/bidding/client/scene/Scene1.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, AppNavigator.LOGIN_WIDTH, AppNavigator.LOGIN_HEIGHT);

            stage.setTitle("BidViet - Đăng nhập hệ thống");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
            System.out.println("[INFO] Ứng dụng khởi động thành công.");
            
        } catch (Exception e) {
            System.err.println("[FATAL] Ứng dụng gặp lỗi nghiêm trọng khi khởi động:");
            e.printStackTrace();
            // Thoát ứng dụng nếu không nạp được giao diện chính
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
