package com.bidding.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.application.Platform;
import com.bidding.network.NetworkClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML
    private ListView<JsonObject> mainNotificationList; // Lưu danh sách dưới dạng JsonObject để dễ bóc tách

    private final NetworkClient networkClient = NetworkClient.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cài đặt handler nhận thông báo từ Server
        networkClient.setMessageHandler(this::handleServerMessage);

        // ĐỔI LỚP ÁO CHO TỪNG DÒNG THÔNG BÁO (CUSTOM CELL FACTORY CHUẨN SOTHEBY'S)
        mainNotificationList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JsonObject item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;"); // Xóa nền xám mặc định
                } else {
                    // Lấy dữ liệu từ Json (Sếp kiểm tra xem key server trả về là gì nhé, ví dụ: message, time/timestamp)
                    String message = item.has("message") ? item.get("message").getAsString() : "Thông báo không có nội dung.";
                    String time = item.has("time") ? item.get("time").getAsString() : "--:--";

                    // Tạo layout phẳng tối giản cho từng dòng
                    VBox cellBox = new VBox(8);
                    // Tạo một đường kẻ mỏng màu xám nhạt ngăn cách dưới mỗi thông báo
                    cellBox.setStyle("-fx-padding: 15 10; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0; -fx-background-color: #FFFFFF;");

                    // Dòng 1: Thời gian (Chữ xám nhỏ nhắn)
                    Label lblTime = new Label("[" + time + "]");
                    lblTime.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");

                    // Dòng 2: Nội dung thông báo (Chữ đen đậm nét sang chảnh)
                    Label lblMessage = new Label(message);
                    lblMessage.setWrapText(true);
                    lblMessage.setStyle("-fx-text-fill: #111111; -fx-font-size: 14px; -fx-line-spacing: 3px;");

                    cellBox.getChildren().addAll(lblTime, lblMessage);

                    setText(null);
                    setGraphic(cellBox);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Tự động gọi server lấy danh sách thông báo mới khi vừa vào trang
        requestNotifications();
    }

    private void requestNotifications() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_NOTIFICATIONS"); // Sếp kiểm tra action này bên Server nhé
        networkClient.sendJson(request);
    }

    private void handleServerMessage(JsonObject response) {
        String action = response.has("action") ? response.get("action").getAsString() : "";

        // 1. Khi vừa vào trang, server trả về 1 cục danh sách cũ
        if ("NOTIFICATIONS_LIST".equals(action)) {
            Platform.runLater(() -> {
                mainNotificationList.getItems().clear();
                if (response.has("data")) {
                    JsonArray array = response.getAsJsonArray("data");
                    for (JsonElement elem : array) {
                        mainNotificationList.getItems().add(elem.getAsJsonObject());
                    }
                }
            });
        }
        // 2. BẮT SÓNG REALTIME: Khi có 1 thông báo mới tinh vừa rơi xuống (Ví dụ: "Giá áo Jalen Brunson vừa tăng lên 50.000đ!")
        else if ("NEW_NOTIFICATION".equals(action)) {
            Platform.runLater(() -> {
                if (response.has("data")) {
                    JsonObject newNotif = response.getAsJsonObject("data");
                    // Nhét thông báo mới lên trên CÙNG của danh sách
                    mainNotificationList.getItems().add(0, newNotif);
                }
            });
        }
    }
}