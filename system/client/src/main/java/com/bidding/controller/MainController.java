package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.application.Platform;
public class MainController {
    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    public MainController() {
        instance = this;
    }
    @FXML private Label lblUsername;
    @FXML private Button btnDashboard;
    @FXML private Button btnInventory;
    @FXML private Button btnHistory;
    @FXML
    private Label balanceLabel;
    @FXML
    private ListView<NotificationEntry> notificationListView;
    // Cái khung trống bên phải để nhúng các màn hình con vào
    @FXML private StackPane contentArea;
    private static final int MAX_NOTIFICATIONS = 20;
    private static final DateTimeFormatter NOTIFICATION_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final Consumer<JsonObject> notificationListener = this::handleGlobalNotification;

    @FXML
    public void initialize() {
        // Lấy tên User đang đăng nhập hiển thị lên Sidebar
        String username = UserSession.getInstance().getUsername();
        lblUsername.setText(username != null ? username : "Khách");

        // Mặc định lúc vừa vào thì nhúng màn hình Kho đồ lên trước
        handleDashboard();
        updateBalanceDisplay();
        setupNotificationList();
        NetworkClient.getInstance().addGlobalMessageListener(notificationListener);
    }

    @FXML
    private void handleDashboard() {
        setActiveMenu(btnDashboard);
        // Nhúng Sảnh Chính (Dashboard.fxml) vào khoảng trống bên phải
        loadSubView("/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleInventory() {
        setActiveMenu(btnInventory);
        // Nhúng nguyên cái màn Kho Đồ (chia 2 nửa) vào khung bên phải
        loadSubView("/fxml/Inventory.fxml");
    }

    @FXML
    private void handleHistory() {
        setActiveMenu(btnHistory);
        // Nhúng màn hình Lịch sử đấu giá vào
        loadSubView("/fxml/BidHistory.fxml");
    }

    @FXML
    private void handleLogout() {
        // Xóa thông tin user và ngắt mạng hiện tại
        NetworkClient.getInstance().removeGlobalMessageListener(notificationListener);
        NetworkClient.getInstance().setMessageHandler(null);
        UserSession.getInstance().clear();

        // Dùng con tàu AppNavigator chở về màn Login
        AppNavigator.navigate("Login.fxml");
    }

    @FXML
    private void handleClearNotifications() {
        if (notificationListView != null) {
            notificationListView.getItems().clear();
        }
    }

    @FXML
    private void handleViewNotificationDetail() {
        if (notificationListView == null) return;

        if (notificationListView.getItems().isEmpty()) {
            showNotificationDetailDialog("Chi tiết thông báo", "Chưa có thông báo nào.");
            return;
        }

        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < notificationListView.getItems().size(); i++) {
            NotificationEntry entry = notificationListView.getItems().get(i);
            detail.append(i + 1)
                    .append(". [")
                    .append(entry.time())
                    .append("] ")
                    .append(entry.message());

            if (i < notificationListView.getItems().size() - 1) {
                detail.append("\n\n");
            }
        }

        showNotificationDetailDialog("Chi tiết thông báo", detail.toString());
    }

    private void handleGlobalNotification(JsonObject json) {
        if (json == null || !json.has("action")) return;

        String action = json.get("action").getAsString();
        if ("ACCOUNT_BANNED".equals(action)) {
            handleAccountBanned();
            return;
        }
        if (!"GLOBAL_NOTIFY".equals(action)) return;

        String message = json.has("message") && !json.get("message").isJsonNull()
                ? json.get("message").getAsString()
                : "Có thông báo mới từ hệ thống.";

        addNotification(message, json);
    }

    private void handleAccountBanned() {
        Platform.runLater(() -> {
            NetworkClient.getInstance().removeGlobalMessageListener(notificationListener);
            NetworkClient.getInstance().setMessageHandler(null);
            UserSession.getInstance().clear();
            AppNavigator.navigate("Login.fxml");
        });
    }

    public void addNotification(String message) {
        addNotification(message, null);
    }

    public void addNotification(String message, JsonObject rawJson) {
        if (message == null || message.isBlank()) return;

        Platform.runLater(() -> {
            if (notificationListView == null) return;

            String time = LocalTime.now().format(NOTIFICATION_TIME_FMT);
            notificationListView.getItems().add(0, new NotificationEntry(time, message));

            while (notificationListView.getItems().size() > MAX_NOTIFICATIONS) {
                notificationListView.getItems().remove(notificationListView.getItems().size() - 1);
            }
        });
    }

    private void setupNotificationList() {
        if (notificationListView == null) return;
        notificationListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(NotificationEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
    }

    private void showNotificationDetailDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);

        TextArea detailArea = new TextArea(content);
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefSize(520, 320);
        detailArea.getStyleClass().add("input-field");

        alert.getDialogPane().setContent(detailArea);
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            alert.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception e) {
            System.err.println("Lỗi load CSS cho dialog chi tiết thông báo!");
        }
        alert.showAndWait();
    }

    private record NotificationEntry(String time, String message) {
        @Override
        public String toString() {
            return "[" + time + "] " + message;
        }
    }

    // --- HÀM HỖ TRỢ: Nhúng giao diện FXML con vào khung contentArea ---
    private void loadSubView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("Lỗi khi load màn hình con: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // --- HÀM HỖ TRỢ: Bôi xanh cái nút đang được chọn trên Sidebar ---
    private void setActiveMenu(Button activeBtn) {
        btnDashboard.getStyleClass().remove("menu-btn-active");
        btnInventory.getStyleClass().remove("menu-btn-active");
        btnHistory.getStyleClass().remove("menu-btn-active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("menu-btn-active");
        }
    }
    @FXML
    private void handleOpenDeposit() {
        // Sếp dùng cái TextInputDialog như anh em mình bàn lúc nãy
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("500000");
        dialog.setTitle("Ví BidVault");
        dialog.setHeaderText("Nạp thêm Polime vào ví");
        dialog.setContentText("Số tiền (VNĐ):");
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");

            // Xóa cái icon dấu chấm hỏi màu xanh dương mặc định cho nó ngầu
            dialog.setGraphic(null);
        } catch (Exception e) {
            System.err.println("Lỗi load CSS cho Dialog nạp tiền!");
        }
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr.replaceAll("[,.]", ""));
                // Bắn lệnh DEPOSIT lên Server thông qua NetworkClient
                com.google.gson.JsonObject request = new com.google.gson.JsonObject();
                request.addProperty("action", "DEPOSIT");
                request.addProperty("amount", amount);
                com.bidding.network.NetworkClient.getInstance().sendJson(request);
            } catch (Exception e) {
                System.err.println("Nhập số thôi sếp ơi!");
            }
        });
    }
    public void updateBalanceDisplay() {
        if (balanceLabel != null) {
            // Lấy số dư từ Session (Nhớ là sếp đã thêm trường balance vào UserSession ở Bước 1 nhé)
            double currentBalance = UserSession.getInstance().getBalance();

            // Định dạng tiền tệ Việt Nam (vi, VN)
            NumberFormat currencyFmt = NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));

            // Cập nhật text cho Label
            Platform.runLater(() -> {
                balanceLabel.setText("Số dư: " + currencyFmt.format(currentBalance) + " ₫");
            });
        }
    }
}
