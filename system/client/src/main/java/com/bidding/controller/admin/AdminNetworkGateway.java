package com.bidding.controller.admin;

import com.bidding.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.scene.control.Label;

import java.util.function.Consumer;

/**
 * Wraps socket connectivity and outbound admin requests.
 */
public class AdminNetworkGateway {

    private final Label connectionLabel;
    private final AdminViewState state;

    public AdminNetworkGateway(Label connectionLabel, AdminViewState state) {
        this.connectionLabel = connectionLabel;
        this.state = state;
    }

    public void connectOnce() {
        if (!state.connectionStarted) {
            state.connectionStarted = true;
            NetworkClient.getInstance().connect();
        }
        updateConnectingStatus();
    }

    public void setupMessageHandler(Consumer<JsonObject> handler) {
        NetworkClient.getInstance().setMessageHandler(handler);
    }

    public boolean checkConnected() {
        connectOnce();
        boolean connected = NetworkClient.getInstance().isConnected();
        if (!connected) {
            connectionLabel.setText("● Mất kết nối");
            connectionLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px; -fx-font-weight: 700;");
        }
        return connected;
    }

    public void sendRequest(JsonObject req) {
        connectOnce();
        NetworkClient.getInstance().sendJson(req);
        connectionLabel.setText("● Đã gửi yêu cầu");
        connectionLabel.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12px; -fx-font-weight: 700;");
    }

    public void sendAction(String action) {
        if (!checkConnected()) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", action);
        sendRequest(req);
    }

    public void sendBanUser(String userId) {
        if (!checkConnected()) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", "BAN_USER");
        req.addProperty("targetUserId", userId);
        sendRequest(req);
    }

    public void sendUnbanUser(String userId) {
        if (!checkConnected()) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", "UNBAN_USER");
        req.addProperty("targetUserId", userId);
        sendRequest(req);
    }

    public void sendForceClose(String auctionId) {
        if (!checkConnected()) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", "FORCE_CLOSE");
        req.addProperty("auctionId", auctionId);
        sendRequest(req);
    }

    public void sendAuctionBidHistoryRequest(String auctionId) {
        if (!checkConnected()) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_ADMIN_AUCTION_BID_HISTORY");
        req.addProperty("auctionId", auctionId);
        sendRequest(req);
    }

    private void updateConnectingStatus() {
        connectionLabel.setText("● Đang kết nối");
        connectionLabel.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12px; -fx-font-weight: 700;");
    }
}
