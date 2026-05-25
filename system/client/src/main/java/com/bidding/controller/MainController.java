package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    @FXML private Button btnNotifications;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;
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
    private void handleLogoutConfirm() {
        setActiveMenu(btnLogout);

        ButtonType logoutType = new ButtonType("Đăng xuất", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", logoutType, cancelType);
        confirm.setTitle("Xác nhận đăng xuất");
        confirm.setHeaderText("Bạn có chắc chắn muốn đăng xuất?");
        confirm.setContentText("Phiên làm việc hiện tại sẽ kết thúc.");
        confirm.setGraphic(null);
        try {
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        confirm.showAndWait().ifPresent(type -> {
            if (type == logoutType) {
                handleLogout();
            }
        });
    }

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
        setActiveMenu(btnNotifications);
        try {
            // Tải cái giao diện hòm thư Sotheby's mà anh em mình vừa tạo
            // (Sếp kiểm tra xem file Notifications.fxml của sếp nằm ở đâu thì sửa lại đường dẫn cho đúng nhé, ví dụ: "/fxml/Notifications.fxml")
            javafx.scene.Parent view = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/Notifications.fxml"));
            // Dọn dẹp sạch sẽ cái ruột ở giữa màn hình
            contentArea.getChildren().clear();

            // Nhét nguyên trang Thông Báo hoành tráng vào
            contentArea.getChildren().add(view);

        } catch (Exception e) {
            System.err.println("Toang! Lỗi chuyển trang Thông Báo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettings() {
        setActiveMenu(btnSettings);
        VBox settingsView = buildSettingsView();
        settingsView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane(settingsView);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-surface");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(scrollPane);
        StackPane.setAlignment(scrollPane, javafx.geometry.Pos.TOP_LEFT);
    }

    private void handleGlobalNotification(JsonObject json) {
        if (json == null || !json.has("action")) return;

        String action = json.get("action").getAsString();
        if (!"GLOBAL_NOTIFY".equals(action)) return;

        String message = json.has("message") && !json.get("message").isJsonNull()
                ? json.get("message").getAsString()
                : "Có thông báo mới từ hệ thống.";

        addNotification(message, json);
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
        notificationListView.setPlaceholder(new Label("Chưa có thông báo"));
        notificationListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(NotificationEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label icon = new Label("✉");
                icon.getStyleClass().add("sidebar-notification-icon");
                StackPane iconWrap = new StackPane(icon);
                iconWrap.getStyleClass().add("sidebar-notification-icon-wrap");

                Label message = new Label(shortMessage(item.message()));
                message.getStyleClass().add("sidebar-notification-message");

                Label time = new Label(item.time() + " xem chi tiết");
                time.getStyleClass().add("sidebar-notification-time");

                javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(1, message, time);
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8, iconWrap, textBox);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
            }
        });
    }

    private String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Thông báo mới";
        }
        return message.length() > 18 ? message.substring(0, 18) + "..." : message;
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
        if (btnNotifications != null) {
            btnNotifications.getStyleClass().remove("menu-btn-active");
        }
        if (btnSettings != null) {
            btnSettings.getStyleClass().remove("menu-btn-active");
        }
        if (btnLogout != null) {
            btnLogout.getStyleClass().remove("menu-btn-active");
        }

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("menu-btn-active");
        }
    }

    private VBox buildSettingsView() {
        VBox page = new VBox(22);
        page.getStyleClass().add("settings-page");

        Label title = new Label("Cài đặt tài khoản");
        title.getStyleClass().add("settings-title");
        Label subtitle = new Label("Quản lý thông tin cá nhân và bảo mật tài khoản của bạn.");
        subtitle.getStyleClass().add("settings-subtitle");
        VBox heading = new VBox(6, title, subtitle);

        VBox card = new VBox(20);
        card.getStyleClass().add("settings-card");

        HBox personalTitle = settingsSectionTitle("/images/icons/user.png", "Thông tin cá nhân");

        TextField nameField = new TextField(UserSession.getInstance().getUsername() != null
                ? UserSession.getInstance().getUsername()
                : "");
        nameField.getStyleClass().add("settings-field");

        TextField emailChangeField = new TextField();
        emailChangeField.setPromptText("Nhập email mới");
        emailChangeField.getStyleClass().add("settings-field");

        TextField emailField = new TextField();
        emailField.setText("Email không thể thay đổi");
        emailField.setDisable(true);
        emailField.getStyleClass().add("settings-field");

        HBox infoRow = new HBox(14,
                fieldBox("Đổi email", emailChangeField),
                fieldBox("Email hiện tại", emailField)
        );

        HBox securityTitle = settingsSectionTitle("/images/icons/padlock.png", "Bảo mật & Mật khẩu");

        PasswordField currentPassword = new PasswordField();
        currentPassword.setPromptText("Nhập mật khẩu hiện tại");
        currentPassword.getStyleClass().add("settings-field");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Nhập mật khẩu mới");
        newPassword.getStyleClass().add("settings-field");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Nhập lại mật khẩu mới");
        confirmPassword.getStyleClass().add("settings-field");

        Button cancelBtn = new Button("Hủy");
        cancelBtn.getStyleClass().add("settings-cancel-button");
        cancelBtn.setOnAction(e -> handleDashboard());

        Button saveBtn = new Button("Lưu thay đổi");
        saveBtn.getStyleClass().add("settings-save-button");
        saveBtn.setOnAction(e -> {
            String username = nameField.getText().trim();
            if (!username.isEmpty()) {
                UserSession.getInstance().setUsername(username);
                lblUsername.setText(username);
            }
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(12, footerSpacer, cancelBtn, saveBtn);

        card.getChildren().addAll(
                personalTitle,
                fieldBox("Họ và tên", nameField),
                infoRow,
                new Separator(),
                securityTitle,
                fieldBox("Mật khẩu hiện tại", currentPassword),
                fieldBox("Mật khẩu mới", newPassword),
                fieldBox("Xác nhận mật khẩu mới", confirmPassword),
                new Separator(),
                actions
        );

        page.getChildren().addAll(heading, card);
        return page;
    }

    private VBox fieldBox(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-field-label");
        VBox box = new VBox(7, label, field);
        box.setMinWidth(260);
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private HBox settingsSectionTitle(String iconPath, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-section-title");
        HBox title = new HBox(9, iconView(iconPath, 16), label);
        title.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return title;
    }

    private ImageView iconView(String iconPath, double size) {
        ImageView icon = new ImageView();
        try {
            icon.setImage(new Image(getClass().getResourceAsStream(iconPath)));
        } catch (Exception ignored) {
        }
        icon.setFitWidth(size);
        icon.setFitHeight(size);
        icon.setPreserveRatio(true);
        return icon;
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
