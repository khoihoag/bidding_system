package com.bidding;

import com.bidding.controller.AppNavigator;
import com.bidding.network.NetworkClient;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Vẫn giữ nguyên lệnh xóa viền ở đầu
        primaryStage.initStyle(javafx.stage.StageStyle.UNDECORATED);



        // Trao quyền điều khiển cửa sổ chính (Stage) cho anh lái tàu AppNavigator
        AppNavigator.setMainStage(primaryStage);
        primaryStage.setTitle("BidVault - Hệ thống Đấu Giá");

        // KHỞI ĐỘNG CỘT SÓNG WIFI: Mở kết nối tới Server ngay khi bật App!
        NetworkClient.getInstance().connect();

        // Xong xuôi thì phi thẳng vào màn hình Đăng Nhập (Lúc này AppNavigator sẽ nạp Scene)
        AppNavigator.navigate("Login.fxml");

        // Sự kiện: Khi người dùng bấm dấu X đỏ để tắt App
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("🔴 Đang đóng ứng dụng, dọn dẹp kết nối...");
            NetworkClient.getInstance().disconnect();
        });

        // 2. Hiện cửa sổ ra trước...
        // ... (phía trên giữ nguyên)

        // 2. Hiện cửa sổ ra trước...
        primaryStage.show();

        // 3. VŨ KHÍ TỐI THƯỢNG: ĐO MÀN HÌNH VÀ ÉP SIZE THỦ CÔNG
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }
}