package com.bidding.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    @FXML
    private Button markAllReadButton;

    private final NetworkClient networkClient = NetworkClient.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cài đặt handler nhận thông báo từ Server
        networkClient.setMessageHandler(this::handleServerMessage);

        if (markAllReadButton != null) {
            markAllReadButton.setOnAction(event -> markAllAsRead());
        }

        mainNotificationList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(JsonObject item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    setGraphic(buildNotificationCell(item));
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

    private HBox buildNotificationCell(JsonObject item) {
        boolean unread = !getBooleanSafe(item, "read", false);
        String message = getStringSafe(item, "message", "Thông báo không có nội dung.");
        String title = getStringSafe(item, "title", inferTitle(message));
        String time = getStringSafe(item, "time", getStringSafe(item, "timestamp", "--:--"));

        Region unreadBar = new Region();
        unreadBar.getStyleClass().add(unread ? "notification-unread-bar" : "notification-read-bar");

        Label icon = new Label(inferIcon(message));
        icon.getStyleClass().add("notification-item-icon");
        StackPane iconWrap = new StackPane(icon);
        iconWrap.getStyleClass().addAll("notification-icon-wrap", inferIconStyle(message));

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add(unread ? "notification-item-title" : "notification-item-title-read");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("notification-item-time");

        HBox titleRow = new HBox(14, titleLabel, timeLabel);
        titleRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("notification-item-message");

        VBox textBox = new VBox(7, titleRow, messageLabel);
        if (isBidAction(message)) {
            Button actionButton = new Button("Đặt giá ngay");
            actionButton.getStyleClass().add("notification-action-button");
            textBox.getChildren().add(actionButton);
        }
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox content = new HBox(16, iconWrap, textBox);
        content.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        content.getStyleClass().add(unread ? "notification-item-content-unread" : "notification-item-content-read");
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox row = new HBox(unreadBar, content);
        row.getStyleClass().add("notification-item-row");
        row.setOnMouseClicked(event -> {
            item.addProperty("read", true);
            mainNotificationList.refresh();
        });
        return row;
    }

    private void markAllAsRead() {
        for (JsonObject item : mainNotificationList.getItems()) {
            item.addProperty("read", true);
        }
        mainNotificationList.refresh();
    }

    private String getStringSafe(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : fallback;
    }

    private boolean getBooleanSafe(JsonObject object, String key, boolean fallback) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsBoolean()
                : fallback;
    }

    private String inferTitle(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("trả giá") || lower.contains("đặt giá") || lower.contains("bid")) {
            return "Cập nhật đấu giá";
        }
        if (lower.contains("theo dõi") || lower.contains("sắp bắt đầu")) {
            return "Phiên đấu giá yêu thích sắp bắt đầu";
        }
        if (lower.contains("nạp")) {
            return "Nạp tiền thành công";
        }
        if (lower.contains("kết thúc") || lower.contains("thắng") || lower.contains("không thắng")) {
            return "Kết thúc phiên đấu giá";
        }
        return "Thông báo mới";
    }

    private String inferIcon(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("nạp")) {
            return "💳";
        }
        if (lower.contains("theo dõi") || lower.contains("sắp bắt đầu")) {
            return "★";
        }
        if (lower.contains("kết thúc") || lower.contains("thắng")) {
            return "🏆";
        }
        return "⚖";
    }

    private String inferIconStyle(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("nạp")) {
            return "notification-icon-wallet";
        }
        if (lower.contains("theo dõi") || lower.contains("sắp bắt đầu")) {
            return "notification-icon-watch";
        }
        if (lower.contains("kết thúc") || lower.contains("thắng")) {
            return "notification-icon-result";
        }
        return "notification-icon-bid";
    }

    private boolean isBidAction(String message) {
        String lower = message.toLowerCase();
        return lower.contains("trả giá cao hơn") || lower.contains("đặt giá mới");
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
