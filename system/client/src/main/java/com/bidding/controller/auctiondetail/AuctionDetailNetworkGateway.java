package com.bidding.controller.auctiondetail;

import com.bidding.network.NetworkClient;
import com.bidding.util.JsonUtil;
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
        JsonObject request = JsonUtil.request("GET_AUCTION_HISTORY");
        request.addProperty("auctionId", auctionId);
        networkClient.sendJson(request);
    }

    public void requestAuctions() {
        JsonObject request = JsonUtil.request("GET_AUCTIONS");
        networkClient.sendJson(request);
    }

    public void requestSellerProfile(String sellerId) {
        JsonObject request = JsonUtil.request("GET_SELLER_PROFILE");
        request.addProperty("sellerId", sellerId);
        networkClient.sendJson(request);
    }

    public void sendBid(String auctionId, double amount) {
        JsonObject request = JsonUtil.request("BID");
        request.addProperty("auctionId", auctionId);
        request.addProperty("amount", amount);
        networkClient.sendJson(request);
    }

    public void sendAutoBid(String auctionId, double maxBid, double increment) {
        JsonObject request = JsonUtil.request("REGISTER_AUTO_BID");
        request.addProperty("auctionId", auctionId);
        request.addProperty("maxBid", maxBid);
        request.addProperty("increment", increment);
        networkClient.sendJson(request);
    }
}
