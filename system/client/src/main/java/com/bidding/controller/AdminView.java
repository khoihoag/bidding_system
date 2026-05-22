package com.bidding.controller;

import com.bidding.controller.admin.*;
import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.AuditRow;
import com.bidding.controller.admin.model.PendingItemRow;
import com.bidding.controller.admin.model.UserRow;
import com.bidding.model.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * FXML controller for the admin panel. Delegates logic to focused helper classes
 * under {@link com.bidding.controller.admin}.
 */
public class AdminView implements Initializable {

    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navApprovals;
    @FXML private Button navAuctions;
    @FXML private Button navAuditLog;
    @FXML private Button navStats;
    @FXML private Label clockLabel;
    @FXML private Label connectionLabel;

    @FXML private VBox dashboardSection;
    @FXML private VBox usersSection;
    @FXML private VBox approvalsSection;
    @FXML private VBox auctionsAdminSection;
    @FXML private VBox auditLogSection;
    @FXML private VBox statsSection;

    @FXML private Label statTotalUsers;
    @FXML private Label statUsersGrowth;
    @FXML private Label statActiveSessions;
    @FXML private Label statSessionsInfo;
    @FXML private Label statTotalBids;
    @FXML private Label statBidsToday;
    @FXML private Label statTotalRevenue;
    @FXML private Label statRevenueGrowth;

    @FXML private BarChart<String, Number> bidBarChart;
    @FXML private PieChart categoryPieChart;

    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userRoleFilter;
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colUserId;
    @FXML private TableColumn<UserRow, String> colUserName;
    @FXML private TableColumn<UserRow, String> colUserEmail;
    @FXML private TableColumn<UserRow, String> colUserRole;
    @FXML private TableColumn<UserRow, String> colUserBalance;
    @FXML private TableColumn<UserRow, Void> colUserActions;
    @FXML private Label userPageInfo;
    @FXML private Label userCountLabel;

    @FXML private VBox createAdminBox;
    @FXML private TextField newAdminUsernameField;
    @FXML private TextField newAdminEmailField;
    @FXML private TextField newAdminFullNameField;
    @FXML private PasswordField newAdminPasswordField;

    @FXML private TableView<PendingItemRow> pendingItemTable;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemId;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemName;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemSeller;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemType;
    @FXML private TableColumn<PendingItemRow, String> colPendingItemPrice;
    @FXML private TableColumn<PendingItemRow, Void> colPendingItemAction;

    @FXML private TextField aucSearchField;
    @FXML private ComboBox<String> aucStatusFilter;
    @FXML private TableView<AuctionRow> adminAucTable;
    @FXML private TableColumn<AuctionRow, String> colAdminAucId;
    @FXML private TableColumn<AuctionRow, String> colAdminAucTitle;
    @FXML private TableColumn<AuctionRow, String> colAdminAucStatus;
    @FXML private TableColumn<AuctionRow, String> colAdminAucPrice;
    @FXML private TableColumn<AuctionRow, Void> colAdminAucAction;
    @FXML private Label aucPageInfo;
    @FXML private Label aucCountLabel;

    @FXML private DatePicker auditDateFilter;
    @FXML private TableView<AuditRow> auditTable;
    @FXML private TableColumn<AuditRow, String> colAuditTime;
    @FXML private TableColumn<AuditRow, String> colAuditActor;
    @FXML private TableColumn<AuditRow, String> colAuditAction;
    @FXML private TableColumn<AuditRow, String> colAuditTarget;
    @FXML private TableColumn<AuditRow, String> colAuditDetail;

    @FXML private BarChart<String, Number> revenueChart;
    @FXML private LineChart<String, Number> usersGrowthChart;
    @FXML private PieChart rolesPieChart;

