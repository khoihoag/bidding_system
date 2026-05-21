package com.bidding.controller.auctiondetail;

import com.bidding.network.NetworkClient;
import com.google.gson.JsonObject;

/**
 * Outbound socket requests for the auction detail screen.
 */
public class AuctionDetailNetworkGateway {

    private final NetworkClient networkClient = NetworkClient.getInstance();

    public void registerMessageHandler(java.util.function.Consumer<JsonObject> handler) {
        networkClient.setMessageHandler(handler);
    }

    public void requestAuctionHistory(String auctionId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTION_HISTORY");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);
    }

    public void requestAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        networkClient.sendJson(request);
    }

    public void sendBid(String auctionId, double amount) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "BID");
        request.addProperty("auctionId", auctionId);
        request.addProperty("amount", amount);
        networkClient.sendJson(request);
    }

    public void sendAutoBid(String auctionId, double maxBid, double increment) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "REGISTER_AUTO_BID");
        request.addProperty("auctionId", auctionId);
        request.addProperty("maxBid", maxBid);
        request.addProperty("increment", increment);
        networkClient.sendJson(request);
    }
}
