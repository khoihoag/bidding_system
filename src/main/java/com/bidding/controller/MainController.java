package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.text.NumberFormat;
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
    // Cái khung trống bên phải để nhúng các màn hình con vào
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        // Lấy tên User đang đăng nhập hiển thị lên Sidebar
        String username = UserSession.getInstance().getUsername();
        lblUsername.setText(username != null ? username : "Khách");

        // Mặc định lúc vừa vào thì nhúng màn hình Kho đồ lên trước
        handleDashboard();
        updateBalanceDisplay();
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
        NetworkClient.getInstance().setMessageHandler(null);
        UserSession.getInstance().clear();

        // Dùng con tàu AppNavigator chở về màn Login
        AppNavigator.navigate("Login.fxml");
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
            NumberFormat currencyFmt = NumberFormat.getNumberInstance(java.util.Locale.of("vi", "VN"));

            // Cập nhật text cho Label
            Platform.runLater(() -> {
                balanceLabel.setText("Số dư: " + currencyFmt.format(currentBalance) + " ₫");
            });
        }
    }
}