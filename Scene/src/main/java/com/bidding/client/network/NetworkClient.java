package com.bidding.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    @SuppressWarnings("unused")
    private static final Gson gson = new Gson();

    public static Consumer<JsonObject> loginListener;
    public static Consumer<JsonObject> registerListener;
    public static Consumer<JsonObject> myBidsListener;
    public static Consumer<JsonObject> itemsListener;
    public static Consumer<JsonObject> historyListener;
    public static Consumer<JsonObject> sellerListener;
    public static Consumer<JsonObject> autoBidListener;
    public static Consumer<JsonObject> notificationListener;
    public static Consumer<JsonObject> addItemListener;

    // Mo ket noi socket toi server.
    public static void connect(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Da ket noi toi server.");
            startListening();
        } catch (IOException exception) {
            System.err.println("Khong the ket noi toi server: " + exception.getMessage());
        }
    }

    // Gui chuoi JSON len server.
    public static void send(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        } else {
            System.err.println("Chua connect server nen khong gui duoc du lieu.");
        }
    }

    // Tao luong nen de nghe server.
    private static void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("[SERVER] " + serverMessage);
                    try {
                        JsonObject response = JsonParser.parseString(serverMessage).getAsJsonObject();
                        handleServerAction(response);
                    } catch (Exception parseException) {
                        System.err.println("Khong parse duoc JSON tu server: " + parseException.getMessage());
                    }
                }
            } catch (IOException exception) {
                System.err.println("Mat ket noi server: " + exception.getMessage());
            }
        }, "network-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Dieu huong action server ve dung man hinh.
    private static void handleServerAction(JsonObject response) {
        String action = response.has("action") && !response.get("action").isJsonNull()
                ? response.get("action").getAsString()
                : "";

        switch (action) {
            case "LOGIN_REPLY" -> dispatch(loginListener, response);
            case "REGISTER_REPLY" -> dispatch(registerListener, response);
            case "MY_BIDS_LIST" -> dispatch(myBidsListener, response);
            case "ITEMS_LIST", "UPDATE_PRICE", "NEW_BID", "NEW_AUCTION_POSTED", "BID_REPLY", "AUCTION_END" ->
                    dispatch(itemsListener, response);
            case "REGISTER_AUTO_BID_REPLY" -> dispatch(autoBidListener, response);
            case "HISTORY_REPLY" -> dispatch(historyListener, response);
            case "AUCTIONS_LIST", "DELETE_ITEM_REPLY", "START_AUCTION_REPLY" -> dispatch(sellerListener, response);
            case "ADD_ITEM_REPLY", "UPDATE_ITEM_REPLY", "SCHEDULE_AUCTION_REPLY" -> {
                dispatch(sellerListener, response);
                dispatch(addItemListener, response);
            }
            case "GLOBAL_NOTIFY" -> {
                dispatch(itemsListener, response);
                dispatch(sellerListener, response);
                dispatch(notificationListener, response);
            }
            case "ERROR" -> {
                dispatch(loginListener, response);
                dispatch(registerListener, response);
                dispatch(itemsListener, response);
                dispatch(historyListener, response);
                dispatch(sellerListener, response);
                dispatch(autoBidListener, response);
                dispatch(notificationListener, response);
                dispatch(addItemListener, response);
            }
            default -> System.out.println("Server gui action chua duoc xu ly: " + action);
        }
    }

    // Day du lieu ve JavaFX thread neu listener ton tai.
    private static void dispatch(Consumer<JsonObject> listener, JsonObject response) {
        if (listener == null) {
            return;
        }
        Platform.runLater(() -> listener.accept(response));
    }
}
