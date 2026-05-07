package com.bidding.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;
    private Consumer<JsonObject> messageHandler;

    private static final String HOST = "localhost";
    private static final int PORT = 8080; // Đảm bảo Backend của ông đang chạy đúng cổng này nhé!

    // Ổ KHÓA CHỐNG KẾT NỐI KÉP
    private boolean isConnecting = false;

    private NetworkClient() {
        this.gson = new Gson();
    }

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void connect() {
        // Chặn đứng ngay nếu đã có socket hoặc ĐANG trong quá trình kết nối
        if ((socket != null && !socket.isClosed()) || isConnecting) {
            return;
        }

        isConnecting = true; // Khóa cửa lại

        new Thread(() -> {
            try {
                socket = new Socket(HOST, PORT);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                System.out.println("✅ Đã kết nối tới Server thành công!");
                startListening();
            } catch (IOException e) {
                System.err.println("❌ Lỗi: Không thể kết nối Server! (Check lại xem Backend đã chạy chưa nha)");
            } finally {
                isConnecting = false; // Mở khóa khi đã xử lý xong
            }
        }).start();
    }

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("🔌 Đã ngắt kết nối Server.");
        } catch (IOException e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while (in != null && (line = in.readLine()) != null) {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    if (messageHandler != null) {
                        messageHandler.accept(json);
                    }
                }
            } catch (IOException e) {
                System.err.println("⚠️ Mất kết nối với Server.");
            }
        }).start();
    }

    public void sendJson(JsonObject json) {
        if (out != null) {
            String msg = gson.toJson(json);
            System.out.println(">>> GỬI LÊN SERVER: " + msg);
            out.println(msg);
        } else {
            System.err.println("🚫 Lỗi: Chưa có kết nối để gửi dữ liệu!");
        }
    }

    public void setMessageHandler(Consumer<JsonObject> handler) {
        this.messageHandler = handler;
    }
}