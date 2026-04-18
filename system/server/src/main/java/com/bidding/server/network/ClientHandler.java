package com.bidding.server.network;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bidding.server.model.user.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.bidding.server.network.AuctionService;
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();


    private String loggedInUser = null;

    // =========================================================
    // 1. CHÌA KHÓA QUYỀN LỰC: CẤP THẺ TỔNG QUẢN VÀ LOA PHƯỜNG
    // =========================================================
    private AuctionService tongQuan;
    private ClientManager loaPhuong;

    public ClientHandler(Socket socket, AuctionService tongQuan, ClientManager loaPhuong) {
        this.socket = socket;
        this.tongQuan = tongQuan;
        this.loaPhuong = loaPhuong;
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

    // ================= CÁC HÀM XỬ LÝ (CONTROLLERS) =================

    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();
        boolean isValid = true; // Code giả lập

        if (isValid) {
            this.loggedInUser = user;
            sendMessage("{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\"}");
        } else {
            sendMessage("{\"action\": \"LOGIN_REPLY\", \"status\": \"FAIL\", \"message\": \"Sai mật khẩu!\"}");
        }
    }

    private void handleGetItems() {
        String fakeData = "[{\"id\":1, \"name\":\"Kiếm rồng\", \"price\":50000}]";
        sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + fakeData + "}");
    }

    // =========================================================
    // 2. SỰ KỲ DIỆU CỦA KIẾN TRÚC: HÀM BID GIỜ CỰC KỲ GỌN NHẸ
    // =========================================================
    private void handleBid(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Chưa đăng nhập mà đòi đấu giá à?");
            return;
        }

        // FIX 1: ID bây giờ là String, nên phải dùng getAsString()
        String itemId = request.get("itemId").getAsString();
        double amount = request.get("amount").getAsDouble();

        // FIX 2: Thay Bidder bằng User, và truyền ID kiểu String
        // TODO: Sau này lấy thông tin thật từ Database dựa trên loggedInUser
        User currentUser = new User();
        currentUser.setId("TEMP-USER-123"); // Ép kiểu String cho khớp chuẩn mới
        currentUser.setUsername(this.loggedInUser);

        try {
            // Gọi hàm placeBid chuẩn với 3 tham số
            tongQuan.placeBid(itemId, amount, currentUser);

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
        // 3. KHI KHÁCH SỦI, TỰ ĐỘNG BÁO LOA PHƯỜNG GẠCH TÊN KHỎI SỔ
        if (loaPhuong != null) {
            loaPhuong.removeClient(this);
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}