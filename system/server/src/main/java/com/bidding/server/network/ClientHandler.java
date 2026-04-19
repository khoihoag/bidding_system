package com.bidding.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();

    // BIẾN QUYỀN LỰC
    private User loggedInUser = null;

    private AuctionService tongQuan;
    private ClientManager loaPhuong;
    private UserService baoVe;

    public ClientHandler(Socket socket, AuctionService tongQuan, ClientManager loaPhuong, UserService baove) {
        this.socket = socket;
        this.tongQuan = tongQuan;
        this.loaPhuong = loaPhuong;
        this.baoVe = baove;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Khách kết nối: " + socket.getInetAddress());

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[RAW DATA TỪ CLIENT]: " + clientMessage);

                try {
                    JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                    String action = request.get("action").getAsString();

                    // =======================================================
                    // BỨC TƯỜNG LỬA (MIDDLEWARE CHECK AUTH)
                    // =======================================================
                    // Danh sách các lệnh không cần đăng nhập (Whitelist)
                    // Sau này có lệnh REGISTER (Đăng ký) thì ông nhét thêm vào đây
                    boolean requiresAuth = !action.equals("LOGIN");

                    if (requiresAuth && this.loggedInUser == null) {
                        sendError("Bạn chưa đăng nhập! Vui lòng đăng nhập để thực hiện lệnh: " + action);
                        continue; // BẬT XỚI: Không cho đi tiếp vào Switch-Case, bắt vòng lại chờ tin nhắn mới
                    }
                    // =======================================================

                    switch (action) {
                        case "LOGIN":
                            handleLogin(request);
                            break;
                        case "GET_ITEMS":
                            handleGetItems();
                            break;
                        case "BID":
                            handleBid(request);
                            break;
                        case "HISTORY":
                            handleGetHistory();
                            break;
                        default:
                            sendError("Lệnh action không tồn tại trên Server!");
                    }
                } catch (Exception e) {
                    sendError("Gửi JSON sai format hoặc lỗi Server!");
                }
            }
        } catch (IOException e) {
            System.out.println("Khách rớt mạng.");
        } finally {
            closeEverything();
        }
    }

    // ================= CÁC HÀM XỬ LÝ (CONTROLLERS) CHUẨN SẠCH =================

    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        try {
            this.loggedInUser = baoVe.login(user, pass);
            sendMessage("{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\", \"myId\": \"" + loggedInUser.getId() + "\"}");
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    private void handleGetHistory() {
        // KHÔNG CẦN IF CHECK LOGIN Ở ĐÂY NỮA, BỨC TƯỜNG LỬA ĐÃ LO
        List<BiddingTransactionEntity> history = baoVe.getBidHistory(this.loggedInUser.getId());
        String jsonHistory = gson.toJson(history);
        sendMessage("{\"action\": \"HISTORY_REPLY\", \"data\": " + jsonHistory + "}");
    }

    private void handleGetItems() {
        // KHÔNG CẦN IF CHECK LOGIN Ở ĐÂY NỮA
        // TODO: Chờ thằng Khôi làm xong ItemRepository thì nhét hàm lấy Item vào đây
        String fakeData = "[{\"id\":\"item-123\", \"name\":\"Kiếm rồng\", \"price\":50000}]";
        sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + fakeData + "}");
    }

    private void handleBid(JsonObject request) {
        // KHÔNG CẦN IF CHECK LOGIN Ở ĐÂY NỮA
        String itemId = request.get("itemId").getAsString();
        double amount = request.get("amount").getAsDouble();

        try {
            tongQuan.placeBid(itemId, amount, this.loggedInUser);
            sendMessage("{\"action\": \"BID_REPLY\", \"status\": \"SUCCESS\"}");
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    // ================= TIỆN ÍCH =================

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void sendError(String errorMsg) {
        sendMessage("{\"action\": \"ERROR\", \"message\": \"" + errorMsg + "\"}");
    }

    private void closeEverything() {
        if (loaPhuong != null) {
            loaPhuong.removeClient(this);
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}