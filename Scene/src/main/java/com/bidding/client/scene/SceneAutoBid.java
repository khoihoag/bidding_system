package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SceneAutoBid implements Initializable {

    @FXML private TextField txtProductId;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Label lblMessage;
    @FXML private Label lblServerNote;
    @FXML private TableView<AutoBidItem> tableAutoBid;
    @FXML private TableColumn<AutoBidItem, String> colProductId;
    @FXML private TableColumn<AutoBidItem, String> colProductName;
    @FXML private TableColumn<AutoBidItem, String> colCurrentPrice;
    @FXML private TableColumn<AutoBidItem, String> colMaxBid;
    @FXML private TableColumn<AutoBidItem, String> colIncrement;
    @FXML private TableColumn<AutoBidItem, String> colStatus;
    @FXML private Button btnBack;

    private final ObservableList<AutoBidItem> autoBidList = FXCollections.observableArrayList();

    @Override
    // Khoi tao bang va thong bao ro action nao co trong doc.
    public void initialize(URL location, ResourceBundle resources) {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colMaxBid.setCellValueFactory(new PropertyValueFactory<>("maxBid"));
        colIncrement.setCellValueFactory(new PropertyValueFactory<>("increment"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableAutoBid.setItems(autoBidList);
        tableAutoBid.setPlaceholder(new Label("Hiện chưa có chức năng lấy danh sách trả giá tự động, nên màn này tạm thời chỉ gửi cấu hình lên server."));
        lblServerNote.setText("Nhập mã phiên, giá tối đa và bước giá. Tài liệu JSON hiện mới cho phép đăng ký trả giá tự động, chưa hỗ trợ tải danh sách đã lưu.");
        NetworkClient.autoBidListener = this::handleAutoBidReply;
    }

    @FXML
    // Gui REGISTER_AUTO_BID theo dung schema doc.
    void handleStartBot(ActionEvent event) {
        lblMessage.setText("");
        String auctionId = safeText(txtProductId);
        String maxBid = safeText(txtMaxBid);
        String increment = safeText(txtIncrement);

        if (auctionId.isEmpty() || maxBid.isEmpty() || increment.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin.", false);
            return;
        }

        try {
            long maxBidValue = Long.parseLong(normalizeNumber(maxBid));
            long incrementValue = Long.parseLong(normalizeNumber(increment));
            String json = String.format(
                    "{\"action\":\"REGISTER_AUTO_BID\",\"auctionId\":\"%s\",\"maxBid\":%d,\"increment\":%d}",
                    escapeJson(auctionId),
                    maxBidValue,
                    incrementValue
            );
            NetworkClient.send(json);
            showMessage("Đã gửi cấu hình trả giá tự động lên server. Bảng bên dưới sẽ để trống cho tới khi có thêm chức năng lấy danh sách.", true);
        } catch (NumberFormatException exception) {
            showMessage("Giá tối đa và bước giá phải là số hợp lệ.", false);
        }
    }

    @FXML
    // Quay ve bidder dashboard.
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception exception) {
            showMessage("Không thể quay lại màn hình người mua.", false);
        }
    }

    // Xu ly ket qua dang ky auto bid tu server.
    private void handleAutoBidReply(JsonObject response) {
        String action = getString(response, "action", "");
        if ("ERROR".equals(action)) {
            showMessage(getString(response, "message", "Không lưu được cấu hình trả giá tự động."), false);
            return;
        }
        if (!"REGISTER_AUTO_BID_REPLY".equals(action)) {
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(getString(response, "status", ""))) {
            showMessage("Server đã xác nhận cấu hình trả giá tự động. Hiện chưa có chức năng tải lại danh sách đã lưu.", true);
            txtProductId.clear();
            txtMaxBid.clear();
            txtIncrement.clear();
        } else {
            showMessage(getString(response, "message", "Server từ chối cấu hình trả giá tự động."), false);
        }
    }

    // Hien message tren man auto bid.
    private void showMessage(String message, boolean success) {
        lblMessage.setStyle(success
                ? "-fx-text-fill: #15803d; -fx-font-weight: bold;"
                : "-fx-text-fill: #b93832; -fx-font-weight: bold;");
        lblMessage.setText(message);
    }

    // Lam sach textfield.
    private String safeText(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    // Chuan hoa so truoc khi parse.
    private String normalizeNumber(String value) {
        return value.replace(".", "").replace(",", "").trim();
    }

    // Escape chuoi truoc khi gui JSON.
    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Doc chuoi JSON voi fallback tuy chon.
    private String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    public static class AutoBidItem {
        private final String productId;
        private final String productName;
        private final String currentPrice;
        private final String maxBid;
        private final String increment;
        private final String status;

        // Tao dong du lieu cho table khi sau nay doc bo sung action list.
        public AutoBidItem(String productId, String productName, String currentPrice, String maxBid, String increment, String status) {
            this.productId = productId;
            this.productName = productName;
            this.currentPrice = currentPrice;
            this.maxBid = maxBid;
            this.increment = increment;
            this.status = status;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCurrentPrice() { return currentPrice; }
        public String getMaxBid() { return maxBid; }
        public String getIncrement() { return increment; }
        public String getStatus() { return status; }
    }
}
