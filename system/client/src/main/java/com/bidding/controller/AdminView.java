package com.bidding.controller;

import com.bidding.network.NetworkClient;
import com.bidding.model.UserSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class AdminView implements Initializable {

    // ==================== SIDEBAR ====================
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navApprovals;
    @FXML private Button navAuctions;
    @FXML private Button navAuditLog;
    @FXML private Button navStats;
    @FXML private Label  clockLabel;
    @FXML private Label  connectionLabel;

    // ==================== SECTIONS ====================
    @FXML private VBox dashboardSection;
    @FXML private VBox usersSection;
    @FXML private VBox approvalsSection;
    @FXML private VBox auctionsAdminSection;
    @FXML private VBox auditLogSection;
    @FXML private VBox statsSection;

    // ==================== DASHBOARD LABELS ====================
    @FXML private Label statTotalUsers;
    @FXML private Label statUsersGrowth;
    @FXML private Label statActiveSessions;
    @FXML private Label statSessionsInfo;
    @FXML private Label statTotalBids;
    @FXML private Label statBidsToday;
    @FXML private Label statTotalRevenue;
    @FXML private Label statRevenueGrowth;

    // ==================== DASHBOARD CHARTS ====================
    @FXML private BarChart<String, Number> bidBarChart;
    @FXML private PieChart                 categoryPieChart;

    // ==================== USERS ====================
    @FXML private TextField              userSearchField;
    @FXML private ComboBox<String>       userRoleFilter;
    @FXML private TableView<UserRow>     userTable;
    @FXML private TableColumn<UserRow, String> colUserId;
    @FXML private TableColumn<UserRow, String> colUserName;
    @FXML private TableColumn<UserRow, String> colUserEmail;
    @FXML private TableColumn<UserRow, String> colUserRole;
    @FXML private TableColumn<UserRow, String> colUserBalance;
    @FXML private TableColumn<UserRow, Void>   colUserActions;
    @FXML private Label userPageInfo;
    @FXML private Label userCountLabel;

    @FXML private VBox createAdminBox;
    @FXML private TextField newAdminUsernameField;
    @FXML private TextField newAdminEmailField;
    @FXML private TextField newAdminFullNameField;
    @FXML private PasswordField newAdminPasswordField;

    // ==================== PRODUCT APPROVALS ====================
    @FXML private TableView<PendingItemRow> pendingItemTable;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemId;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemName;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemSeller;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemType;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemPrice;
    @FXML private TableColumn<PendingItemRow, Void> colPendingItemAction;

    // ==================== AUCTIONS ====================
    @FXML private TextField                 aucSearchField;
    @FXML private ComboBox<String>          aucStatusFilter;
    @FXML private TableView<AuctionRow>     adminAucTable;
    @FXML private TableColumn<AuctionRow, String> colAdminAucId;
    @FXML private TableColumn<AuctionRow, String> colAdminAucTitle;
    @FXML private TableColumn<AuctionRow, String> colAdminAucStatus;
    @FXML private TableColumn<AuctionRow, String> colAdminAucPrice;
    @FXML private TableColumn<AuctionRow, Void>   colAdminAucAction;
    @FXML private Label aucPageInfo;
    @FXML private Label aucCountLabel;

    // ==================== AUDIT ====================
    @FXML private DatePicker              auditDateFilter;
    @FXML private TableView<AuditRow>     auditTable;
    @FXML private TableColumn<AuditRow, String> colAuditTime;
    @FXML private TableColumn<AuditRow, String> colAuditActor;
    @FXML private TableColumn<AuditRow, String> colAuditAction;
    @FXML private TableColumn<AuditRow, String> colAuditTarget;
    @FXML private TableColumn<AuditRow, String> colAuditDetail;

    // ==================== STATS ====================
    @FXML private BarChart<String, Number>  revenueChart;
    @FXML private LineChart<String, Number> usersGrowthChart;
    @FXML private PieChart                  rolesPieChart;

    // ==================== STATE ====================
    private final ObservableList<UserRow>    allUsers         = FXCollections.observableArrayList();
    private final ObservableList<AuctionRow> allAuctions      = FXCollections.observableArrayList();
    private final ObservableList<PendingItemRow> pendingItems = FXCollections.observableArrayList();
    private final ObservableList<AuditRow>   allAuditLogs     = FXCollections.observableArrayList();
    private final ObservableList<UserRow>    filteredUsers    = FXCollections.observableArrayList();
    private final ObservableList<AuctionRow> filteredAuctions = FXCollections.observableArrayList();
    private final ObservableList<BidRow>     currentBidRows   = FXCollections.observableArrayList();

    private int     userCurrentPage = 1;
    private int     aucCurrentPage  = 1;
    private static final int PAGE_SIZE = 20;
    private boolean adminLoggedIn   = false;
    private int adminLevel = 0;
    private boolean connectionStarted = false;
    private Dialog<Void> bidHistoryDialog;
    private TableView<BidRow> bidHistoryTable;
    private Label bidHistorySummary;
    private String bidHistoryAuctionId;

    // ==================== INIT ====================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();
        setupUserTable();
        setupApprovalTable();
        setupAuctionTable();
        hideOutOfScopeAdminFeatures();
        setupComboBoxes();
        setupNetworkListeners();
        connectToServer();
        adminLoggedIn = "ADMIN".equals(UserSession.getInstance().getRole());
        adminLevel = UserSession.getInstance().getAdminLevel();
        if (adminLoggedIn && adminLevel == 0) adminLevel = 2;
        configureAdminLevelVisibility();
        showUsers();
    }

    private void hideOutOfScopeAdminFeatures() {
        for (Button btn : new Button[]{navDashboard, navAuditLog, navStats}) {
            if (btn != null) {
                btn.setVisible(false);
                btn.setManaged(false);
            }
        }
        for (VBox section : new VBox[]{dashboardSection, auditLogSection, statsSection}) {
            if (section != null) {
                section.setVisible(false);
                section.setManaged(false);
            }
        }
    }


    // ==================== CLOCK ====================
    private void startClock() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String time = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));
                Platform.runLater(() -> clockLabel.setText(time));
            }
        }, 0, 1000);
    }

    // ==================== CONNECTION STATUS ====================
    private void updateConnectionStatus() {
        connectionLabel.setText("● Đang kết nối");
        connectionLabel.setStyle("-fx-text-fill: #E0B44C; -fx-font-size: 11px; -fx-padding: 0 0 8 4;");
    }

    private void connectToServer() {
        if (!connectionStarted) {
            connectionStarted = true;
            NetworkClient.getInstance().connect();
        }
        updateConnectionStatus();
    }

    // ==================== ADMIN ACTIONS ====================
    private void sendApproveItem(String itemId, String name) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "APPROVE_ITEM");
        req.addProperty("itemId", itemId);
        sendRequest(req);
        appendAuditRow("APPROVE_ITEM", UserSession.getInstance().getUsername(), name + " (ID: " + itemId + ")", "Đang chờ...");
    }

    private void sendRejectItem(String itemId, String name, String reason) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "REJECT_ITEM");
        req.addProperty("itemId", itemId);
        req.addProperty("reason", reason);
        sendRequest(req);
        appendAuditRow("REJECT_ITEM", UserSession.getInstance().getUsername(), name + " (ID: " + itemId + ")", "Đang chờ...");
    }

    @FXML private void handleCreateAdminLevel1() {
        if (!checkConnected()) return;
        if (adminLevel < 2) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ admin level 2 mới được tạo admin level 1.");
            return;
        }

        String username = newAdminUsernameField.getText().trim();
        String email = newAdminEmailField.getText().trim();
        String fullName = newAdminFullNameField.getText().trim();
        String password = newAdminPasswordField.getText().trim();
        if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin admin level 1.");
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("action", "CREATE_ADMIN_LEVEL1");
        req.addProperty("username", username);
        req.addProperty("email", email);
        req.addProperty("fullName", fullName);
        req.addProperty("password", password);
        sendRequest(req);
        appendAuditRow("CREATE_ADMIN_LEVEL1", UserSession.getInstance().getUsername(), username, "Đang chờ...");
    }

    // ==================== NAVIGATION ====================
    @FXML public void showDashboard() {
        showSection(dashboardSection);
        highlightNav(navDashboard);
        if (adminLoggedIn) requestDashboardData();
    }

    @FXML public void showUsers() {
        showSection(usersSection);
        highlightNav(navUsers);
        if (adminLoggedIn) requestAllUsers();
        else warnNotLoggedIn();
    }

    @FXML public void showApprovals() {
        showSection(approvalsSection);
        highlightNav(navApprovals);
        if (adminLoggedIn) requestPendingItems();
        else warnNotLoggedIn();
    }

    @FXML public void showAuctions() {
        showSection(auctionsAdminSection);
        highlightNav(navAuctions);
        if (adminLoggedIn) requestAllAuctions();
        else warnNotLoggedIn();
    }

    @FXML public void showAuditLog() {
        showSection(auditLogSection);
        highlightNav(navAuditLog);
        auditTable.setItems(allAuditLogs);
    }

    @FXML public void showStats() {
        showSection(statsSection);
        highlightNav(navStats);
        if (adminLoggedIn) requestStatsData();
        else warnNotLoggedIn();
    }

    private void showSection(VBox target) {
        dashboardSection.setVisible(false);
        usersSection.setVisible(false);
        approvalsSection.setVisible(false);
        auctionsAdminSection.setVisible(false);
        auditLogSection.setVisible(false);
        statsSection.setVisible(false);
        target.setVisible(true);
    }

    private void highlightNav(Button active) {
        for (Button btn : new Button[]{navDashboard, navUsers, navApprovals, navAuctions, navAuditLog, navStats}) {
            if (btn == null) continue;
            btn.getStyleClass().remove("nav-btn-active");
            if (!btn.getStyleClass().contains("nav-btn")) btn.getStyleClass().add("nav-btn");
        }
        if (active != null) active.getStyleClass().add("nav-btn-active");
    }

    private void warnNotLoggedIn() {
        showAlert(Alert.AlertType.WARNING, "Chưa xác thực",
                "Đang chờ đăng nhập admin...\nVui lòng thử lại sau vài giây.");
    }

    @FXML public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                adminLoggedIn = false;
                NetworkClient.getInstance().setMessageHandler(null);
                UserSession.getInstance().clear();
                AppNavigator.navigate("Login.fxml");
            }
        });
    }

    // ==================== TABLE SETUP ====================
    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balanceFormatted"));
        colUserActions.setCellFactory(buildBanButtonColumn());
        userTable.setItems(filteredUsers);
    }

    private void setupApprovalTable() {
        if (pendingItemTable == null) return;
        colPendingItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPendingItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPendingItemSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colPendingItemType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPendingItemPrice.setCellValueFactory(new PropertyValueFactory<>("priceFormatted"));
        colPendingItemAction.setCellFactory(buildApprovalButtonColumn());
        pendingItemTable.setItems(pendingItems);
    }

    private void setupAuctionTable() {
        adminAucTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        colAdminAucId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminAucTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAdminAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminAucPrice.setCellValueFactory(new PropertyValueFactory<>("priceFormatted"));
        colAdminAucId.setCellFactory(alignedTextCell("-fx-alignment: CENTER_LEFT;"));
        colAdminAucTitle.setCellFactory(alignedTextCell("-fx-alignment: CENTER;"));
        colAdminAucStatus.setCellFactory(alignedTextCell("-fx-alignment: CENTER;"));
        colAdminAucPrice.setCellFactory(alignedTextCell("-fx-alignment: CENTER;"));
        colAdminAucAction.setCellFactory(buildForceCloseButtonColumn());
        colAdminAucId.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        colAdminAucTitle.setMaxWidth(1f * Integer.MAX_VALUE * 28);
        colAdminAucStatus.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colAdminAucPrice.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        colAdminAucAction.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        adminAucTable.setItems(filteredAuctions);
    }

    private void setupAuditTable() {
        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAuditActor.setCellValueFactory(new PropertyValueFactory<>("actor"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colAuditDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));
        auditTable.setItems(allAuditLogs);
    }

    private Callback<TableColumn<AuctionRow, String>, TableCell<AuctionRow, String>> alignedTextCell(String style) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value);
                setStyle(style);
            }
        };
    }

    private void setupComboBoxes() {
        userRoleFilter.setItems(FXCollections.observableArrayList("Tất cả", "USER", "ADMIN L1", "ADMIN L2"));
        userRoleFilter.getSelectionModel().selectFirst();
        aucStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "RUNNING", "OPEN", "FINISHED", "FAILED", "PAID", "CANCELED"));
        aucStatusFilter.getSelectionModel().selectFirst();
    }

    // ==================== BAN USER BUTTON ====================
    private Callback<TableColumn<UserRow, Void>, TableCell<UserRow, Void>> buildBanButtonColumn() {
        return param -> new TableCell<>() {
            private final Button banBtn = new Button("Khóa");

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                UserRow row = getTableView().getItems().get(getIndex());
                banBtn.getStyleClass().removeAll("btn-danger", "btn-outline");
                if (row.isActive()) {
                    banBtn.setText("Ban");
                    banBtn.getStyleClass().add("btn-danger");
                    banBtn.setOnAction(e -> confirmBanUser(row));
                } else {
                    banBtn.setText("Unban");
                    banBtn.getStyleClass().add("btn-outline");
                    banBtn.setOnAction(e -> confirmUnbanUser(row));
                }
                banBtn.setDisable(row.getId().equals(UserSession.getInstance().getUserId()));
                setGraphic(banBtn);
            }
        };
    }

    private void confirmBanUser(UserRow user) {
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận khóa tài khoản");
        confirm.setHeaderText("Khóa: " + user.getUsername());
        confirm.setContentText("ID: " + user.getId() + "  |  Role: " + user.getRole()
                + "\n\nHành động này sẽ khóa tài khoản ngay lập tức.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) sendBanUser(user.getId(), user.getUsername());
        });
    }

    private void confirmUnbanUser(UserRow user) {
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mở khóa tài khoản");
        confirm.setHeaderText("Mở khóa: " + user.getUsername());
        confirm.setContentText("ID: " + user.getId() + "  |  Role: " + user.getRole()
                + "\n\nHành động này sẽ cho phép tài khoản đăng nhập lại.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) sendUnbanUser(user.getId(), user.getUsername());
        });
    }

    private Callback<TableColumn<PendingItemRow, Void>, TableCell<PendingItemRow, Void>> buildApprovalButtonColumn() {
        return param -> new TableCell<>() {
            private final Button detailBtn = new Button("Chi tiết");
            private final Button approveBtn = new Button("Duyệt");
            private final Button rejectBtn = new Button("Từ chối");
            private final HBox actions = new HBox(8, detailBtn, approveBtn, rejectBtn);
            {
                detailBtn.getStyleClass().add("btn-outline");
                approveBtn.getStyleClass().add("btn-gold");
                rejectBtn.getStyleClass().add("btn-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                PendingItemRow row = getTableView().getItems().get(getIndex());
                actions.setStyle("-fx-alignment: center;");
                detailBtn.setOnAction(e -> showPendingItemDetails(row));
                approveBtn.setOnAction(e -> confirmApproveItem(row));
                rejectBtn.setOnAction(e -> confirmRejectItem(row));
                setGraphic(actions);
            }
        };
    }

    private void showPendingItemDetails(PendingItemRow row) {
        JsonObject item = row.getItemJson();
        StringBuilder details = new StringBuilder();
        details.append("ID: ").append(row.getId()).append("\n");
        details.append("Sản phẩm: ").append(row.getName()).append("\n");
        details.append("Người bán: ").append(row.getSeller()).append("\n");
        details.append("Loại: ").append(row.getType()).append("\n");
        details.append("Giá khởi điểm: ").append(row.getPriceFormatted()).append("\n\n");
        details.append(getStr(item, "description"));

        showAlert(Alert.AlertType.INFORMATION, "Chi tiết sản phẩm", details.toString());
    }

    private void confirmApproveItem(PendingItemRow row) {
        if (row == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Duyệt sản phẩm");
        confirm.setHeaderText(row.getName());
        confirm.setContentText("Duyệt sản phẩm này để người bán có thể mở phiên đấu giá?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) sendApproveItem(row.getId(), row.getName());
        });
    }

    private void confirmRejectItem(PendingItemRow row) {
        if (row == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối sản phẩm");
        dialog.setHeaderText(row.getName());
        dialog.setContentText("Lý do từ chối:");
        dialog.showAndWait().ifPresent(reason -> sendRejectItem(row.getId(), row.getName(), reason));
    }

    // ==================== FORCE CLOSE BUTTON ====================
    private Callback<TableColumn<AuctionRow, Void>, TableCell<AuctionRow, Void>> buildForceCloseButtonColumn() {
        return param -> new TableCell<>() {
            private final Button closeBtn = new Button("Đóng phiên");
            private final Button viewBtn = new Button("Chi tiết");
            private final HBox actions = new HBox(8, viewBtn, closeBtn);
            {
                viewBtn.getStyleClass().add("btn-outline");
                closeBtn.getStyleClass().add("btn-warn");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AuctionRow row = getTableView().getItems().get(getIndex());
                actions.setStyle("-fx-alignment: center;");
                viewBtn.setOnAction(e -> showAuctionItemDetails(row));
                if ("RUNNING".equals(row.getStatus())) {
                    closeBtn.setOnAction(e -> confirmForceClose(row));
                    closeBtn.setVisible(true);
                    closeBtn.setManaged(true);
                } else {
                    closeBtn.setVisible(false);
                    closeBtn.setManaged(false);
                }
                setGraphic(actions);
            }
        };
    }

    private void showAuctionItemDetails(AuctionRow auction) {
        JsonObject item = auction.getItemJson();
        if (item == null) {
            showAlert(Alert.AlertType.WARNING, "Không có dữ liệu", "Không tìm thấy thông tin sản phẩm của phiên này.");
            return;
        }

        bidHistoryAuctionId = auction.getId();
        currentBidRows.clear();

        Dialog<Void> dialog = new Dialog<>();
        bidHistoryDialog = dialog;
        dialog.setTitle("Chi tiết phiên đấu giá");
        dialog.setHeaderText(auction.getTitle() + " - " + auction.getId());
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.getDialogPane().setStyle("-fx-background-color: #0b0b0b; -fx-border-color: #b99300; -fx-border-width: 1;");

        VBox content = new VBox(12);
        content.setMinWidth(260);
        content.setPrefWidth(280);
        content.setMaxWidth(300);
        content.setStyle("-fx-padding: 14; -fx-font-size: 14px; -fx-background-color: #0b0b0b;");

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitWidth(252);
        imageView.setFitHeight(145);
        imageView.setPreserveRatio(false);
        if (item.has("images") && item.get("images").isJsonArray() && item.getAsJsonArray("images").size() > 0) {
            String imagePath = item.getAsJsonArray("images").get(0).getAsString();
            java.io.File file = new java.io.File(imagePath);
            if (file.exists()) {
                imageView.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                content.getChildren().add(imageView);
            } else {
                content.getChildren().add(new Label("(Không tìm thấy ảnh)"));
            }
        } else {
            content.getChildren().add(new Label("(Chưa có hình ảnh)"));
        }

        Label type = new Label("Loại: " + getStr(item, "type"));
        String conditionText = getStr(item, "condition");
        if ("NEW".equals(conditionText)) conditionText = "Mới 100%";
        else if ("USED".equals(conditionText)) conditionText = "Đã sử dụng";
        Label condition = new Label("Tình trạng: " + conditionText);
        condition.setStyle("-fx-font-style: italic; -fx-text-fill: #8aa0bd;");

        double startingPrice = item.has("startingPrice") && !item.get("startingPrice").isJsonNull()
                ? item.get("startingPrice").getAsDouble() : 0.0;
        Label price = new Label(String.format("Giá khởi điểm: %,.0f đ", startingPrice));
        price.setStyle("-fx-text-fill: #f0c040; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label desc = new Label("Mô tả: " + getStr(item, "description"));
        desc.setWrapText(true);
        content.getChildren().addAll(type, condition, price, new Separator(), desc);

        if (item.has("specifications") && item.get("specifications").isJsonObject()) {
            VBox specsBox = new VBox(8);
            specsBox.setStyle("-fx-padding: 12; -fx-background-color: #111820; -fx-background-radius: 8; -fx-border-color: #3a3522; -fx-border-radius: 8;");
            specsBox.getChildren().add(new Label("Thông số chi tiết:"));
            JsonObject specs = item.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                specsBox.getChildren().add(new Label("• " + key + ": " + specs.get(key).getAsString()));
            }
            content.getChildren().add(specsBox);
        }

        VBox bidPane = new VBox(12);
        bidPane.setPrefWidth(700);
        bidPane.setStyle("-fx-padding: 14 18 14 10; -fx-font-size: 13px; -fx-background-color: #0b0b0b;");

        bidHistorySummary = new Label("Đang tải danh sách lượt đặt giá...");
        bidHistorySummary.setStyle("-fx-text-fill: #8aa0bd; -fx-font-size: 13px;");

        bidHistoryTable = new TableView<>();
        bidHistoryTable.setItems(currentBidRows);
        bidHistoryTable.setPlaceholder(new Label("Chưa có lượt đặt giá nào."));
        bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bidHistoryTable.setPrefHeight(320);
        VBox.setVgrow(bidHistoryTable, Priority.ALWAYS);

        TableColumn<BidRow, String> timeCol = new TableColumn<>("Thời gian");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));

        TableColumn<BidRow, String> bidderCol = new TableColumn<>("Người đặt");
        bidderCol.setCellValueFactory(new PropertyValueFactory<>("bidder"));

        TableColumn<BidRow, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<BidRow, String> amountCol = new TableColumn<>("Mức giá");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amountFormatted"));

        TableColumn<BidRow, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<BidRow, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        timeCol.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        bidderCol.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        emailCol.setMaxWidth(1f * Integer.MAX_VALUE * 16);
        amountCol.setMaxWidth(1f * Integer.MAX_VALUE * 16);
        typeCol.setMaxWidth(1f * Integer.MAX_VALUE * 13);
        statusCol.setMaxWidth(1f * Integer.MAX_VALUE * 23);

        bidHistoryTable.getColumns().addAll(timeCol, bidderCol, emailCol, amountCol, typeCol, statusCol);
        Label bidTitle = new Label("Lượt đặt giá");
        bidTitle.getStyleClass().add("detail-section-title");
        bidPane.getChildren().addAll(bidTitle, bidHistorySummary, bidHistoryTable);

        HBox detailLayout = new HBox(16, content, bidPane);
        HBox.setHgrow(bidPane, Priority.ALWAYS);
        detailLayout.setPrefWidth(1000);
        detailLayout.setPrefHeight(430);
        detailLayout.setStyle("-fx-background-color: #0b0b0b;");

        ScrollPane scrollPane = new ScrollPane(detailLayout);
        scrollPane.getStyleClass().add("auction-detail-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportWidth(1040);
        scrollPane.setPrefViewportHeight(480);
        scrollPane.setMaxHeight(520);
        scrollPane.setStyle("-fx-background: #0b0b0b; -fx-background-color: #0b0b0b;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(1080);
        dialog.getDialogPane().setMaxHeight(620);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setOnHidden(e -> {
            bidHistoryDialog = null;
            bidHistoryTable = null;
            bidHistorySummary = null;
            bidHistoryAuctionId = null;
            currentBidRows.clear();
        });
        dialog.show();

        requestAuctionBidHistory(auction.getId());
    }

    private void confirmForceClose(AuctionRow auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đóng phiên");
        confirm.setHeaderText("Ép đóng: " + auction.getTitle());
        confirm.setContentText("ID phiên: " + auction.getId()
                + "\nGiá hiện tại: " + auction.getPriceFormatted()
                + "\n\nHành động này kết thúc phiên ngay lập tức!");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) sendForceClose(auction.getId(), auction.getTitle());
        });
    }

    // ==================== NETWORK REQUESTS ====================

    private void requestDashboardData() {
        requestAllUsers();
    }

    private void requestAllUsers() {
        sendAction("GET_ALL_USERS");
    }

    private void requestPendingItems() {
        sendAction("GET_PENDING_ITEMS");
    }

    private void requestAllAuctions() {
        sendAction("GET_AUCTIONS");
    }

    private void requestAuctionBidHistory(String auctionId) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTION_HISTORY");
        req.addProperty("auctionId", auctionId);
        sendRequest(req);
    }

    private void requestStatsData() {
        sendAction("GET_ALL_USERS");
        sendAction("GET_AUCTIONS");
    }

    /**
     * BAN_USER
     * {
     *   "action":       "BAN_USER",
     *   "targetUserId": "u1"         â† field báº¯t buá»™c Ä‘Ãºng tÃªn
     * }
     */
    private void sendBanUser(String userId, String username) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action",       "BAN_USER");
        req.addProperty("targetUserId", userId);
        sendRequest(req);
        appendAuditRow("BAN_USER", "admin", username + " (ID: " + userId + ")", "Đang chờ...");
    }

    private void sendUnbanUser(String userId, String username) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "UNBAN_USER");
        req.addProperty("targetUserId", userId);
        sendRequest(req);
        appendAuditRow("UNBAN_USER", "admin", username + " (ID: " + userId + ")", "Đang chờ...");
    }

    /**
     * FORCE_CLOSE
     * {
     *   "action":    "FORCE_CLOSE",
     *   "auctionId": "auc789"
     * }
     */
    private void sendForceClose(String auctionId, String title) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action",    "FORCE_CLOSE");
        req.addProperty("auctionId", auctionId);
        sendRequest(req);
        appendAuditRow("FORCE_CLOSE", "admin", title + " (ID: " + auctionId + ")", "Đang chờ...");
    }

    private void sendAction(String action) {
        if (!checkConnected()) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", action);
        sendRequest(req);
    }

    private boolean checkConnected() {
        connectToServer();
        return true;
    }

    private void sendRequest(JsonObject req) {
        connectToServer();
        NetworkClient.getInstance().sendJson(req);
        connectionLabel.setText("● Đã gửi yêu cầu");
        connectionLabel.setStyle("-fx-text-fill: #40DD80; -fx-font-size: 11px; -fx-padding: 0 0 8 4;");
    }

    // ==================== NETWORK LISTENERS ====================
    /*
     * NetworkClient hiện tại chỉ có một messageHandler.
     * AdminView gom toàn bộ response admin vào handler này.
     */
    private void setupNetworkListeners() {
        NetworkClient.getInstance().setMessageHandler(response -> {
            Platform.runLater(() -> handleServerMessage(response));
        });
    }

    private void handleServerMessage(JsonObject response) {
        String action = getStr(response, "action");
        switch (action) {
            case "LOGIN_REPLY"       -> handleLoginReply(response);
            case "USERS_LIST"        -> handleUsersList(response);
            case "BAN_USER_REPLY"    -> handleBanUserReply(response);
            case "UNBAN_USER_REPLY"  -> handleUnbanUserReply(response);
            case "FORCE_CLOSE_REPLY" -> handleForceCloseReply(response);
            case "AUCTIONS_LIST"     -> handleAuctionsList(response);
            case "PENDING_ITEMS_LIST" -> handlePendingItemsList(response);
            case "APPROVE_ITEM_REPLY" -> handleApprovalMutationReply(response, "APPROVE_ITEM");
            case "REJECT_ITEM_REPLY"  -> handleApprovalMutationReply(response, "REJECT_ITEM");
            case "CREATE_ADMIN_LEVEL1_REPLY" -> handleCreateAdminLevel1Reply(response);
            case "AUCTION_HISTORY_REPLY" -> handleAuctionHistoryReply(response);
            case "HISTORY_REPLY"     -> handleHistoryReply(response);
            case "GLOBAL_NOTIFY"     -> handleGlobalNotify(response);
            case "ERROR"             -> handleError(response);
        }
    }
    // ==================== RESPONSE HANDLERS ====================

    /**
     * LOGIN_REPLY
     * Response: { "action": "LOGIN_REPLY", "status": "SUCCESS", "myId": "...", "role": "ADMIN" }
     */
    private void handleLoginReply(JsonObject res) {
        String status = getStr(res, "status");
        String role   = getStr(res, "role");

        if ("SUCCESS".equals(status) && "ADMIN".equals(role)) {
            adminLoggedIn = true;
            adminLevel = getInt(res, "adminLevel");
            if (adminLevel == 0) adminLevel = 2;
            UserSession.getInstance().setAdminLevel(adminLevel);
            configureAdminLevelVisibility();
            appendAuditRow("LOGIN", "admin", "Hệ thống", "Đăng nhập admin thành công");
            requestDashboardData();
        } else {
            adminLoggedIn = false;
            adminLevel = 0;
            configureAdminLevelVisibility();
            showAlert(Alert.AlertType.ERROR, "Lỗi xác thực",
                    "Đăng nhập admin thất bại.\nStatus: " + status + "  |  Role nhận: " + role
                    + "\nVui lòng kiểm tra credentials.");
        }
    }

    /**
     * USERS_LIST
     * Response: { "action": "USERS_LIST", "data": [ { "id", "username", "role", "balance" } ] }
     */
    private void handleUsersList(JsonObject res) {
        allUsers.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) return;

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject o = el.getAsJsonObject();
            String role = getDisplayRole(getStr(o, "role"), getInt(o, "adminLevel"));
            allUsers.add(new UserRow(
                    getStr(o, "id"),
                    getStr(o, "username"),
                    getStr(o, "email"),
                    role,
                    o.has("balance") && !o.get("balance").isJsonNull()
                            ? o.get("balance").getAsDouble() : 0.0,
                    getActiveValue(o)
            ));
        }

        statTotalUsers.setText(String.valueOf(allUsers.size()));
        statUsersGrowth.setText("Tổng tài khoản: " + allUsers.size());
        applyUserFilter();
        updateRolesPieChart();
    }

    private void handlePendingItemsList(JsonObject res) {
        pendingItems.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) return;

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject item = el.getAsJsonObject();
            pendingItems.add(new PendingItemRow(
                    getStr(item, "id"),
                    getStr(item, "name"),
                    getStr(item, "sellerFullName"),
                    getStr(item, "type"),
                    item.has("startingPrice") && !item.get("startingPrice").isJsonNull()
                            ? item.get("startingPrice").getAsDouble() : 0.0,
                    item
            ));
        }
    }

    private void handleApprovalMutationReply(JsonObject res, String actionName) {
        String status = getStr(res, "status");
        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", getStr(res, "message"));
            requestPendingItems();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", getStr(res, "message"));
        }
        updateLastAuditDetail(actionName, "Đã xử lý: " + status);
    }

    private void handleCreateAdminLevel1Reply(JsonObject res) {
        String status = getStr(res, "status");
        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", getStr(res, "message"));
            newAdminUsernameField.clear();
            newAdminEmailField.clear();
            newAdminFullNameField.clear();
            newAdminPasswordField.clear();
            requestAllUsers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", getStr(res, "message"));
        }
        updateLastAuditDetail("CREATE_ADMIN_LEVEL1", "Đã xử lý: " + status);
    }

    /**
     * AUCTIONS_LIST
     * Response: { "action": "AUCTIONS_LIST", "data": [ { "id", "item": {...}, "currentPrice", "status" } ] }
     */
    private void handleAuctionsList(JsonObject res) {
        allAuctions.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) return;

        int running = 0, ended = 0, other = 0;
        double totalRevenue = 0.0;

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject o      = el.getAsJsonObject();
            String id         = getStr(o, "id");
            String status     = getStr(o, "status");
            double price      = o.has("currentPrice") && !o.get("currentPrice").isJsonNull()
                    ? o.get("currentPrice").getAsDouble() : 0.0;

            String title = "(Không có tên)";
            JsonObject itemJson = null;
            if (o.has("item") && !o.get("item").isJsonNull() && o.get("item").isJsonObject()) {
                itemJson = o.getAsJsonObject("item");
                String n = getStr(itemJson, "name");
                if (!n.isEmpty()) title = n;
            }

            allAuctions.add(new AuctionRow(id, title, status, price, itemJson));
            switch (status) {
                case "RUNNING" -> running++;
                case "ENDED"   -> { ended++; totalRevenue += price; }
                default        -> other++;
            }
        }

        statActiveSessions.setText(String.valueOf(running));
        statSessionsInfo.setText("Phiên đang chạy");
        statTotalRevenue.setText(String.format("VNĐ %,.0f", totalRevenue));
        statRevenueGrowth.setText("Từ " + ended + " phiên đã kết thúc");

        categoryPieChart.getData().clear();
        if (running > 0) categoryPieChart.getData().add(new PieChart.Data("RUNNING (" + running + ")", running));
        if (ended > 0)   categoryPieChart.getData().add(new PieChart.Data("ENDED (" + ended + ")", ended));
        if (other > 0)   categoryPieChart.getData().add(new PieChart.Data("Khác (" + other + ")", other));

        applyAucFilter();
        updateRevenueChart();
    }

    /**
     * HISTORY_REPLY
     * Response: { "action": "HISTORY_REPLY", "data": [ { "auctionId", "bidAmount", "status" } ] }
     */
    private void handleHistoryReply(JsonObject res) {
        if (!res.has("data") || res.get("data").isJsonNull()) return;
        JsonArray data = res.getAsJsonArray("data");

        statTotalBids.setText(String.valueOf(data.size()));
        statBidsToday.setText("Tổng lượt đặt giá");

        LinkedHashMap<String, Integer> bidCount = new LinkedHashMap<>();
        for (JsonElement el : data) {
            String aucId = getStr(el.getAsJsonObject(), "auctionId");
            if (!aucId.isEmpty()) bidCount.merge(aucId, 1, Integer::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt bid");
        int count = 0;
        for (Map.Entry<String, Integer> entry : bidCount.entrySet()) {
            if (count++ >= 10) break;
            String lbl = entry.getKey().length() > 10
                    ? entry.getKey().substring(0, 10) + "..." : entry.getKey();
            series.getData().add(new XYChart.Data<>(lbl, entry.getValue()));
        }
        bidBarChart.getData().clear();
        if (!series.getData().isEmpty()) bidBarChart.getData().add(series);
    }

    private void handleAuctionHistoryReply(JsonObject res) {
        String auctionId = getStr(res, "auctionId");
        if (bidHistoryAuctionId != null && !auctionId.isEmpty() && !bidHistoryAuctionId.equals(auctionId)) {
            return;
        }

        currentBidRows.clear();
        if (res.has("data") && !res.get("data").isJsonNull()) {
            for (JsonElement el : res.getAsJsonArray("data")) {
                JsonObject o = el.getAsJsonObject();
                currentBidRows.add(new BidRow(
                        formatTimestampForDisplay(getStr(o, "timestamp")),
                        getFirstNonEmpty(o, "bidderName", "bidderId", "winnerId"),
                        getStr(o, "bidderEmail"),
                        o.has("price") && !o.get("price").isJsonNull() ? o.get("price").getAsDouble() : 0.0,
                        o.has("isAuto") && !o.get("isAuto").isJsonNull() && o.get("isAuto").getAsBoolean()
                                ? "Auto bid" : "Thủ công",
                        getStr(o, "status")
                ));
            }
        }

        if (bidHistorySummary != null) {
            bidHistorySummary.setText(currentBidRows.isEmpty()
                    ? "Phiên này chưa có lượt đặt giá nào."
                    : "Tổng lượt đặt giá: " + currentBidRows.size());
        }
    }

    /**
     * BAN_USER_REPLY
     * Response: { "action": "BAN_USER_REPLY", "status": "SUCCESS", "message": "Đã khóa..." }
     */
    private void handleBanUserReply(JsonObject res) {
        String status  = getStr(res, "status");
        String message = getStr(res, "message");

        updateLastAuditDetail("BAN_USER",
                "SUCCESS".equals(status) ? "OK " + message : "Lỗi: " + message);

        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    message.isEmpty() ? "Đã khóa tài khoản thành công!" : message);
            requestAllUsers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi khóa tài khoản",
                    message.isEmpty() ? "Không thể khóa tài khoản." : message);
        }
    }

    private void handleUnbanUserReply(JsonObject res) {
        String status  = getStr(res, "status");
        String message = getStr(res, "message");

        updateLastAuditDetail("UNBAN_USER",
                "SUCCESS".equals(status) ? "OK " + message : "Lỗi: " + message);

        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    message.isEmpty() ? "Đã mở khóa tài khoản thành công!" : message);
            requestAllUsers();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi mở khóa tài khoản",
                    message.isEmpty() ? "Không thể mở khóa tài khoản." : message);
        }
    }

    /**
     * FORCE_CLOSE_REPLY
     * Response: { "action": "FORCE_CLOSE_REPLY", "status": "SUCCESS" }
     */
    private void handleForceCloseReply(JsonObject res) {
        String status = getStr(res, "status");

        updateLastAuditDetail("FORCE_CLOSE",
                "SUCCESS".equals(status) ? "OK Đóng phiên thành công" : "Lỗi");

        if ("SUCCESS".equals(status)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ép đóng phiên đấu giá thành công!");
            requestAllAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đóng phiên đấu giá.");
        }
    }

    /**
     * GLOBAL_NOTIFY â€” server broadcast khi phiÃªn má»›i má»Ÿ
     * { "action": "GLOBAL_NOTIFY", "message": "MÃ³n hÃ ng má»›i lÃªn sÃ n: ..." }
     */
    private void handleGlobalNotify(JsonObject res) {
        String message = getStr(res, "message");
        appendAuditRow("GLOBAL_NOTIFY", "system", "Broadcast", message);
        if (adminLoggedIn) requestAllAuctions();
    }

    private void handleError(JsonObject res) {
        String msg = getStr(res, "message");
        if (msg.toLowerCase().contains("admin nay da bi khoa")) {
            adminLoggedIn = false;
            NetworkClient.getInstance().setMessageHandler(null);
            UserSession.getInstance().clear();
            showAlert(Alert.AlertType.ERROR, "Tài khoản bị khóa", "Tài khoản admin này đã bị khóa.");
            AppNavigator.navigate("Login.fxml");
            return;
        }
        if (!msg.isEmpty()) showAlert(Alert.AlertType.ERROR, "Lỗi từ server", msg);
    }

    // ==================== CHART UPDATES ====================

    private void updateRolesPieChart() {
        int u = 0, a1 = 0, a2 = 0, o = 0;
        for (UserRow row : allUsers) {
            switch (row.getRole()) {
                case "USER"   -> u++;
                case "ADMIN L1" -> a1++;
                case "ADMIN L2" -> a2++;
                default       -> o++;
            }
        }
        rolesPieChart.getData().clear();
        if (u > 0) rolesPieChart.getData().add(new PieChart.Data("USER (" + u + ")", u));
        if (a1 > 0) rolesPieChart.getData().add(new PieChart.Data("ADMIN L1 (" + a1 + ")", a1));
        if (a2 > 0) rolesPieChart.getData().add(new PieChart.Data("ADMIN L2 (" + a2 + ")", a2));
        if (o > 0) rolesPieChart.getData().add(new PieChart.Data("Khác (" + o + ")", o));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng");
        if (u > 0) series.getData().add(new XYChart.Data<>("USER", u));
        if (a1 > 0) series.getData().add(new XYChart.Data<>("ADMIN L1", a1));
        if (a2 > 0) series.getData().add(new XYChart.Data<>("ADMIN L2", a2));
        usersGrowthChart.getData().clear();
        if (!series.getData().isEmpty()) usersGrowthChart.getData().add(series);
    }

    private void updateRevenueChart() {
        LinkedHashMap<String, Double> rev = new LinkedHashMap<>();
        for (AuctionRow r : allAuctions) rev.merge(r.getStatus(), r.getCurrentPrice(), Double::sum);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tổng giá trị (VNĐ)");
        for (Map.Entry<String, Double> e : rev.entrySet()) {
            if (e.getValue() > 0) series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        revenueChart.getData().clear();
        if (!series.getData().isEmpty()) revenueChart.getData().add(series);
    }

    // ==================== SEARCH + FILTER ====================

    @FXML public void handleUserSearch() { applyUserFilter(); }

    @FXML public void clearUserSearch() {
        userSearchField.clear();
        userRoleFilter.getSelectionModel().selectFirst();
        applyUserFilter();
    }

    private void applyUserFilter() {
        String keyword = userSearchField != null ? userSearchField.getText().trim().toLowerCase() : "";
        String role    = userRoleFilter  != null ? userRoleFilter.getValue() : "Tất cả";
        filteredUsers.clear();
        for (UserRow u : allUsers) {
            boolean ok = (keyword.isEmpty()
                    || u.getUsername().toLowerCase().contains(keyword)
                    || u.getEmail().toLowerCase().contains(keyword)
                    || u.getId().toLowerCase().contains(keyword))
                    && (role == null || role.equals("Tất cả") || u.getRole().equals(role));
            if (ok) filteredUsers.add(u);
        }
        userCurrentPage = 1;
        updateUserPageInfo();
    }

    @FXML public void handleAucSearch() { applyAucFilter(); }

    @FXML public void clearAucSearch() {
        aucSearchField.clear();
        aucStatusFilter.getSelectionModel().selectFirst();
        applyAucFilter();
    }

    private void applyAucFilter() {
        String keyword = aucSearchField  != null ? aucSearchField.getText().trim().toLowerCase() : "";
        String status  = aucStatusFilter != null ? aucStatusFilter.getValue() : "Tất cả";
        filteredAuctions.clear();
        for (AuctionRow a : allAuctions) {
            boolean ok = (keyword.isEmpty()
                    || a.getTitle().toLowerCase().contains(keyword)
                    || a.getId().toLowerCase().contains(keyword))
                    && (status == null || status.equals("Tất cả") || a.getStatus().equals(status));
            if (ok) filteredAuctions.add(a);
        }
        aucCurrentPage = 1;
        updateAucPageInfo();
    }

    // ==================== AUDIT FILTER ====================

    @FXML public void filterAuditLog() {
        if (auditDateFilter.getValue() == null) return;
        String date = auditDateFilter.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        ObservableList<AuditRow> f = FXCollections.observableArrayList();
        for (AuditRow a : allAuditLogs) if (a.getTime().startsWith(date)) f.add(a);
        auditTable.setItems(f);
    }

    @FXML public void resetAuditFilter() {
        auditDateFilter.setValue(null);
        auditTable.setItems(allAuditLogs);
    }

    // ==================== PAGINATION ====================

    @FXML public void userPrevPage() {
        if (userCurrentPage > 1) { userCurrentPage--; updateUserPageInfo(); }
    }

    @FXML public void userNextPage() {
        int tp = (int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE);
        if (userCurrentPage < tp) { userCurrentPage++; updateUserPageInfo(); }
    }

    @FXML public void aucPrevPage() {
        if (aucCurrentPage > 1) { aucCurrentPage--; updateAucPageInfo(); }
    }

    @FXML public void aucNextPage() {
        int tp = (int) Math.ceil((double) filteredAuctions.size() / PAGE_SIZE);
        if (aucCurrentPage < tp) { aucCurrentPage++; updateAucPageInfo(); }
    }

    private void updateUserPageInfo() {
        int total = filteredUsers.size();
        int tp    = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        userPageInfo.setText("Trang " + userCurrentPage + " / " + tp);
        userCountLabel.setText(total + " người dùng");
    }

    private void updateAucPageInfo() {
        int total = filteredAuctions.size();
        int tp    = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        aucPageInfo.setText("Trang " + aucCurrentPage + " / " + tp);
        aucCountLabel.setText(total + " phiên");
    }

    // ==================== AUDIT LOG ====================

    private void appendAuditRow(String action, String actor, String target, String detail) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        allAuditLogs.add(0, new AuditRow(time, actor, action, target, detail));
    }

    /** Cáº­p nháº­t dÃ²ng audit log gáº§n nháº¥t theo action type */
    private void updateLastAuditDetail(String actionType, String newDetail) {
        for (int i = 0; i < Math.min(5, allAuditLogs.size()); i++) {
            AuditRow r = allAuditLogs.get(i);
            if (actionType.equals(r.getAction())) {
                allAuditLogs.set(i, new AuditRow(r.getTime(), r.getActor(), r.getAction(), r.getTarget(), newDetail));
                break;
            }
        }
    }

    // ==================== HELPERS ====================

    private String getStr(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "";
    }

    private int getInt(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }

    private String getDisplayRole(String role, int adminLevel) {
        if ("ADMIN".equals(role)) {
            return adminLevel >= 2 ? "ADMIN L2" : "ADMIN L1";
        }
        return role;
    }

    private String getFirstNonEmpty(JsonObject obj, String... keys) {
        for (String key : keys) {
            String value = getStr(obj, key);
            if (!value.isEmpty()) return value;
        }
        return "---";
    }

    private String formatTimestampForDisplay(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) return "---";
        try {
            return LocalDateTime.parse(rawTime).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception ignored) {
            return rawTime;
        }
    }

    private void configureAdminLevelVisibility() {
        boolean canCreateAdmin = adminLoggedIn && adminLevel >= 2;
        if (createAdminBox != null) {
            createAdminBox.setVisible(canCreateAdmin);
            createAdminBox.setManaged(canCreateAdmin);
        }
    }

    private boolean getActiveValue(JsonObject obj) {
        if (obj.has("active") && !obj.get("active").isJsonNull()) {
            return obj.get("active").getAsBoolean();
        }
        if (obj.has("isActive") && !obj.get("isActive").isJsonNull()) {
            return obj.get("isActive").getAsBoolean();
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ==================== MODEL CLASSES ====================

    public static class UserRow {
        private final String id, username, email, role;
        private final double balance;
        private final boolean active;

        public UserRow(String id, String username, String email, String role, double balance, boolean active) {
            this.id = id; this.username = username; this.email = email;
            this.role = role; this.balance = balance; this.active = active;
        }

        public String getId()               { return id; }
        public String getUsername()         { return username; }
        public String getEmail()            { return email; }
        public String getRole()             { return role; }
        public double getBalance()          { return balance; }
        public String getBalanceFormatted() { return String.format("VNĐ %,.0f", balance); }
        public boolean isActive()           { return active; }
    }

    public static class PendingItemRow {
        private final String id, name, seller, type;
        private final double startingPrice;
        private final JsonObject itemJson;

        public PendingItemRow(String id, String name, String seller, String type, double startingPrice, JsonObject itemJson) {
            this.id = id;
            this.name = name;
            this.seller = seller == null || seller.isEmpty() ? "---" : seller;
            this.type = type == null || type.isEmpty() ? "---" : type;
            this.startingPrice = startingPrice;
            this.itemJson = itemJson;
        }

        public String getId()             { return id; }
        public String getName()           { return name; }
        public String getSeller()         { return seller; }
        public String getType()           { return type; }
        public double getStartingPrice()  { return startingPrice; }
        public String getPriceFormatted() { return String.format("VNĐ %,.0f", startingPrice); }
        public JsonObject getItemJson()   { return itemJson; }
    }

    public static class AuctionRow {
        private final String id, title, status;
        private final double currentPrice;
        private final JsonObject itemJson;

        public AuctionRow(String id, String title, String status, double currentPrice, JsonObject itemJson) {
            this.id = id; this.title = title; this.status = status; this.currentPrice = currentPrice;
            this.itemJson = itemJson;
        }

        public String getId()             { return id; }
        public String getTitle()          { return title; }
        public String getStatus()         { return status; }
        public double getCurrentPrice()   { return currentPrice; }
        public String getPriceFormatted() { return String.format("VNĐ %,.0f", currentPrice); }
        public JsonObject getItemJson()   { return itemJson; }
    }

    public static class BidRow {
        private final String time, bidder, email, type, status;
        private final double amount;

        public BidRow(String time, String bidder, String email, double amount, String type, String status) {
            this.time = time;
            this.bidder = bidder == null || bidder.isEmpty() ? "---" : bidder;
            this.email = email == null || email.isEmpty() ? "---" : email;
            this.amount = amount;
            this.type = type == null || type.isEmpty() ? "---" : type;
            this.status = status == null || status.isEmpty() ? "---" : status;
        }

        public String getTime()            { return time; }
        public String getBidder()          { return bidder; }
        public String getEmail()           { return email; }
        public double getAmount()          { return amount; }
        public String getAmountFormatted() { return String.format("VNĐ %,.0f", amount); }
        public String getType()            { return type; }
        public String getStatus()          { return status; }
    }

    public static class AuditRow {
        private final String time, actor, action, target, detail;

        public AuditRow(String time, String actor, String action, String target, String detail) {
            this.time = time; this.actor = actor; this.action = action;
            this.target = target; this.detail = detail;
        }

        public String getTime()   { return time; }
        public String getActor()  { return actor; }
        public String getAction() { return action; }
        public String getTarget() { return target; }
        public String getDetail() { return detail; }
    }
}

