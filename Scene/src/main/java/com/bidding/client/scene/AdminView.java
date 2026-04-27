package com.bidding.client.scene;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminView implements Initializable {

    // â”€â”€ Navbar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label clockLabel;

    // â”€â”€ Nav buttons â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Button navDashboard, navUsers, navAuctions, navAuditLog, navStats;

    // â”€â”€ Sections â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private VBox dashboardSection, usersSection, auctionsAdminSection, auditLogSection, statsSection;

    // â”€â”€ Dashboard stats â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label statTotalUsers, statUsersGrowth, statActiveSessions, statSessionsInfo;
    @FXML private Label statTotalBids, statBidsToday, statTotalRevenue, statRevenueGrowth;

    // â”€â”€ Dashboard charts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private BarChart<String, Number> bidBarChart;
    @FXML private PieChart categoryPieChart;

    // â”€â”€ User management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userRoleFilter;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUserId, colUserName, colUserEmail, colUserRole, colUserStatus, colUserCreatedAt, colUserActions;
    @FXML private Label userPageInfo;

    // â”€â”€ Auction management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TextField aucSearchField;
    @FXML private ComboBox<String> aucStatusFilter;
    @FXML private TableView<Auction> adminAucTable;
    @FXML private TableColumn<Auction, String> colAdminAucId, colAdminAucTitle, colAdminAucSeller, colAdminAucStatus, colAdminAucPrice, colAdminAucBids, colAdminAucEndTime, colAdminAucAction;
    @FXML private Label aucPageInfo;

    // â”€â”€ Audit Log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private DatePicker auditDateFilter;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> colAuditTime, colAuditActor, colAuditAction, colAuditTarget, colAuditDetail;

    // â”€â”€ Stats charts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private LineChart<String, Number> usersGrowthChart;
    @FXML private PieChart rolesPieChart;

    private Timeline clockTimeline;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startClock();
        setupUserTable();
        setupAucTable();
        setupAuditTable();
        showDashboard(); // Máº·c Ä‘á»‹nh má»Ÿ Dashboard
    }

    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> clockLabel.setText(LocalTime.now().format(fmt))));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // â”€â”€ Section Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML public void showDashboard() { switchSection(dashboardSection); setNavActive(navDashboard); loadDashboardStats(); loadDashboardCharts(); }
    @FXML public void showUsers() { switchSection(usersSection); setNavActive(navUsers); loadUsers(); }
    @FXML public void showAuctions() { switchSection(auctionsAdminSection); setNavActive(navAuctions); loadAuctions(); }
    @FXML public void showAuditLog() { switchSection(auditLogSection); setNavActive(navAuditLog); loadAuditLog(); }
    @FXML public void showStats() { switchSection(statsSection); setNavActive(navStats); loadStatsCharts(); }

    private void switchSection(VBox show) {
        for (VBox s : List.of(dashboardSection, usersSection, auctionsAdminSection, auditLogSection, statsSection)) {
            if (s != null) { s.setVisible(s == show); s.setManaged(s == show); }
        }
    }

    private void setNavActive(Button active) {
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 10 15;";
        String activeStyle = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 10 15; -fx-background-radius: 8px;";
        for (Button b : List.of(navDashboard, navUsers, navAuctions, navAuditLog, navStats)) { if (b != null) b.setStyle(defaultStyle); }
        if (active != null) active.setStyle(activeStyle);
    }

    // â”€â”€ Dashboard Stats (DUMMY DATA) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void loadDashboardStats() {
        statTotalUsers.setText("1,250");
        statUsersGrowth.setText("+12 hÃ´m nay");
        statActiveSessions.setText("45");
        statSessionsInfo.setText("3 phiÃªn chá»");
        statTotalBids.setText("15,420");
        statBidsToday.setText("+320 hÃ´m nay");
        statTotalRevenue.setText("450,000,000 Ä‘");
        statRevenueGrowth.setText("+15.5% thÃ¡ng nÃ y");
    }

    private void loadDashboardCharts() {
        if(bidBarChart.getData().isEmpty()) {
            XYChart.Series<String, Number> bidSeries = new XYChart.Series<>();
            bidSeries.setName("LÆ°á»£t bid");
            bidSeries.getData().add(new XYChart.Data<>("T2", 120));
            bidSeries.getData().add(new XYChart.Data<>("T3", 250));
            bidSeries.getData().add(new XYChart.Data<>("T4", 180));
            bidSeries.getData().add(new XYChart.Data<>("T5", 300));
            bidSeries.getData().add(new XYChart.Data<>("T6", 450));
            bidSeries.getData().add(new XYChart.Data<>("T7", 600));
            bidSeries.getData().add(new XYChart.Data<>("CN", 550));
            bidBarChart.getData().add(bidSeries);
        }

        if(categoryPieChart.getData().isEmpty()) {
            categoryPieChart.getData().add(new PieChart.Data("Äá»“ Ä‘iá»‡n tá»­", 40));
            categoryPieChart.getData().add(new PieChart.Data("Äá»“ cá»•", 25));
            categoryPieChart.getData().add(new PieChart.Data("Trang sá»©c", 20));
            categoryPieChart.getData().add(new PieChart.Data("KhÃ¡c", 15));
        }
    }

    // â”€â”€ User Management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupUserTable() {
        colUserId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id));
        colUserName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fullName));
        colUserEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email));
        colUserRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().role));
        colUserStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isBanned ? "ðŸ”’ Bá»‹ khÃ³a" : "âœ… Hoáº¡t Ä‘á»™ng"));
        colUserCreatedAt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().createdAt.format(DATETIME_FMT)));

        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button lockBtn = new Button();
            private final Button viewBtn = new Button("ðŸ‘ Chi tiáº¿t");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(10, viewBtn, lockBtn);
            {
                viewBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
                lockBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); }
                else {
                    User u = getTableView().getItems().get(getIndex());
                    lockBtn.setText(u.isBanned ? "ðŸ”“ Má»Ÿ khÃ³a" : "ðŸ”’ KhÃ³a");
                    setGraphic(box);
                }
            }
        });
    }

    private void loadUsers() {
        List<User> dummyUsers = List.of(
                new User("U01", "Nguyá»…n VÄƒn A", "vana@gmail.com", "BIDDER", false, LocalDateTime.now().minusDays(10)),
                new User("U02", "Tráº§n Thá»‹ B", "thib@gmail.com", "SELLER", false, LocalDateTime.now().minusDays(5)),
                new User("U03", "LÃª VÄƒn C", "vanc@gmail.com", "BIDDER", true, LocalDateTime.now().minusDays(2))
        );
        userTable.setItems(FXCollections.observableArrayList(dummyUsers));
        userPageInfo.setText("Trang 1 / 1");
    }

    @FXML private void handleUserSearch() { loadUsers(); }
    @FXML private void userPrevPage() { }
    @FXML private void userNextPage() { }

    // â”€â”€ Auction Management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupAucTable() {
        colAdminAucId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id));
        colAdminAucTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAdminAucSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sellerName));
        colAdminAucStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colAdminAucPrice.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().currentPrice)));
        colAdminAucBids.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().bidCount)));
        colAdminAucEndTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().endTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))));

        colAdminAucAction.setCellFactory(col -> new TableCell<>() {
            private final Button closeBtn = new Button("ðŸš¨ ÄÃ³ng kháº©n");
            {
                closeBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : closeBtn);
            }
        });
    }

    private void loadAuctions() {
        List<Auction> dummyAucs = List.of(
                new Auction("A01", "MacBook Pro M3", "Shop ABC", "Äang cháº¡y", 45000000, 12, LocalDateTime.now().plusHours(2)),
                new Auction("A02", "Äá»“ng há»“ Rolex", "Tráº§n Thá»‹ B", "Sáº¯p má»Ÿ", 150000000, 0, LocalDateTime.now().plusDays(1))
        );
        adminAucTable.setItems(FXCollections.observableArrayList(dummyAucs));
        aucPageInfo.setText("Trang 1 / 1");
    }

    @FXML private void handleAucSearch() { loadAuctions(); }
    @FXML private void aucPrevPage() { }
    @FXML private void aucNextPage() { }

    // â”€â”€ Audit Log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupAuditTable() {
        colAuditTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().timestamp.format(DATETIME_FMT)));
        colAuditActor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().actorName));
        colAuditAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().action));
        colAuditTarget.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().target));
        colAuditDetail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().detail));
    }

    private void loadAuditLog() {
        List<AuditLog> dummyLogs = List.of(
                new AuditLog(LocalDateTime.now().minusMinutes(5), "Admin_01", "KHÃ“A_USER", "User#U03", "Vi pháº¡m ná»™i quy Ä‘áº¥u giÃ¡"),
                new AuditLog(LocalDateTime.now().minusHours(2), "Há»‡ thá»‘ng", "Káº¾T_THÃšC_PHIÃŠN", "Auction#A09", "PhiÃªn káº¿t thÃºc thÃ nh cÃ´ng")
        );
        auditTable.setItems(FXCollections.observableArrayList(dummyLogs));
    }

    @FXML private void filterAuditLog() { loadAuditLog(); }
    @FXML private void resetAuditFilter() { auditDateFilter.setValue(null); loadAuditLog(); }

    // â”€â”€ Stats Charts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void loadStatsCharts() {
        if(revenueChart.getData().isEmpty()) {
            XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
            revSeries.setName("Doanh thu (triá»‡u Ä‘)");
            revSeries.getData().add(new XYChart.Data<>("T1", 150));
            revSeries.getData().add(new XYChart.Data<>("T2", 200));
            revSeries.getData().add(new XYChart.Data<>("T3", 180));
            revenueChart.getData().add(revSeries);
        }
        if(rolesPieChart.getData().isEmpty()) {
            rolesPieChart.getData().add(new PieChart.Data("Bidder", 70));
            rolesPieChart.getData().add(new PieChart.Data("Seller", 25));
            rolesPieChart.getData().add(new PieChart.Data("Admin", 5));
        }
    }

    // â”€â”€ Logout â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML
    private void handleLogout(ActionEvent event) {
        if (clockTimeline != null) clockTimeline.stop();
        System.out.println("ÄÃ£ Ä‘Äƒng xuáº¥t! Chuyá»ƒn vá» mÃ n hÃ¬nh Ä‘Äƒng nháº­p...");
        // Viáº¿t code loadPage("Login.fxml") cá»§a báº¡n á»Ÿ Ä‘Ã¢y
    }

    private String formatMoney(double amount) { return String.format("%,.0f Ä‘", amount); }

    // =====================================================================
    // INNER CLASSES Äá»‚ TRÃNH Lá»–I THIáº¾U MODEL
    // =====================================================================
    public static class User {
        String id, fullName, email, role; boolean isBanned; LocalDateTime createdAt;
        public User(String id, String fullName, String email, String role, boolean isBanned, LocalDateTime createdAt) {
            this.id = id; this.fullName = fullName; this.email = email; this.role = role; this.isBanned = isBanned; this.createdAt = createdAt;
        }
    }
    public static class Auction {
        String id, itemName, sellerName, status; double currentPrice; int bidCount; LocalDateTime endTime;
        public Auction(String id, String itemName, String sellerName, String status, double currentPrice, int bidCount, LocalDateTime endTime) {
            this.id = id; this.itemName = itemName; this.sellerName = sellerName; this.status = status; this.currentPrice = currentPrice; this.bidCount = bidCount; this.endTime = endTime;
        }
    }
    public static class AuditLog {
        LocalDateTime timestamp; String actorName, action, target, detail;
        public AuditLog(LocalDateTime timestamp, String actorName, String action, String target, String detail) {
            this.timestamp = timestamp; this.actorName = actorName; this.action = action; this.target = target; this.detail = detail;
        }
    }
}