    private final AdminViewState state = new AdminViewState();
    private AdminNetworkGateway network;
    private AdminAuditLog auditLog;
    private AdminCharts charts;
    private AdminFilters filters;
    private AdminResponseHandler responseHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        network = new AdminNetworkGateway(connectionLabel, state);
        auditLog = new AdminAuditLog(state);
        charts = new AdminCharts(state,
                statTotalUsers, statUsersGrowth, statActiveSessions, statSessionsInfo,
                statTotalBids, statBidsToday, statTotalRevenue, statRevenueGrowth,
                bidBarChart, categoryPieChart, rolesPieChart, revenueChart, usersGrowthChart);
        filters = new AdminFilters(state,
                userSearchField, userRoleFilter, userPageInfo, userCountLabel,
                aucSearchField, aucStatusFilter, aucPageInfo, aucCountLabel,
                auditDateFilter, auditTable);
        responseHandler = new AdminResponseHandler(
                state, auditLog, charts, filters, network,
                this::requestAllUsers,
                this::requestAllAuctions,
                this::requestPendingItems,
                (auctionId, title, rows) -> AdminDialogs.showAuctionBidHistoryDialog(
                        auctionId, title, rows, AdminView.class));

        startClock();
        setupTables();
        hideOutOfScopeAdminFeatures();
        setupComboBoxes();
        setupNetworkListeners();
        network.connectOnce();
        state.adminLoggedIn = "ADMIN".equals(UserSession.getInstance().getRole());
        configureAdminLevelVisibility();
        showUsers();
    }

    private void setupTables() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balanceFormatted"));
        colUserActions.setCellFactory(AdminTableCellFactory.banButtonColumn(new AdminTableCellFactory.UserActionHandler() {
            @Override
            public void onBan(UserRow user) {
                confirmBanUser(user);
            }

            @Override
            public void onUnban(UserRow user) {
                confirmUnbanUser(user);
            }
        }));
        userTable.setItems(state.filteredUsers);

        colPendingItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPendingItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPendingItemSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colPendingItemType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPendingItemPrice.setCellValueFactory(new PropertyValueFactory<>("startingPriceFormatted"));
        colPendingItemAction.setCellFactory(AdminTableCellFactory.approvalActionColumn(new AdminTableCellFactory.PendingItemActionHandler() {
            @Override
            public void onApprove(PendingItemRow item) {
                confirmApproveItem(item);
            }

            @Override
            public void onReject(PendingItemRow item) {
                confirmRejectItem(item);
            }
        }));
        pendingItemTable.setItems(state.pendingItems);

        colAdminAucId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminAucTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAdminAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminAucPrice.setCellValueFactory(new PropertyValueFactory<>("priceFormatted"));
        colAdminAucAction.setCellFactory(AdminTableCellFactory.auctionActionColumn(new AdminTableCellFactory.AuctionActionHandler() {
            @Override
            public void onViewItem(AuctionRow auction) {
                AdminDialogs.showAuctionItemDetails(auction, AdminView.class);
            }

            @Override
            public void onViewHistory(AuctionRow auction) {
                requestAuctionBidHistory(auction);
            }

            @Override
            public void onForceClose(AuctionRow auction) {
                confirmForceClose(auction);
            }
        }));
        adminAucTable.setItems(state.filteredAuctions);

        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAuditActor.setCellValueFactory(new PropertyValueFactory<>("actor"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colAuditDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));
        auditTable.setItems(state.allAuditLogs);
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

    private void setupComboBoxes() {
        userRoleFilter.setItems(FXCollections.observableArrayList("Tất cả", "USER", "SELLER", "ADMIN"));
        userRoleFilter.getSelectionModel().selectFirst();
        aucStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "RUNNING", "OPEN", "FINISHED", "FAILED", "PAID", "CANCELED"));
        aucStatusFilter.getSelectionModel().selectFirst();
    }

    private void setupNetworkListeners() {
        network.setupMessageHandler(response ->
                Platform.runLater(() -> responseHandler.handle(response)));
    }

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

    @FXML
    public void showDashboard() {
        showSection(dashboardSection);
        highlightNav(navDashboard);
        if (state.adminLoggedIn) {
            requestDashboardData();
        }
    }

    @FXML
    public void showUsers() {
        showSection(usersSection);
        highlightNav(navUsers);
        if (state.adminLoggedIn) {
            requestAllUsers();
        } else {
            warnNotLoggedIn();
        }
    }

    @FXML
    public void showApprovals() {
        showSection(approvalsSection);
        highlightNav(navApprovals);
        if (state.adminLoggedIn) {
            requestPendingItems();
        } else {
            warnNotLoggedIn();
        }
    }

    @FXML
    public void showAuctions() {
        showSection(auctionsAdminSection);
        highlightNav(navAuctions);
        if (state.adminLoggedIn) {
            requestAllAuctions();
        } else {
            warnNotLoggedIn();
        }
    }

    @FXML
    public void showAuditLog() {
        showSection(auditLogSection);
        highlightNav(navAuditLog);
        auditTable.setItems(state.allAuditLogs);
    }

    @FXML
    public void showStats() {
        showSection(statsSection);
        highlightNav(navStats);
        if (state.adminLoggedIn) {
            requestStatsData();
        } else {
            warnNotLoggedIn();
        }
    }

    @FXML
    private void handleCreateAdminLevel1() {
        if (UserSession.getInstance().getAdminLevel() < 2) {
            AdminUiHelper.showAlert(Alert.AlertType.WARNING, "Khong du quyen",
                    "Chi admin level 2 moi duoc tao admin level 1.");
            return;
        }

        String username = newAdminUsernameField.getText().trim();
        String email = newAdminEmailField.getText().trim();
        String fullName = newAdminFullNameField.getText().trim();
        String password = newAdminPasswordField.getText().trim();
        if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            AdminUiHelper.showAlert(Alert.AlertType.WARNING, "Thieu thong tin",
                    "Vui long nhap day du thong tin admin level 1.");
            return;
        }

        network.sendCreateAdminLevel1(username, email, fullName, password);
        auditLog.append("CREATE_ADMIN_LEVEL1", "admin", username, "Dang cho...");
        newAdminUsernameField.clear();
        newAdminEmailField.clear();
        newAdminFullNameField.clear();
        newAdminPasswordField.clear();
    }

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                state.adminLoggedIn = false;
                Platform.exit();
            }
        });
    }

    @FXML
    public void handleUserSearch() {
        filters.applyUserFilter();
    }

    @FXML
    public void clearUserSearch() {
        filters.clearUserSearch();
    }

    @FXML
    public void handleAucSearch() {
        filters.applyAucFilter();
    }

    @FXML
    public void clearAucSearch() {
        filters.clearAucSearch();
    }

    @FXML
    public void filterAuditLog() {
        filters.filterAuditLog();
    }

    @FXML
    public void resetAuditFilter() {
        filters.resetAuditFilter();
    }

    @FXML
    public void userPrevPage() {
        filters.userPrevPage();
    }

    @FXML
    public void userNextPage() {
        filters.userNextPage();
    }

    @FXML
    public void aucPrevPage() {
        filters.aucPrevPage();
    }

    @FXML
    public void aucNextPage() {
        filters.aucNextPage();
    }

    private void requestDashboardData() {
        requestAllUsers();
    }

    private void requestAllUsers() {
        network.sendAction("GET_ALL_USERS");
    }

    private void requestAllAuctions() {
        network.sendAction("GET_AUCTIONS");
    }

    private void requestPendingItems() {
        network.sendAction("GET_PENDING_ITEMS");
    }

    private void requestStatsData() {
        network.sendAction("GET_ALL_USERS");
        network.sendAction("GET_AUCTIONS");
    }

    private void requestAuctionBidHistory(AuctionRow auction) {
        if (auction == null || !network.checkConnected()) {
            return;
        }
        state.pendingHistoryAuctionId = auction.getId();
        state.pendingHistoryAuctionTitle = auction.getTitle();
        network.sendAuctionBidHistoryRequest(auction.getId());
    }

    private void confirmBanUser(UserRow user) {
        if (user == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận khóa tài khoản");
        confirm.setHeaderText("Khóa: " + user.getUsername());
        confirm.setContentText("ID: " + user.getId() + "  |  Role: " + user.getRole()
                + "\n\nHành động này sẽ khóa tài khoản ngay lập tức.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                sendBanUser(user.getId(), user.getUsername());
            }
        });
    }

    private void confirmUnbanUser(UserRow user) {
        if (user == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mở khóa tài khoản");
        confirm.setHeaderText("Mở khóa: " + user.getUsername());
        confirm.setContentText("ID: " + user.getId() + "  |  Role: " + user.getRole()
                + "\n\nHành động này sẽ cho phép tài khoản đăng nhập lại.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                sendUnbanUser(user.getId(), user.getUsername());
            }
        });
    }

    private void confirmForceClose(AuctionRow auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đóng phiên");
        confirm.setHeaderText("Ép đóng: " + auction.getTitle());
        confirm.setContentText("ID phiên: " + auction.getId()
                + "\nGiá hiện tại: " + auction.getPriceFormatted()
                + "\n\nHành động này kết thúc phiên ngay lập tức!");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                sendForceClose(auction.getId(), auction.getTitle());
            }
        });
    }

    private void confirmApproveItem(PendingItemRow item) {
        if (item == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Duyet san pham");
        confirm.setHeaderText("Duyet: " + item.getName());
        confirm.setContentText("ID: " + item.getId()
                + "\nNguoi ban: " + item.getSeller()
                + "\n\nSan pham se duoc phep mo phien dau gia.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                sendApproveItem(item.getId(), item.getName());
            }
        });
    }

    private void confirmRejectItem(PendingItemRow item) {
        if (item == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog("San pham chua dat yeu cau.");
        dialog.setTitle("Tu choi san pham");
        dialog.setHeaderText("Tu choi: " + item.getName());
        dialog.setContentText("Ly do:");
        dialog.showAndWait().ifPresent(reason ->
                sendRejectItem(item.getId(), item.getName(), reason));
    }

    private void sendBanUser(String userId, String username) {
        network.sendBanUser(userId);
        auditLog.append("BAN_USER", "admin", username + " (ID: " + userId + ")", "Đang chờ...");
    }

    private void sendUnbanUser(String userId, String username) {
        network.sendUnbanUser(userId);
        auditLog.append("UNBAN_USER", "admin", username + " (ID: " + userId + ")", "Đang chờ...");
    }

    private void sendForceClose(String auctionId, String title) {
        network.sendForceClose(auctionId);
        auditLog.append("FORCE_CLOSE", "admin", title + " (ID: " + auctionId + ")", "Đang chờ...");
    }

    private void sendApproveItem(String itemId, String name) {
        network.sendApproveItem(itemId);
        auditLog.append("APPROVE_ITEM", "admin", name + " (ID: " + itemId + ")", "Dang cho...");
    }

    private void sendRejectItem(String itemId, String name, String reason) {
        network.sendRejectItem(itemId, reason);
        auditLog.append("REJECT_ITEM", "admin", name + " (ID: " + itemId + ")", "Dang cho...");
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
            btn.getStyleClass().remove("nav-btn-active");
            if (!btn.getStyleClass().contains("nav-btn")) {
                btn.getStyleClass().add("nav-btn");
            }
        }
        active.getStyleClass().add("nav-btn-active");
    }

    private void warnNotLoggedIn() {
        AdminUiHelper.showAlert(Alert.AlertType.WARNING, "Chưa xác thực",
                "Đang chờ đăng nhập admin...\nVui lòng thử lại sau vài giây.");
    }

    private void configureAdminLevelVisibility() {
        boolean canCreateAdmin = state.adminLoggedIn && UserSession.getInstance().getAdminLevel() >= 2;
        if (createAdminBox != null) {
            createAdminBox.setVisible(canCreateAdmin);
            createAdminBox.setManaged(canCreateAdmin);
        }
    }
}
