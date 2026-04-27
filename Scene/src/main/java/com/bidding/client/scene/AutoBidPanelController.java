package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class AutoBidPanelController implements Initializable {

    @FXML private ToggleButton toggleAutoBid;
    @FXML private Label lblExpandIcon;
    @FXML private VBox panelBody;

    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Label lblMaxBidValidation;
    @FXML private Button btnSaveAutoBid;
    @FXML private Label lblSaveResult;

    @FXML private VBox vboxCurrentAutoBid;
    @FXML private Label lblActiveMaxBid;
    @FXML private Label lblActiveIncrement;

    private final NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
    private boolean isExpanded = true;
    private boolean isEnabled = false;
    private long currentPrice = 0L;
    private String currentAuctionId = "";
    private int currentItemId = -1;

    // Khoi tao validate va listener auto-bid reply.
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        txtMaxBid.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9]*")) {
                txtMaxBid.setText(newValue.replaceAll("[^0-9]", ""));
            }
        });
        txtIncrement.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[0-9]*")) {
                txtIncrement.setText(newValue.replaceAll("[^0-9]", ""));
            }
        });
        NetworkClient.autoBidListener = this::handleAutoBidReply;
    }

    // Cap nhat gia hien tai de validate auto-bid.
    public void setCurrentPrice(long price) {
        this.currentPrice = price;
    }

    // Ghi nho auction dang mo de gui REGISTER_AUTO_BID.
    public void setAuctionContext(String auctionId, int itemId) {
        this.currentAuctionId = auctionId == null ? "" : auctionId;
        this.currentItemId = itemId;
    }

    // Mo dong panel auto-bid.
    @FXML
    private void handleTogglePanel() {
        isExpanded = !isExpanded;
        panelBody.setVisible(isExpanded);
        panelBody.setManaged(isExpanded);
        lblExpandIcon.setText(isExpanded ? "\u25bc" : "\u25ba");
    }

    // Bat tat che do auto-bid.
    @FXML
    private void handleToggleAutoBid() {
        isEnabled = toggleAutoBid.isSelected();
        if (isEnabled) {
            toggleAutoBid.setText("BAT");
            toggleAutoBid.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;");
        } else {
            toggleAutoBid.setText("TAT");
            toggleAutoBid.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #757575; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;");
            vboxCurrentAutoBid.setVisible(false);
            vboxCurrentAutoBid.setManaged(false);
        }
    }

    // Gui cau hinh REGISTER_AUTO_BID len server.
    @FXML
    private void handleSaveAutoBid() {
        String maxBidStr = txtMaxBid.getText().trim();
        String incrStr = txtIncrement.getText().trim();

        if (maxBidStr.isEmpty()) {
            showMaxBidValidation("Vui long nhap gia toi da.");
            return;
        }

        long maxBid;
        long increment;
        try {
            maxBid = Long.parseLong(maxBidStr);
            increment = incrStr.isEmpty() ? 100_000L : Long.parseLong(incrStr);
        } catch (NumberFormatException exception) {
            showMaxBidValidation("So tien khong hop le.");
            return;
        }

        if (maxBid <= currentPrice) {
            showMaxBidValidation("Gia toi da phai lon hon gia hien tai (" + nf.format(currentPrice) + " VND).");
            return;
        }

        hideMaxBidValidation();
        lblSaveResult.setText("Dang luu len server...");
        lblSaveResult.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565C0;");
        lblSaveResult.setVisible(true);
        lblSaveResult.setManaged(true);

        String requestJson;
        if (currentAuctionId != null && !currentAuctionId.isBlank()) {
            requestJson = String.format(
                    "{\"action\":\"REGISTER_AUTO_BID\",\"auctionId\":\"%s\",\"maxBid\":%d,\"increment\":%d}",
                    escapeJson(currentAuctionId),
                    maxBid,
                    increment
            );
        } else {
            requestJson = String.format(
                    "{\"action\":\"REGISTER_AUTO_BID\",\"itemId\":%d,\"maxBid\":%d,\"increment\":%d}",
                    currentItemId,
                    maxBid,
                    increment
            );
        }
        NetworkClient.send(requestJson);
    }

    // Xu ly reply khi server luu auto-bid.
    private void handleAutoBidReply(JsonObject response) {
        String action = getString(response, "action");
        if ("ERROR".equals(action)) {
            lblSaveResult.setText(getString(response, "message", "Khong luu duoc auto-bid."));
            lblSaveResult.setStyle("-fx-font-size: 11px; -fx-text-fill: #C62828;");
            lblSaveResult.setVisible(true);
            lblSaveResult.setManaged(true);
            return;
        }
        if (!"REGISTER_AUTO_BID_REPLY".equals(action)) {
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(getString(response, "status"))) {
            long maxBid = getMaxBidAmount();
            long increment = getIncrement();
            onSaveSuccess(maxBid, increment);
        } else {
            lblSaveResult.setText(getString(response, "message", "Server tu choi luu auto-bid."));
            lblSaveResult.setStyle("-fx-font-size: 11px; -fx-text-fill: #C62828;");
            lblSaveResult.setVisible(true);
            lblSaveResult.setManaged(true);
        }
    }

    // Cap nhat UI sau khi luu thanh cong.
    private void onSaveSuccess(long maxBid, long increment) {
        lblSaveResult.setText("Da luu thanh cong!");
        lblSaveResult.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32;");
        lblSaveResult.setVisible(true);
        lblSaveResult.setManaged(true);

        vboxCurrentAutoBid.setVisible(true);
        vboxCurrentAutoBid.setManaged(true);
        lblActiveMaxBid.setText(nf.format(maxBid) + " VND");
        lblActiveIncrement.setText(nf.format(increment) + " VND");

        new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            lblSaveResult.setVisible(false);
            lblSaveResult.setManaged(false);
        })).play();
    }

    // Hien validate loi cho max bid.
    private void showMaxBidValidation(String message) {
        lblMaxBidValidation.setText(message);
        lblMaxBidValidation.setVisible(true);
        lblMaxBidValidation.setManaged(true);
    }

    // An validate khi hop le.
    private void hideMaxBidValidation() {
        lblMaxBidValidation.setVisible(false);
        lblMaxBidValidation.setManaged(false);
    }

    // Tra ve trang thai bat auto-bid.
    public boolean isAutoBidEnabled() {
        return isEnabled;
    }

    // Lay max bid tu input hien tai.
    public long getMaxBidAmount() {
        try {
            return Long.parseLong(txtMaxBid.getText().replaceAll("[^0-9]", ""));
        } catch (Exception exception) {
            return 0L;
        }
    }

    // Lay buoc gia tu input hien tai.
    public long getIncrement() {
        try {
            return Long.parseLong(txtIncrement.getText().replaceAll("[^0-9]", ""));
        } catch (Exception exception) {
            return 100_000L;
        }
    }

    // Tu dong dat gia khi server bao co gia moi.
    public void evaluateAndBid(String auctionId, int itemId, long newServerPrice, String highestBidder) {
        String myUsername = Scene1.myname;
        if (!isEnabled || myUsername.equals(highestBidder)) {
            System.out.println("Dang dan dau - khong kich hoat auto bid");
            return;
        }

        long maxBid = getMaxBidAmount();
        long nextBid = newServerPrice + getIncrement();
        if (nextBid <= maxBid) {
            String json;
            if (auctionId != null && !auctionId.isBlank()) {
                json = String.format("{\"action\": \"BID\", \"auctionId\": \"%s\", \"amount\": %d}", escapeJson(auctionId), nextBid);
            } else {
                json = String.format("{\"action\": \"BID\", \"itemId\": %d, \"amount\": %d}", itemId, nextBid);
            }

            NetworkClient.send(json);
            System.out.println("[AutoBid] Da tu dong dat gia moi: " + nextBid);
        } else {
            System.out.println("[AutoBid] Gia " + nextBid + " vuot qua gioi han " + maxBid + ". Dung.");
        }
    }

    // Escape text truoc khi gui JSON.
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Lay string tu response.
    private String getString(JsonObject response, String key) {
        return getString(response, key, "");
    }

    // Lay string tu response voi fallback.
    private String getString(JsonObject response, String key, String fallback) {
        if (response == null || !response.has(key) || response.get(key).isJsonNull()) {
            return fallback;
        }
        return response.get(key).getAsString();
    }
}
