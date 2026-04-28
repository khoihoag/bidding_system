package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class SceneLSGD {

    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colId;
    @FXML private TableColumn<Transaction, String> colItemName;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colTime;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private Label lblTransactionCount;
    @FXML private Label lblTotalSpent;
    @FXML private Label lblHistoryNote;

    private final ObservableList<Transaction> data = FXCollections.observableArrayList();

    // Khoi tao bang va gui yeu cau HISTORY.
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableTransactions.setItems(data);

        NetworkClient.historyListener = this::handleHistoryResponse;
        NetworkClient.send("{\"action\":\"HISTORY\"}");
        lblHistoryNote.setText("Du lieu bang duoc nap that tu HISTORY_REPLY.");
    }

    // Xu ly lich su tra ve tu server.
    private void handleHistoryResponse(JsonObject response) {
        String action = getString(response, "action");
        if ("ERROR".equals(action)) {
            showAlert(Alert.AlertType.ERROR, "Khong tai duoc lich su", getString(response, "message", "Server tra ve loi."));
            return;
        }
        if (!"HISTORY_REPLY".equals(action)) {
            return;
        }

        data.clear();
        JsonArray historyArray = getArray(response, "data");
        long totalSpent = 0L;
        for (JsonElement element : historyArray) {
            JsonObject item = element.getAsJsonObject();
            long amount = getLong(item, "amount", getLong(item, "price", getLong(item, "newPrice", 0L)));
            totalSpent += amount;
            data.add(new Transaction(
                    firstNonBlank(getString(item, "id"), getString(item, "itemId"), getString(item, "auctionId"), "---"),
                    firstNonBlank(getString(item, "itemName"), getNestedString(item, "item", "name"), "Chua co ten"),
                    formatPrice(amount) + " d",
                    firstNonBlank(getString(item, "time"), getString(item, "createdAt"), getString(item, "bidTime"), "---"),
                    firstNonBlank(getString(item, "status"), getString(item, "result"), "UNKNOWN")
            ));
        }
        lblTransactionCount.setText(String.valueOf(data.size()));
        lblTotalSpent.setText(formatPrice(totalSpent) + " d");
        lblHistoryNote.setText(data.isEmpty()
                ? "Server da tra HISTORY_REPLY nhung hien chua co giao dich nao."
                : "Tong ket ben tren duoc tinh truc tiep tu mang data cua HISTORY_REPLY.");
    }

    // Quay ve scene bidder.
    @FXML
    void goBack(ActionEvent event) {
        try {
            Stage stage = (Stage) tableTransactions.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Loi dieu huong", "Khong mo duoc SceneBidder1.fxml");
        }
    }

    // Format gia tri tien te de hien thi.
    private String formatPrice(long value) {
        try {
            return String.format("%,d", value).replace(',', '.');
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    // Lay chuoi tu object voi fallback rong.
    private String getString(JsonObject object, String key) {
        return getString(object, key, "");
    }

    // Lay chuoi tu object voi fallback tuy chon.
    private String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    // Lay mang tu object.
    private JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    // Lay object con va doc field string.
    private String getNestedString(JsonObject object, String objectKey, String valueKey) {
        if (object == null || !object.has(objectKey) || !object.get(objectKey).isJsonObject()) {
            return "";
        }
        return getString(object.getAsJsonObject(objectKey), valueKey, "");
    }

    // Lay long tu object voi fallback.
    private long getLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception exception) {
            try {
                return Long.parseLong(object.get(key).getAsString().replace(".", "").replace(",", "").trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    // Tra ve chuoi dau tien khong rong.
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    // Hien alert ngan gon.
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Transaction {
        private final String id;
        private final String itemName;
        private final String amount;
        private final String time;
        private final String status;

        // Tao dong lich su de bind vao table.
        public Transaction(String id, String itemName, String amount, String time, String status) {
            this.id = id;
            this.itemName = itemName;
            this.amount = amount;
            this.time = time;
            this.status = status;
        }

        // Tra ve id giao dich.
        public String getId() {
            return id;
        }

        // Tra ve ten vat pham.
        public String getItemName() {
            return itemName;
        }

        // Tra ve so tien hien thi.
        public String getAmount() {
            return amount;
        }

        // Tra ve moc thoi gian.
        public String getTime() {
            return time;
        }

        // Tra ve trang thai giao dich.
        public String getStatus() {
            return status;
        }
    }
}
