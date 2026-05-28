package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
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
import com.bidding.util.WinnerCelebrationDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
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
    @FXML private Button btnWatchlist;
    @FXML private Button btnHistory;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;
    @FXML
    private Label balanceLabel;
    @FXML private Label sidebarClockTimeLabel;
    @FXML private Label sidebarClockDateLabel;
    @FXML
    private ListView<NotificationEntry> notificationListView;
    // Cái khung trống bên phải để nhúng các màn hình con vào
    @FXML private StackPane contentArea;
    private static final int MAX_NOTIFICATIONS = 20;
    private static final DateTimeFormatter NOTIFICATION_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SIDEBAR_CLOCK_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SIDEBAR_CLOCK_DAY =
            DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter SIDEBAR_CLOCK_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern SETTINGS_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SETTINGS_PASSWORD_PATTERN = Pattern.compile("^\\S{6,32}$");
    private Timeline sidebarClockTimeline;
    private final Consumer<JsonObject> notificationListener = this::handleGlobalNotification;
    private boolean settingsProfileUpdatePending;

    @FXML
    public void initialize() {
        // Lấy tên User đang đăng nhập hiển thị lên Sidebar
        String displayName = UserSession.getInstance().getFullName() != null
                ? UserSession.getInstance().getFullName()
                : UserSession.getInstance().getUsername();
        lblUsername.setText(displayName != null ? displayName : "Khách");

        // Mặc định lúc vừa vào thì nhúng màn hình Kho đồ lên trước
        handleDashboard();
        updateBalanceDisplay();
        setupNotificationList();
        startSidebarClock();
        NetworkClient.getInstance().addGlobalMessageListener(notificationListener);
        requestInitialNotifications();
    }

    private void startSidebarClock() {
        if (sidebarClockTimeline != null) {
            sidebarClockTimeline.stop();
        }
        tickSidebarClock();
        sidebarClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickSidebarClock()));
        sidebarClockTimeline.setCycleCount(Timeline.INDEFINITE);
        sidebarClockTimeline.play();
    }

    private void tickSidebarClock() {
        if (sidebarClockTimeLabel == null && sidebarClockDateLabel == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (sidebarClockTimeLabel != null) {
            sidebarClockTimeLabel.setText(now.format(SIDEBAR_CLOCK_TIME));
        }
        if (sidebarClockDateLabel != null) {
            String day = now.format(SIDEBAR_CLOCK_DAY);
            if (!day.isEmpty()) {
                day = Character.toUpperCase(day.charAt(0)) + day.substring(1);
            }
            sidebarClockDateLabel.setText(day + " · " + now.format(SIDEBAR_CLOCK_DATE));
        }
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
    private void handleWatchlist() {
        setActiveMenu(btnWatchlist);
        loadSubView("/fxml/Watchlist.fxml");
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
        setActiveMenu(null);
        loadSubView("/fxml/Notifications.fxml");
    }

    @FXML
    private void handleSettings() {
        setActiveMenu(btnSettings);
        NetworkClient.getInstance().setMessageHandler(null);
        VBox settingsView = buildSettingsView();
        settingsView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane(settingsView);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().addAll("content-surface", "settings-scroll-pane");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(scrollPane);
        StackPane.setAlignment(scrollPane, javafx.geometry.Pos.TOP_LEFT);
    }

    private void requestInitialNotifications() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_NOTIFICATIONS");
        NetworkClient.getInstance().sendJson(request);
    }

    private void handleGlobalNotification(JsonObject json) {
        if (json == null || !json.has("action")) {
            return;
        }

        if (json.has("balance") && !json.get("balance").isJsonNull()) {
            UserSession.getInstance().setBalance(json.get("balance").getAsDouble());
            updateBalanceDisplay();
        }

        String action = json.get("action").getAsString();
        if ("ACCOUNT_BANNED".equals(action)) {
            handleAccountBanned();
            return;
        }
        switch (action) {
            case "GLOBAL_NOTIFY" -> addNotification(extractNotificationMessage(json), json);
            case "NEW_NOTIFICATION" -> addNotification(extractNotificationMessage(json), json);
            case "NOTIFICATIONS_LIST" -> populateSidebarFromServer(json);
            case "AUCTION_FINISHED" -> WinnerCelebrationDialog.show(json, getClass());
            case "UPDATE_PROFILE_REPLY" -> {
                if (settingsProfileUpdatePending) {
                    settingsProfileUpdatePending = false;
                    handleUpdateProfileReply(json);
                }
            }
            case "ERROR" -> {
                if (settingsProfileUpdatePending) {
                    settingsProfileUpdatePending = false;
                    handleUpdateProfileReply(json);
                }
            }
            default -> { }
        }
    }

    private void populateSidebarFromServer(JsonObject response) {
        Platform.runLater(() -> {
            if (notificationListView == null) {
                return;
            }
            notificationListView.getItems().clear();
            if (!response.has("data") || !response.get("data").isJsonArray()) {
                return;
            }
            JsonArray array = response.getAsJsonArray("data");
            int count = 0;
            for (JsonElement element : array) {
                if (!element.isJsonObject() || count >= MAX_NOTIFICATIONS) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String message = extractNotificationMessage(item);
                String time = item.has("time") && !item.get("time").isJsonNull()
                        ? item.get("time").getAsString()
                        : LocalTime.now().format(NOTIFICATION_TIME_FMT);
                if (time.length() > 5) {
                    time = time.substring(0, 5);
                }
                notificationListView.getItems().add(new NotificationEntry(time, message));
                count++;
            }
        });
    }

    private String extractNotificationMessage(JsonObject json) {
        if (json == null) {
            return "Thông báo mới";
        }
        if (json.has("message") && !json.get("message").isJsonNull()) {
            return json.get("message").getAsString();
        }
        if (json.has("data") && json.get("data").isJsonObject()) {
            JsonObject data = json.getAsJsonObject("data");
            if (data.has("message") && !data.get("message").isJsonNull()) {
                return data.get("message").getAsString();
            }
        }
        return "Thông báo mới";
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
                setMinHeight(44);
                setPrefHeight(44);
            }
        });
        notificationListView.setFixedCellSize(44);
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
        if (btnWatchlist != null) {
            btnWatchlist.getStyleClass().remove("menu-btn-active");
        }
        btnHistory.getStyleClass().remove("menu-btn-active");
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
        VBox page = new VBox(28);
        page.getStyleClass().add("settings-page");

        Label kicker = new Label("TÀI KHOẢN");
        kicker.getStyleClass().add("settings-kicker");
        Label title = new Label("Cài đặt tài khoản");
        title.getStyleClass().add("settings-title");
        Label subtitle = new Label("Quản lý thông tin cá nhân và bảo mật tài khoản của bạn.");
        subtitle.getStyleClass().add("settings-subtitle");
        VBox heading = new VBox(8, kicker, title, subtitle);

        VBox card = new VBox(26);
        card.getStyleClass().add("settings-card");

        HBox personalTitle = settingsSectionTitle("/images/icons/user.png", "Thông tin cá nhân");

        TextField nameField = new TextField(UserSession.getInstance().getFullName() != null
                ? UserSession.getInstance().getFullName()
                : (UserSession.getInstance().getUsername() != null ? UserSession.getInstance().getUsername() : ""));
        nameField.getStyleClass().add("settings-field");

        TextField emailChangeField = new TextField();
        emailChangeField.setText(UserSession.getInstance().getEmail() != null
                ? UserSession.getInstance().getEmail()
                : "");
        emailChangeField.setPromptText("Nhập email");
        emailChangeField.getStyleClass().add("settings-field");

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
        saveBtn.setOnAction(e ->
                submitProfileUpdate(nameField, emailChangeField, currentPassword, newPassword, confirmPassword, saveBtn));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(12, footerSpacer, cancelBtn, saveBtn);

        Separator divider1 = new Separator();
        divider1.getStyleClass().add("settings-divider");
        Separator divider2 = new Separator();
        divider2.getStyleClass().add("settings-divider");

        card.getChildren().addAll(
                personalTitle,
                fieldBox("Họ và tên", nameField),
                fieldBox("Email", emailChangeField),
                divider1,
                securityTitle,
                fieldBox("Mật khẩu hiện tại", currentPassword),
                fieldBox("Mật khẩu mới", newPassword),
                fieldBox("Xác nhận mật khẩu mới", confirmPassword),
                divider2,
                actions
        );

        page.getChildren().addAll(heading, card);
        return page;
    }

    private void submitProfileUpdate(TextField nameField,
                                     TextField emailField,
                                     PasswordField currentPassword,
                                     PasswordField newPassword,
                                     PasswordField confirmPassword,
                                     Button saveBtn) {
        String fullName = nameField.getText() != null ? nameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String oldPassword = currentPassword.getText() != null ? currentPassword.getText() : "";
        String requestedPassword = newPassword.getText() != null ? newPassword.getText().trim() : "";
        String repeatedPassword = confirmPassword.getText() != null ? confirmPassword.getText().trim() : "";

        if (fullName.isEmpty()) {
            showSettingsAlert(Alert.AlertType.WARNING, "Thông tin chưa hợp lệ", "Họ và tên không được để trống.");
            return;
        }
        if (!SETTINGS_EMAIL_PATTERN.matcher(email).matches()) {
            showSettingsAlert(Alert.AlertType.WARNING, "Email chưa hợp lệ", "Email cần đúng định dạng, ví dụ name@example.com.");
            return;
        }

        boolean changingPassword = !requestedPassword.isEmpty() || !repeatedPassword.isEmpty() || !oldPassword.isEmpty();
        if (changingPassword) {
            if (oldPassword.isBlank()) {
                showSettingsAlert(Alert.AlertType.WARNING, "Thiếu mật khẩu cũ", "Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu.");
                return;
            }
            if (!SETTINGS_PASSWORD_PATTERN.matcher(requestedPassword).matches()) {
                showSettingsAlert(Alert.AlertType.WARNING, "Mật khẩu mới chưa hợp lệ", "Mật khẩu mới phải dài 6-32 ký tự và không chứa khoảng trắng.");
                return;
            }
            if (!requestedPassword.equals(repeatedPassword)) {
                showSettingsAlert(Alert.AlertType.WARNING, "Xác nhận mật khẩu không khớp", "Xác nhận mật khẩu cần khớp với mật khẩu mới.");
                return;
            }
        }

        saveBtn.setDisable(true);
        settingsProfileUpdatePending = true;
        JsonObject request = new JsonObject();
        request.addProperty("action", "UPDATE_PROFILE");
        request.addProperty("fullName", fullName);
        request.addProperty("email", email);
        request.addProperty("currentPassword", oldPassword);
        request.addProperty("newPassword", requestedPassword);
        NetworkClient.getInstance().sendJson(request);
    }

    private void handleUpdateProfileReply(JsonObject response) {
        Platform.runLater(() -> {
            String status = response.has("status") ? response.get("status").getAsString() : "ERROR";
            settingsProfileUpdatePending = false;
            if (!"SUCCESS".equals(status)) {
                String message = response.has("message") && !response.get("message").isJsonNull()
                        ? response.get("message").getAsString()
                        : "Không thể cập nhật thông tin tài khoản.";
                showSettingsAlert(Alert.AlertType.ERROR, "Cập nhật thất bại", message);
                handleSettings();
                return;
            }

            UserSession session = UserSession.getInstance();
            if (response.has("username") && !response.get("username").isJsonNull()) {
                session.setUsername(response.get("username").getAsString());
            }
            if (response.has("fullName") && !response.get("fullName").isJsonNull()) {
                session.setFullName(response.get("fullName").getAsString());
            }
            if (response.has("email") && !response.get("email").isJsonNull()) {
                session.setEmail(response.get("email").getAsString());
            }

            lblUsername.setText(session.getFullName() != null ? session.getFullName() : session.getUsername());
            showSettingsAlert(Alert.AlertType.INFORMATION, "Cập nhật thành công", "Thông tin tài khoản đã được lưu vào hệ thống.");
            handleSettings();
        });
    }

    private void showSettingsAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }
        alert.showAndWait();
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
        StackPane iconWrap = new StackPane(iconView(iconPath, 15));
        iconWrap.getStyleClass().add("settings-section-icon");
        HBox title = new HBox(12, iconWrap, label);
        title.getStyleClass().add("settings-section-header");
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
        showDepositDialog().ifPresent(amount -> {
            JsonObject request = new JsonObject();
            request.addProperty("action", "DEPOSIT");
            request.addProperty("amount", amount);
            NetworkClient.getInstance().sendJson(request);
        });
    }

    private Optional<Double> showDepositDialog() {
        NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        double currentBalance = UserSession.getInstance().getBalance();

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("BidVault Private Banking");
        dialog.setResizable(false);

        ButtonType confirmType = new ButtonType("Xác nhận nạp", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(confirmType, cancelType);

        Label kicker = new Label("PRIVATE VAULT");
        kicker.getStyleClass().add("deposit-dialog-kicker");
        Label title = new Label("Nạp tiền đấu giá");
        title.getStyleClass().add("deposit-dialog-title");
        Label subtitle = new Label("Nâng hạn mức ví để sẵn sàng chốt các phiên đấu giá cao cấp.");
        subtitle.getStyleClass().add("deposit-dialog-subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(6, kicker, title, subtitle);
        header.getStyleClass().add("deposit-dialog-header");

        Label balanceCaption = new Label("Số dư hiện tại");
        balanceCaption.getStyleClass().add("deposit-balance-caption");
        Label balanceValue = new Label(currencyFmt.format(currentBalance) + " ₫");
        balanceValue.getStyleClass().add("deposit-balance-value");
        VBox balanceBox = new VBox(4, balanceCaption, balanceValue);
        balanceBox.getStyleClass().add("deposit-balance-card");
        balanceBox.setAlignment(Pos.CENTER_LEFT);

        TextField amountField = new TextField("500000");
        amountField.getStyleClass().add("deposit-amount-field");
        amountField.setPromptText("Nhập số tiền (VNĐ)");

        Label amountLabel = new Label("Số tiền nạp");
        amountLabel.getStyleClass().add("deposit-field-label");
        VBox amountBox = new VBox(8, amountLabel, amountField);

        Label quickLabel = new Label("Gợi ý nhanh");
        quickLabel.getStyleClass().add("deposit-field-label");
        HBox quickRow = new HBox(8);
        quickRow.getStyleClass().add("deposit-quick-row");
        for (long preset : new long[]{500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000}) {
            Button chip = new Button(formatDepositChip(preset));
            chip.getStyleClass().add("deposit-quick-chip");
            chip.setOnAction(e -> amountField.setText(String.valueOf(preset)));
            quickRow.getChildren().add(chip);
        }

        Label note = new Label("Giao dịch được xử lý tức thì. Số dư cập nhật ngay sau khi xác nhận.");
        note.getStyleClass().add("deposit-dialog-note");
        note.setWrapText(true);

        VBox content = new VBox(18, header, balanceBox, amountBox, quickLabel, quickRow, note);
        content.getStyleClass().add("deposit-dialog-content");
        content.setPadding(new Insets(4, 4, 0, 4));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setHeader(null);
        applyStylesheet(dialog.getDialogPane());
        dialog.getDialogPane().getStyleClass().add("deposit-dialog-pane");

        dialog.setOnShown(e -> {
            Node confirmBtn = dialog.getDialogPane().lookupButton(confirmType);
            Node cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
            if (confirmBtn != null) {
                confirmBtn.getStyleClass().add("deposit-dialog-confirm");
            }
            if (cancelBtn != null) {
                cancelBtn.getStyleClass().add("deposit-dialog-cancel");
            }
            amountField.requestFocus();
            amountField.selectAll();
        });

        dialog.setResultConverter(button -> {
            if (button != confirmType) {
                return null;
            }
            try {
                double amount = Double.parseDouble(amountField.getText().replaceAll("[,.\\s]", ""));
                if (amount <= 0) {
                    System.err.println("Số tiền nạp phải lớn hơn 0!");
                    return null;
                }
                return amount;
            } catch (Exception ex) {
                System.err.println("Vui lòng nhập số tiền hợp lệ!");
                return null;
            }
        });

        return dialog.showAndWait();
    }

    private String formatDepositChip(long amount) {
        if (amount >= 1_000_000 && amount % 1_000_000 == 0) {
            return (amount / 1_000_000) + "M";
        }
        if (amount >= 1_000 && amount % 1_000 == 0) {
            return (amount / 1_000) + "K";
        }
        return String.valueOf(amount);
    }

    private void applyStylesheet(javafx.scene.control.DialogPane pane) {
        try {
            pane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Lỗi load CSS cho dialog nạp tiền!");
        }
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
