package com.bidding.client.scene;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SceneAdmin {

    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label welcomeLabel;
    @FXML private Label headerSummaryLabel;
    @FXML private Label dbSummaryLabel;

    @FXML private Label totalUsersLabel;
    @FXML private Label runningAuctionsLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label revenueLabel;
    @FXML private Label dashboardNoticeLabel;
    @FXML private BarChart<String, Number> bidsChart;
    @FXML private PieChart categoryChart;

    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userRoleFilter;
    @FXML private TableView<SceneAdminData.UserRow> userTable;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userNameCol;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userEmailCol;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userRoleCol;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userStatusCol;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userCreatedCol;
    @FXML private TableColumn<SceneAdminData.UserRow, String> userActionCol;
    @FXML private Label userPageLabel;
    @FXML private Label userSectionNoticeLabel;
    @FXML private Label userDetailNameLabel;
    @FXML private Label userDetailRoleLabel;
    @FXML private Label userDetailStatusLabel;
    @FXML private Label userDetailCreatedLabel;
    @FXML private Label userDetailDbLabel;
    @FXML private TableView<SceneAdminData.UserLoginRecord> userLoginTable;
    @FXML private TableColumn<SceneAdminData.UserLoginRecord, String> userLoginTimeCol;
    @FXML private TableColumn<SceneAdminData.UserLoginRecord, String> userLoginIpCol;
    @FXML private TableColumn<SceneAdminData.UserLoginRecord, String> userLoginDeviceCol;
    @FXML private TableView<SceneAdminData.UserActivityRecord> userActivityTable;
    @FXML private TableColumn<SceneAdminData.UserActivityRecord, String> userActivityTimeCol;
    @FXML private TableColumn<SceneAdminData.UserActivityRecord, String> userActivityTypeCol;
    @FXML private TableColumn<SceneAdminData.UserActivityRecord, String> userActivityItemCol;

    @FXML private TextField auctionSearchField;
    @FXML private ComboBox<String> auctionStatusFilter;
    @FXML private TableView<SceneAdminData.AuctionRow> auctionTable;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionIdCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionTitleCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionSellerCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionStatusCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionPriceCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionBidCountCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionEndTimeCol;
    @FXML private TableColumn<SceneAdminData.AuctionRow, String> auctionActionCol;
    @FXML private Label auctionPageLabel;
    @FXML private Label auctionSectionNoticeLabel;
    @FXML private Label auctionDetailTitleLabel;
    @FXML private Label auctionDetailSellerLabel;
    @FXML private Label auctionDetailStatusLabel;
    @FXML private Label auctionDetailPriceLabel;
    @FXML private TextArea forceCloseReasonArea;
    @FXML private Label forceCloseStatusLabel;

    @FXML private DatePicker auditDateFilter;
    @FXML private TableView<SceneAdminData.AuditRow> auditTable;
    @FXML private TableColumn<SceneAdminData.AuditRow, String> auditTimeCol;
    @FXML private TableColumn<SceneAdminData.AuditRow, String> auditActorCol;
    @FXML private TableColumn<SceneAdminData.AuditRow, String> auditActionCol;
    @FXML private TableColumn<SceneAdminData.AuditRow, String> auditTargetCol;
    @FXML private TableColumn<SceneAdminData.AuditRow, String> auditDetailCol;
    @FXML private Label auditNoticeLabel;

    private final ObservableList<SceneAdminData.UserRow> allUsers = FXCollections.observableArrayList();
    private final ObservableList<SceneAdminData.AuctionRow> allAuctions = FXCollections.observableArrayList();
    private final ObservableList<SceneAdminData.AuditRow> auditRows = FXCollections.observableArrayList();

    private int userPage = 0;
    private int auctionPage = 0;

    @FXML
    public void initialize() {
        seedMockData();
        configureFilters();
        configureTables();
        configureCharts();
        bindSelection();
        refreshDashboard();
        applyUserFilters();
        applyAuctionFilters();
        refreshAuditTable();
        dbSummaryLabel.setText("Trang admin dang dung du lieu mau. Can ket noi database/service de dong bo du lieu thuc va ghi nhat ky he thong.");
    }

    private void seedMockData() {
        welcomeLabel.setText("Admin Dashboard");
        headerSummaryLabel.setText("Tong hop dashboard, quan ly user, phien dau gia, force close va audit log trong mot man hinh.");

        allUsers.setAll(
                new SceneAdminData.UserRow("U001", "Nguyen Van An", "an.admin@bidviet.vn", "ADMIN", "Hoat dong", false, LocalDateTime.now().minusMonths(5),
                        List.of(
                                new SceneAdminData.UserLoginRecord("11/04/2026 08:30", "192.168.1.10", "Chrome / Windows"),
                                new SceneAdminData.UserLoginRecord("10/04/2026 21:05", "192.168.1.10", "Chrome / Windows")
                        ),
                        List.of(
                                new SceneAdminData.UserActivityRecord("11/04/2026 09:05", "Quan tri", "Ban user U021"),
                                new SceneAdminData.UserActivityRecord("11/04/2026 09:12", "Audit", "Xem audit log")
                        )
                ),
                new SceneAdminData.UserRow("U014", "Tran Thi Bich", "bich.bidder@gmail.com", "BIDDER", "Hoat dong", false, LocalDateTime.now().minusDays(20),
                        List.of(
                                new SceneAdminData.UserLoginRecord("11/04/2026 10:10", "10.10.0.2", "Android App"),
                                new SceneAdminData.UserLoginRecord("09/04/2026 18:22", "10.10.0.2", "Android App")
                        ),
                        List.of(
                                new SceneAdminData.UserActivityRecord("11/04/2026 10:12", "Bid", "iPhone 15 Pro Max"),
                                new SceneAdminData.UserActivityRecord("11/04/2026 10:14", "Watchlist", "Rolex Submariner")
                        )
                ),
                new SceneAdminData.UserRow("U021", "Le Quoc Huy", "huy.seller@gmail.com", "SELLER", "Bi khoa", true, LocalDateTime.now().minusDays(15),
                        List.of(
                                new SceneAdminData.UserLoginRecord("10/04/2026 14:05", "172.20.10.5", "Safari / Mac"),
                                new SceneAdminData.UserLoginRecord("08/04/2026 09:45", "172.20.10.5", "Safari / Mac")
                        ),
                        List.of(
                                new SceneAdminData.UserActivityRecord("10/04/2026 15:00", "Dang san pham", "MacBook Pro M3"),
                                new SceneAdminData.UserActivityRecord("10/04/2026 15:18", "Canh bao", "San pham bi bao cao")
                        )
                ),
                new SceneAdminData.UserRow("U033", "Pham Minh Chau", "chau.bidder@gmail.com", "BIDDER", "Hoat dong", false, LocalDateTime.now().minusDays(7),
                        List.of(new SceneAdminData.UserLoginRecord("11/04/2026 12:20", "192.168.0.8", "Firefox / Linux")),
                        List.of(new SceneAdminData.UserActivityRecord("11/04/2026 12:28", "Nap tien", "500.000 VND"))
                ),
                new SceneAdminData.UserRow("U040", "Do Hai Dang", "dang.seller@gmail.com", "SELLER", "Cho xac minh", false, LocalDateTime.now().minusDays(3),
                        List.of(new SceneAdminData.UserLoginRecord("11/04/2026 07:55", "113.161.22.1", "Edge / Windows")),
                        List.of(new SceneAdminData.UserActivityRecord("11/04/2026 08:10", "Dang ky seller", "Cho duyet ho so"))
                )
        );

        allAuctions.setAll(
                new SceneAdminData.AuctionRow("A001", "iPhone 15 Pro Max 256GB", "Tran Thi Bich", "Dang chay", 21500000, 12, LocalDateTime.now().plusHours(4), false),
                new SceneAdminData.AuctionRow("A009", "Dong ho Rolex Submariner", "Le Quoc Huy", "Sap mo", 52000000, 0, LocalDateTime.now().plusDays(1), false),
                new SceneAdminData.AuctionRow("A013", "MacBook Pro M3", "Do Hai Dang", "Dang chay", 45000000, 7, LocalDateTime.now().plusHours(1), false),
                new SceneAdminData.AuctionRow("A014", "Tranh Son Dau Ha Noi", "Gallery Ha Noi", "Da dong khan cap", 5800000, 4, LocalDateTime.now().minusHours(1), true),
                new SceneAdminData.AuctionRow("A021", "Binh gom Minh Long", "Nguyen Van Son", "Da ket thuc", 3200000, 9, LocalDateTime.now().minusDays(1), false)
        );

        auditRows.setAll(
                new SceneAdminData.AuditRow(LocalDateTime.now().minusMinutes(30), "adminmanh", "FORCE_CLOSE_AUCTION", "Auction#A014", "Dong phien do co bao cao gian lan"),
                new SceneAdminData.AuditRow(LocalDateTime.now().minusHours(1), "adminmanh", "BAN_USER", "User#U021", "Khoa tai khoan seller vi vi pham chinh sach"),
                new SceneAdminData.AuditRow(LocalDateTime.now().minusHours(3), "system", "SYNC_PENDING", "AdminDashboard", "Can ket noi database de cap nhat dashboard thoi gian thuc")
        );
    }

    private void configureFilters() {
        userRoleFilter.setItems(FXCollections.observableArrayList("Tat ca", "ADMIN", "SELLER", "BIDDER"));
        userRoleFilter.getSelectionModel().selectFirst();
        auctionStatusFilter.setItems(FXCollections.observableArrayList("Tat ca", "Dang chay", "Sap mo", "Da ket thuc", "Da dong khan cap"));
        auctionStatusFilter.getSelectionModel().selectFirst();

        userSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            userPage = 0;
            applyUserFilters();
        });
        userRoleFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            userPage = 0;
            applyUserFilters();
        });
        auctionSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            auctionPage = 0;
            applyAuctionFilters();
        });
        auctionStatusFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            auctionPage = 0;
            applyAuctionFilters();
        });
    }

    private void configureTables() {
        userNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fullName()));
        userEmailCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().email()));
        userRoleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().role()));
        userStatusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));
        userCreatedCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().createdAt().format(DATE_TIME_FMT)));
        userActionCol.setCellValueFactory(cell -> new SimpleStringProperty(""));
        userActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button actionButton = new Button();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                SceneAdminData.UserRow row = getTableView().getItems().get(getIndex());
                actionButton.setText(row.banned() ? "Mo khoa" : "Khoa");
                actionButton.setOnAction(event -> toggleUserStatus(row));
                actionButton.setStyle("-fx-background-color: " + (row.banned() ? "#dcfce7" : "#fee2e2") + "; -fx-font-weight: bold;");
                setGraphic(actionButton);
            }
        });

        userLoginTimeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().time()));
        userLoginIpCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ip()));
        userLoginDeviceCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().device()));
        userActivityTimeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().time()));
        userActivityTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type()));
        userActivityItemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().item()));

        auctionIdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().id()));
        auctionTitleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
        auctionSellerCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().seller()));
        auctionStatusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status()));
        auctionPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().currentPrice())));
        auctionBidCountCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().bidCount())));
        auctionEndTimeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().endTime().format(DATE_TIME_FMT)));
        auctionActionCol.setCellValueFactory(cell -> new SimpleStringProperty(""));
        auctionActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button actionButton = new Button("Dong khan cap");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                SceneAdminData.AuctionRow row = getTableView().getItems().get(getIndex());
                actionButton.setDisable(row.closedEmergency());
                actionButton.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                actionButton.setOnAction(event -> forceCloseAuction(row));
                setGraphic(actionButton);
            }
        });

        auditTimeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().time().format(DATE_TIME_FMT)));
        auditActorCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().actor()));
        auditActionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().action()));
        auditTargetCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().target()));
        auditDetailCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().detail()));

        userTable.setPlaceholder(new Label("Can ket noi database de dong bo bang nguoi dung theo thoi gian thuc."));
        auctionTable.setPlaceholder(new Label("Can ket noi database de dong bo danh sach phien dau gia."));
        auditTable.setPlaceholder(new Label("Can ket noi database de doc audit log thuc te."));
    }

    private void configureCharts() {
        XYChart.Series<String, Number> bidsSeries = new XYChart.Series<>();
        bidsSeries.setName("So bid");
        bidsSeries.getData().add(new XYChart.Data<>("T2", 120));
        bidsSeries.getData().add(new XYChart.Data<>("T3", 185));
        bidsSeries.getData().add(new XYChart.Data<>("T4", 240));
        bidsSeries.getData().add(new XYChart.Data<>("T5", 210));
        bidsSeries.getData().add(new XYChart.Data<>("T6", 320));
        bidsSeries.getData().add(new XYChart.Data<>("T7", 410));
        bidsSeries.getData().add(new XYChart.Data<>("CN", 360));
        bidsChart.getData().setAll(bidsSeries);

        categoryChart.getData().setAll(
                new PieChart.Data("Dien tu", 38),
                new PieChart.Data("Trang suc", 21),
                new PieChart.Data("My thuat", 17),
                new PieChart.Data("Do co", 12),
                new PieChart.Data("Khac", 12)
        );
    }

    private void bindSelection() {
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateUserDetail(newValue));
        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateAuctionDetail(newValue));
    }

    private void refreshDashboard() {
        long runningAuctions = allAuctions.stream().filter(row -> "Dang chay".equals(row.status())).count();
        totalUsersLabel.setText(String.valueOf(allUsers.size()));
        runningAuctionsLabel.setText(String.valueOf(runningAuctions));
        totalTransactionsLabel.setText("15420");
        revenueLabel.setText("450.000.000 VND");
        dashboardNoticeLabel.setText("Dashboard hien thi du lieu mau. Khi ket noi database, 4 card nay can bind vao truy van tong hop real-time.");
    }

    private void applyUserFilters() {
        String keyword = normalize(userSearchField.getText());
        String role = userRoleFilter.getValue();
        List<SceneAdminData.UserRow> filtered = allUsers.stream()
                .filter(user -> "Tat ca".equals(role) || user.role().equals(role))
                .filter(user -> keyword.isEmpty() || normalize(user.fullName()).contains(keyword) || normalize(user.email()).contains(keyword))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (userPage >= totalPages) {
            userPage = totalPages - 1;
        }

        int fromIndex = Math.max(0, userPage * PAGE_SIZE);
        int toIndex = Math.min(filtered.size(), fromIndex + PAGE_SIZE);
        userTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
        userPageLabel.setText("Trang " + (userPage + 1) + " / " + totalPages);
        userSectionNoticeLabel.setText("Tim kiem va loc dang chay tren du lieu mau trong bo nho. Can ket noi database de phan trang va tim kiem real-time.");
        if (!userTable.getItems().isEmpty()) {
            userTable.getSelectionModel().selectFirst();
        } else {
            populateUserDetail(null);
        }
    }

    private void applyAuctionFilters() {
        String keyword = normalize(auctionSearchField.getText());
        String status = auctionStatusFilter.getValue();
        List<SceneAdminData.AuctionRow> filtered = allAuctions.stream()
                .filter(auction -> "Tat ca".equals(status) || auction.status().equals(status))
                .filter(auction -> keyword.isEmpty() || normalize(auction.title()).contains(keyword) || normalize(auction.seller()).contains(keyword))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (auctionPage >= totalPages) {
            auctionPage = totalPages - 1;
        }

        int fromIndex = Math.max(0, auctionPage * PAGE_SIZE);
        int toIndex = Math.min(filtered.size(), fromIndex + PAGE_SIZE);
        auctionTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIndex, toIndex)));
        auctionPageLabel.setText("Trang " + (auctionPage + 1) + " / " + totalPages);
        auctionSectionNoticeLabel.setText("Bang phien dau gia dang hien thi mock data. Can ket noi database de force close va ghi log thuc.");
        if (!auctionTable.getItems().isEmpty()) {
            auctionTable.getSelectionModel().selectFirst();
        } else {
            populateAuctionDetail(null);
        }
    }

    private void refreshAuditTable() {
        LocalDate filterDate = auditDateFilter == null ? null : auditDateFilter.getValue();
        List<SceneAdminData.AuditRow> filtered = auditRows.stream()
                .filter(row -> filterDate == null || row.time().toLocalDate().equals(filterDate))
                .collect(Collectors.toList());
        auditTable.setItems(FXCollections.observableArrayList(filtered));
        auditNoticeLabel.setText(filterDate == null
                ? "Audit log chi doc. Can ket noi database de dong bo du lieu quan tri thuc te."
                : "Dang loc theo ngay " + filterDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    private void populateUserDetail(SceneAdminData.UserRow user) {
        if (user == null) {
            userDetailNameLabel.setText("Chua chon nguoi dung");
            userDetailRoleLabel.setText("-");
            userDetailStatusLabel.setText("-");
            userDetailCreatedLabel.setText("-");
            userDetailDbLabel.setText("Can ket noi database de nap lich su chi tiet cua user duoc chon.");
            userLoginTable.setItems(FXCollections.emptyObservableList());
            userActivityTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        userDetailNameLabel.setText(user.fullName());
        userDetailRoleLabel.setText("Role: " + user.role());
        userDetailStatusLabel.setText("Trang thai: " + user.status());
        userDetailCreatedLabel.setText("Ngay tao: " + user.createdAt().format(DATE_TIME_FMT));
        userDetailDbLabel.setText("Lich su dang nhap va lich su bid dang la du lieu mau. Sau nay can truy van database theo user id " + user.id() + ".");
        userLoginTable.setItems(FXCollections.observableArrayList(user.loginHistory()));
        userActivityTable.setItems(FXCollections.observableArrayList(user.activityHistory()));
    }

    private void populateAuctionDetail(SceneAdminData.AuctionRow auction) {
        if (auction == null) {
            auctionDetailTitleLabel.setText("Chua chon phien dau gia");
            auctionDetailSellerLabel.setText("-");
            auctionDetailStatusLabel.setText("-");
            auctionDetailPriceLabel.setText("-");
            forceCloseStatusLabel.setText("Chon mot phien trong bang de nhap ly do dong khan cap.");
            return;
        }

        auctionDetailTitleLabel.setText(auction.title());
        auctionDetailSellerLabel.setText("Seller: " + auction.seller());
        auctionDetailStatusLabel.setText("Trang thai: " + auction.status());
        auctionDetailPriceLabel.setText("Gia hien tai: " + formatMoney(auction.currentPrice()));
        forceCloseStatusLabel.setText("Neu force close, he thong can goi service backend va ghi audit log vao database.");
    }

    private void toggleUserStatus(SceneAdminData.UserRow user) {
        if ("ADMIN".equals(user.role())) {
            userSectionNoticeLabel.setText("Khong the khoa Admin khac. Rang buoc nay phai duoc backend xac nhan lai khi ket noi database.");
            return;
        }

        SceneAdminData.UserRow updated = user.withStatus(user.banned() ? "Hoat dong" : "Bi khoa", !user.banned());
        replaceUser(updated);
        auditRows.add(0, new SceneAdminData.AuditRow(
                LocalDateTime.now(),
                "adminmanh",
                updated.banned() ? "BAN_USER" : "UNBAN_USER",
                "User#" + updated.id(),
                "Thao tac duoc xu ly tren du lieu mau. Can ket noi database de xac nhan va luu vinh vien."
        ));
        userSectionNoticeLabel.setText(updated.banned()
                ? "Da khoa " + updated.fullName() + " tren du lieu mau. Can ket noi database de khoa that va dong bo session."
                : "Da mo khoa " + updated.fullName() + " tren du lieu mau. Can ket noi database de mo khoa that.");
        applyUserFilters();
        refreshAuditTable();
    }

    @FXML
    private void forceCloseAuction() {
        SceneAdminData.AuctionRow auction = auctionTable.getSelectionModel().getSelectedItem();
        if (auction == null) {
            forceCloseStatusLabel.setText("Can chon mot phien truoc khi dong khan cap.");
            return;
        }

        String reason = forceCloseReasonArea.getText() == null ? "" : forceCloseReasonArea.getText().trim();
        if (reason.isEmpty()) {
            forceCloseStatusLabel.setText("Can nhap ly do dong phien khan cap trong o ben duoi truoc khi thuc hien.");
            return;
        }

        SceneAdminData.AuctionRow updated = auction.withForceClosed();
        replaceAuction(updated);
        auditRows.add(0, new SceneAdminData.AuditRow(
                LocalDateTime.now(),
                "adminmanh",
                "FORCE_CLOSE_AUCTION",
                "Auction#" + updated.id(),
                reason + " | Can ghi vao audit log database khi backend san sang."
        ));
        forceCloseStatusLabel.setText("Da danh dau dong khan cap cho phien " + updated.id() + ". Hien tai chi cap nhat tren UI mau, chua goi database.");
        forceCloseReasonArea.clear();
        applyAuctionFilters();
        refreshAuditTable();
    }

    private void forceCloseAuction(SceneAdminData.AuctionRow auction) {
        auctionTable.getSelectionModel().select(auction);
        populateAuctionDetail(auction);
        forceCloseAuction();
    }

    private void replaceUser(SceneAdminData.UserRow updatedUser) {
        for (int index = 0; index < allUsers.size(); index++) {
            if (allUsers.get(index).id().equals(updatedUser.id())) {
                allUsers.set(index, updatedUser);
                break;
            }
        }
    }

    private void replaceAuction(SceneAdminData.AuctionRow updatedAuction) {
        for (int index = 0; index < allAuctions.size(); index++) {
            if (allAuctions.get(index).id().equals(updatedAuction.id())) {
                allAuctions.set(index, updatedAuction);
                break;
            }
        }
        refreshDashboard();
    }

    @FXML
    private void handleUserPrevPage() {
        if (userPage > 0) {
            userPage--;
            applyUserFilters();
        }
    }

    @FXML
    private void handleUserNextPage() {
        userPage++;
        applyUserFilters();
    }

    @FXML
    private void handleAuctionPrevPage() {
        if (auctionPage > 0) {
            auctionPage--;
            applyAuctionFilters();
        }
    }

    @FXML
    private void handleAuctionNextPage() {
        auctionPage++;
        applyAuctionFilters();
    }

    @FXML
    private void handleAuditFilter() {
        refreshAuditTable();
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/bidding/client/scene/Scene1.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Login System");
            stage.setMaximized(false);
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            dbSummaryLabel.setText("Khong mo duoc Scene1.fxml. Can kiem tra lai duong dan login.");
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private String formatMoney(long amount) {
        return String.format("%,d VND", amount).replace(',', '.');
    }
}
