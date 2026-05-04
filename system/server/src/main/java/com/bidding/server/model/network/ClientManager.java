package com.bidding.server.model.network;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.item.Item;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientManager implements AuctionObserver {
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public void addClient(ClientHandler client) {
        activeClients.add(client);
        System.out.println("ClientManager: added client. Total: " + activeClients.size());
    }

    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
        System.out.println("ClientManager: removed client. Total: " + activeClients.size());
    }

    @Override
    public void onAuctionStarted(AuctionEvent event) {
        JsonObject message = new JsonObject();
        message.addProperty("action", "NEW_AUCTION_POSTED");
        message.add("data", buildAuctionJson(event.getAuction()));
        broadcast(message.toString());
    }

    @Override
    public void onBidPlaced(AuctionEvent event) {
        JsonObject message = new JsonObject();
        message.addProperty("action", "NEW_BID");
        message.addProperty("auctionId", event.getAuctionId());
        message.addProperty("newPrice", event.getNewPrice());
        message.addProperty("winnerId", event.getWinnerId() == null ? "" : event.getWinnerId());
        broadcast(message.toString());
    }

    @Override
    public void onAuctionClosed(AuctionEvent event) {
        JsonObject message = new JsonObject();
        message.addProperty("action", "AUCTION_END");
        message.addProperty("auctionId", event.getAuctionId());
        message.addProperty("winnerId", event.getWinnerId() == null ? "NONE" : event.getWinnerId());
        message.addProperty("finalPrice", event.getNewPrice());
        broadcast(message.toString());
    }

    @Override
    public void onPriceChanged(AuctionEvent event) {
    }

    public void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }

    private JsonObject buildAuctionJson(Auction auction) {
        JsonObject json = new JsonObject();
        if (auction == null) {
            return json;
        }

        json.addProperty("id", auction.getId());
        json.addProperty("auctionId", auction.getId());
        json.addProperty("status", auction.getStatus() == null ? "" : auction.getStatus().name());
        json.addProperty("startTime", auction.getStartTime() == null ? "" : auction.getStartTime().toString());
        json.addProperty("endTime", auction.getEndTime() == null ? "" : auction.getEndTime().toString());
        json.addProperty("currentPrice", auction.getCurrentPrice() == null ? 0.0 : auction.getCurrentPrice().get());
        json.addProperty("winnerId", auction.getCurrentWinner() == null ? "" : auction.getCurrentWinner().getUsername());

        Item item = auction.getItem();
        if (item != null) {
            json.addProperty("itemId", item.getId());
            json.add("item", buildItemJson(item));
        }
        return json;
    }

    private JsonObject buildItemJson(Item item) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item.getId());
        json.addProperty("itemId", item.getId());
        json.addProperty("name", item.getName());
        json.addProperty("description", item.getDescription());
        json.addProperty("startingPrice", item.getStartingPrice());
        json.addProperty("category", item.getCategory());
        json.addProperty("item_type", item.getCategory());
        json.addProperty("seller", item.getSeller() == null ? "" : item.getSeller().getUsername());
        json.addProperty("condition", item.getCondition() == null ? "" : item.getCondition().name());

        JsonArray images = new JsonArray();
        if (item.getImages() != null) {
            for (String image : item.getImages()) {
                images.add(image);
            }
        }
        json.add("images", images);

        JsonObject specs = new JsonObject();
        item.getSpecifications().forEach(specs::addProperty);
        json.add("specifications", specs);
        return json;
    }
}
