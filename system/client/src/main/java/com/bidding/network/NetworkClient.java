package com.bidding.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class NetworkClient {
    private static NetworkClient instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    private Consumer<JsonObject> messageHandler;
    private final List<Consumer<JsonObject>> globalMessageListeners = new CopyOnWriteArrayList<>();
    private final List<JsonObject> pendingMessages = new ArrayList<>();

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    private boolean isConnecting = false;

    private NetworkClient() {
    }

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public synchronized void connect() {
        if (isConnected() || isConnecting) {
            return;
        }

        isConnecting = true;
        new Thread(() -> {
            try {
                socket = new Socket(HOST, PORT);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                System.out.println("Đã kết nối tới server.");
                flushPendingMessages();
                startListening();
            } catch (IOException e) {
                System.err.println("Không thể kết nối server: " + e.getMessage());
            } finally {
                isConnecting = false;
            }
        }, "bidding-network-connect").start();
    }

    public synchronized void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        } finally {
            out = null;
            in = null;
            socket = null;
            System.out.println("Đã ngắt kết nối server.");
        }
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while (in != null && (line = in.readLine()) != null) {
                    System.out.println("<<< NHẬN TỪ SERVER: " + line);
                    try {
                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        for (Consumer<JsonObject> listener : globalMessageListeners) {
                            listener.accept(json);
                        }
                        Consumer<JsonObject> handler = messageHandler;
                        if (handler != null) {
                            handler.accept(json);
                        }
                    } catch (Exception parseError) {
                        System.err.println("Không parse được JSON từ server: " + parseError.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Mất kết nối với server: " + e.getMessage());
            } finally {
                disconnect();
            }
        }, "bidding-network-listener").start();
    }

    public void sendJson(JsonObject json) {
        if (!isConnected()) {
            synchronized (pendingMessages) {
                pendingMessages.add(json.deepCopy());
            }
            connect();
            System.out.println(">>> XEP HANG GUI SERVER: " + gson.toJson(json));
            return;
        }

        sendNow(json);
    }

    public void setMessageHandler(Consumer<JsonObject> handler) {
        this.messageHandler = handler;
    }

    public void addGlobalMessageListener(Consumer<JsonObject> listener) {
        if (listener != null && !globalMessageListeners.contains(listener)) {
            globalMessageListeners.add(listener);
        }
    }

    public void removeGlobalMessageListener(Consumer<JsonObject> listener) {
        globalMessageListeners.remove(listener);
    }

    private void flushPendingMessages() {
        synchronized (pendingMessages) {
            for (JsonObject json : pendingMessages) {
                sendNow(json);
            }
            pendingMessages.clear();
        }
    }

    private void sendNow(JsonObject json) {
        String msg = gson.toJson(json);
        System.out.println(">>> GUI LEN SERVER: " + msg);
        out.println(msg);
    }
}
