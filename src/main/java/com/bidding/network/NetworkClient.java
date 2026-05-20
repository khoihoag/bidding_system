package com.bidding.network;

import com.bidding.controller.AppNavigator;
import com.bidding.model.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NetworkClient {
    private static NetworkClient instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    private Consumer<JsonObject> messageHandler;
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

                System.out.println("Da ket noi toi server.");
                flushPendingMessages();
                startListening();
            } catch (IOException e) {
                System.err.println("Khong the ket noi server: " + e.getMessage());
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
            System.err.println("Loi khi ngat ket noi: " + e.getMessage());
        } finally {
            out = null;
            in = null;
            socket = null;
            System.out.println("Da ngat ket noi server.");
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
                    System.out.println("<<< NHAN TU SERVER: " + line);
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    if (isBanNotice(json)) {
                        handleBanNotice(json);
                        continue;
                    }
                    Consumer<JsonObject> handler = messageHandler;
                    if (handler != null) {
                        handler.accept(json);
                    }
                }
            } catch (IOException e) {
                System.err.println("Mat ket noi voi server: " + e.getMessage());
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

    private boolean isBanNotice(JsonObject json) {
        return json.has("action")
                && !json.get("action").isJsonNull()
                && "YOU_ARE_BANNED".equals(json.get("action").getAsString());
    }

    private void handleBanNotice(JsonObject json) {
        String message = json.has("message") && !json.get("message").isJsonNull()
                ? json.get("message").getAsString()
                : "Tai khoan cua ban da bi khoa boi Admin.";

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Tai khoan bi khoa");
            alert.setHeaderText("Ban da bi dang xuat");
            alert.setContentText(message + "\n\nHe thong se dua ban ve man hinh dang nhap sau vai giay.");
            alert.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(event -> {
                alert.close();
                UserSession.getInstance().clear();
                disconnect();
                AppNavigator.navigate("Login.fxml");
            });
            delay.play();
        });
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
