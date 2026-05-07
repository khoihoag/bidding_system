package com.bidding.controller;

import com.bidding.model.BidHistoryRecord;
import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BidHistoryController {

    // ─── FXML nodes — Top Bar ──────────────────────────────────────────────────
    @FXML private Label subtitleLabel;

    // ─── FXML nodes — Stat Cards ──────────────────────────────────────────────
    @FXML private Label totalBidsStatLabel;
    @FXML private Label acceptedBidsStatLabel;
    @FXML private Label rejectedBidsStatLabel;
    @FXML private Label highestBidStatLabel;

    // ─── FXML nodes — Filter ──────────────────────────────────────────────────
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label            recordCountLabel;

    // ─── FXML nodes — Table ───────────────────────────────────────────────────
    @FXML private TableView<BidHistoryRecord>   historyTable;
    @FXML private TableColumn<BidHistoryRecord, Integer> colIndex;
    @FXML private TableColumn<BidHistoryRecord, String>  colAuctionId;
    @FXML private TableColumn<BidHistoryRecord, String>  colBidAmount;
    @FXML private TableColumn<BidHistoryRecord, String>  colBidTime;
    @FXML private TableColumn<BidHistoryRecord, String>  colBidType;
    @FXML private TableColumn<BidHistoryRecord, String>  colStatus;

    // ─── FXML nodes — Status ──────────────────────────────────────────────────
    @FXML private Label statusLabel;

    // ─── State ────────────────────────────────────────────────────────────────
    private ObservableList<BidHistoryRecord> masterList;
    private FilteredList<BidHistoryRecord>   filteredList;

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final UserSession   session       = UserSession.getInstance();
    private final NumberFormat  currencyFmt   =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupFilter();

        networkClient.setMessageHandler(this::handleServerMessage);

        showStatus("Đang tải lịch sử...");
        requestHistory();
    }

    // ─── Table Setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // Bind từng cột với property tương ứng trong BidHistoryRecord
        colIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colBidType.setCellValueFactory(new PropertyValueFactory<>("bidType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Căn giữa cột STT
        colIndex.setStyle("-fx-alignment: CENTER;");

        // Căn phải cột số tiền
        colBidAmount.setStyle("-fx-alignment: CENTER_RIGHT;");

        // Custom cell factory cho cột Trạng Thái — hiển thị badge màu
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(mapStatusDisplay(status));
                    setStyle(mapStatusCellStyle(status));
                }
            }
        });

        // Custom cell factory cho cột Loại — icon trực quan
        colBidType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(mapBidTypeDisplay(type));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // Tạo danh sách ban đầu trống
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        historyTable.setItems(filteredList);
    }

    private void setupFilter() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tất cả", "ACCEPTED", "REJECTED", "PENDING"
        ));
        statusFilterCombo.getSelectionModel().selectFirst();
    }

    // ─── Network — Outgoing ───────────────────────────────────────────────────

    private void requestHistory() {
        JsonObject request = new JsonObject();
        // Đổi "HISTORY" thành "GET_HISTORY" hoặc đúng cái chữ sếp đã cài bên Server
        request.addProperty("action", "GET_HISTORY");
        networkClient.sendJson(request);
    }

    // ─── Network — Incoming ───────────────────────────────────────────────────

    private void handleServerMessage(JsonObject json) {
        System.out.println("[Client UI] Đã nhận bưu kiện từ Server: " + json.toString());
        if (!json.has("action")) return;
        String action = json.get("action").getAsString();

        switch (action) {
            case "HISTORY_REPLY" -> handleHistoryReply(json);
            case "ERROR"         -> handleError(json);
            default              -> {}
        }
    }

    private void handleHistoryReply(JsonObject json) {
        if (!json.has("data")) {
            Platform.runLater(() -> showStatus("Không có dữ liệu lịch sử."));
            return;
        }

        JsonArray data = json.getAsJsonArray("data");

        Platform.runLater(() -> {
            masterList.clear();

            if (data.isEmpty()) {
                showStatus("Bạn chưa có lịch sử đặt giá nào.");
                updateStatCards(masterList);
                updateRecordCount();
                return;
            }

            int index         = 1;
            int acceptedCount = 0;
            int rejectedCount = 0;
            double highestBid = 0.0;

            for (JsonElement element : data) {
                JsonObject entry = element.getAsJsonObject();

                String auctionId = getStringSafe(entry, "auctionId");
                double bidAmount = entry.has("amount")
                        ? entry.get("amount").getAsDouble() : 0.0;
                String rawTime   = getStringSafe(entry, "timestamp");
                String bidType = "MANUAL";
                if (entry.has("isAuto")) {
                    boolean isAuto = entry.get("isAuto").getAsBoolean();
                    bidType = isAuto ? "AUTO" : "MANUAL";
                }
                String status    = getStringSafe(entry, "status");

                // Định dạng số tiền
                String formattedAmount = currencyFmt.format(bidAmount) + " ₫";

                // Định dạng thời gian
                String formattedTime = formatTimestampForDisplay(rawTime);

                masterList.add(new BidHistoryRecord(
                        index++,
                        auctionId,
                        formattedAmount,
                        formattedTime,
                        bidType,
                        status
                ));

                // Tính thống kê
                if ("ACCEPTED".equalsIgnoreCase(status)) acceptedCount++;
                if ("REJECTED".equalsIgnoreCase(status)) rejectedCount++;
                if (bidAmount > highestBid) highestBid = bidAmount;
            }

            // Cập nhật stat cards
            totalBidsStatLabel.setText(String.valueOf(masterList.size()));
            acceptedBidsStatLabel.setText(String.valueOf(acceptedCount));
            rejectedBidsStatLabel.setText(String.valueOf(rejectedCount));
            highestBidStatLabel.setText(currencyFmt.format(highestBid) + " ₫");

            updateRecordCount();
            hideStatus();

            // Cập nhật subtitle
            subtitleLabel.setText(
                    "Hiển thị " + masterList.size() + " lượt đặt giá của "
                            + session.getUsername()
            );
        });
    }

    private void handleError(JsonObject json) {
        String message = getStringSafe(json, "message");
        Platform.runLater(() -> showStatus("❌ " + message));
    }

    // ─── Filter Handler ───────────────────────────────────────────────────────

    @FXML
    private void handleFilterChange() {
        String selected = statusFilterCombo.getValue();

        if (selected == null || selected.equals("Tất cả")) {
            filteredList.setPredicate(p -> true);
        } else {
            filteredList.setPredicate(record ->
                    record.getStatus().equalsIgnoreCase(selected)
            );
        }

        updateRecordCount();
    }

    @FXML
    private void handleRefresh() {
        masterList.clear();
        statusFilterCombo.getSelectionModel().selectFirst();
        showStatus("Đang tải lại...");
        requestHistory();
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Main.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 750));
            stage.setTitle("BidVault – Sảnh Đấu Giá");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[BidHistoryController] Không thể quay lại Main: "
                    + e.getMessage());
        }
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void updateStatCards(ObservableList<BidHistoryRecord> list) {
        totalBidsStatLabel.setText("0");
        acceptedBidsStatLabel.setText("0");
        rejectedBidsStatLabel.setText("0");
        highestBidStatLabel.setText("0 ₫");
    }

    private void updateRecordCount() {
        int shown = filteredList.size();
        int total = masterList.size();
        recordCountLabel.setText(
                shown == total
                        ? "Hiển thị tất cả " + total + " bản ghi"
                        : "Hiển thị " + shown + " / " + total + " bản ghi"
        );
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
    }

    private String mapStatusDisplay(String status) {
        return switch (status.toUpperCase()) {
            case "ACCEPTED" -> "✅ Chấp nhận";
            case "REJECTED" -> "❌ Từ chối";
            case "PENDING"  -> "⏳ Chờ xử lý";
            default         -> "⚪ " + status;
        };
    }

    private String mapStatusCellStyle(String status) {
        return switch (status.toUpperCase()) {
            case "ACCEPTED" -> "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER;";
            case "REJECTED" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER;";
            case "PENDING"  -> "-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-alignment: CENTER;";
            default         -> "-fx-alignment: CENTER;";
        };
    }

    private String mapBidTypeDisplay(String type) {
        return switch (type.toUpperCase()) {
            case "AUTO"   -> "🤖 Auto-Bid";
            case "MANUAL" -> "✋ Thủ công";
            default       -> type;
        };
    }

    private String formatTimestampForDisplay(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "---";
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTimestamp, ISO_FMT);
            return dt.format(DISPLAY_FMT);
        } catch (Exception e) {
            // Trả về chuỗi gốc nếu không parse được
            return isoTimestamp;
        }
    }

    private String getStringSafe(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : "";
    }
}
