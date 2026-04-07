package com.bidding.server.network;
import com.bidding.server.network.EmailService;
import com.bidding.server.ServerMain;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson(); // Đồ chơi bóc/gói JSON
    private String generateOTP() {
        return String.format("%06d", new java.util.Random().nextInt(999999));
    }
    // Tạm lưu thông tin thằng khách này sau khi nó login thành công
    private String loggedInUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            ServerMain.clients.add(this); // Ghi danh vào sổ phát loa
            System.out.println("Khách kết nối: " + socket.getInetAddress());

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[RAW DATA TỪ CLIENT]: " + clientMessage);

                try {
                    // 1. Biến cục text khô khan thành Object JSON
                    JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                    String action = request.get("action").getAsString();

                    // 2. TỔNG ĐÀI RẼ NHÁNH (API ROUTER)
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
                        case "REQ_OTP":
                            String emailToReset = request.get("email").getAsString();

                            // 1. Đẻ mã OTP ngẫu nhiên
                            String newOtp = generateOTP();

                            // 2. Ghi vào sổ tay Server (Để lát nó gửi lên còn biết đường check)
                            ServerMain.otpStorage.put(emailToReset, newOtp);

                            // 3. Gọi thằng EmailService vác súng ra bắn!
                            boolean isSent = EmailService.sendOTP(emailToReset, newOtp);

                            // 4. Báo cáo lại cho Giao diện (UI) biết tình hình
                            if (isSent) {
                                sendMessage("{\"action\": \"REQ_OTP_REPLY\", \"status\": \"SUCCESS\"}");
                            } else {
                                sendMessage("{\"action\": \"REQ_OTP_REPLY\", \"status\": \"FAIL\", \"message\": \"Lỗi gửi mail!\"}");
                            }
                            break;

                        // CASE 2: Khách hàng gửi mã OTP lên để xác nhận đổi Pass
                        case "RESET_PASS":
                            String email = request.get("email").getAsString();
                            String otpClientGuiLen = request.get("otp").getAsString();
                            String newPassword = request.get("newPass").getAsString();

                            // 1. Lấy mã OTP chuẩn từ trong sổ tay ra
                            String otpChuan = ServerMain.otpStorage.get(email);

                            // 2. Kiểm tra xem mã có khớp không
                            if (otpChuan != null && otpChuan.equals(otpClientGuiLen)) {
                                // Khớp! Cho phép đổi Pass (Bác gọi hàm Update Database ở đây)
                                // Database.updatePassword(email, newPassword); <-- Code ví dụ

                                // Xóa OTP khỏi sổ tay vì đã dùng xong (OTP chỉ dùng 1 lần)
                                ServerMain.otpStorage.remove(email);

                                sendMessage("{\"action\": \"RESET_PASS_REPLY\", \"status\": \"SUCCESS\"}");
                            } else {
                                // Sai OTP hoặc OTP không tồn tại
                                sendMessage("{\"action\": \"RESET_PASS_REPLY\", \"status\": \"FAIL\", \"message\": \"Mã OTP sai hoặc đã hết hạn!\"}");
                            }
                            break;


                        default:
                            sendError("Lệnh action không tồn tại trên Server!");
                    }
                } catch (Exception e) {
                    sendError("Gửi JSON sai format rồi thằng ngu!");
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

        // CHỖ NÀY LẮP CODE CỦA TV4 (DATABASE)
        // boolean isValid = UserDAO.checkLogin(user, pass);
        boolean isValid = true; // Code giả lập tạm thời

        if (isValid) {
            this.loggedInUser = user; // Nhớ mặt thằng này
            sendMessage("{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\"}");
            System.out.println(user + " đã đăng nhập!");
        } else {
            sendMessage("{\"action\": \"LOGIN_REPLY\", \"status\": \"FAIL\", \"message\": \"Sai mật khẩu!\"}");
        }
    }

    private void handleGetItems() {
        // CHỖ NÀY LẮP CODE CỦA TV4/TV1 ĐỂ LẤY DANH SÁCH ĐỒ
        // String jsonItems = ItemDAO.getAllItemsAsJson();
        String fakeData = "[{\"id\":1, \"name\":\"Kiếm rồng\", \"price\":50000}]";
        sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + fakeData + "}");
    }

    private void handleBid(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Chưa đăng nhập mà đòi đấu giá à?");
            return;
        }

        int itemId = request.get("itemId").getAsInt();
        double amount = request.get("amount").getAsDouble();

        // CHỖ NÀY LẮP CODE CỦA TV1 (LOGIC CORE)
        // boolean isOk = AuctionManager.getInstance().placeBid(this.loggedInUser, itemId, amount);
        boolean isOk = true; // Giả lập TV1 bảo OK

        if (isOk) {
            // Nhắn riêng nó là thành công
            sendMessage("{\"action\": \"BID_REPLY\", \"status\": \"SUCCESS\"}");

            // Vác loa phường la làng cho cả Server đổi số tiền
            String broadcastJson = String.format(
                    "{\"action\": \"UPDATE_PRICE\", \"itemId\": %d, \"newPrice\": %.0f, \"highestBidder\": \"%s\"}",
                    itemId, amount, this.loggedInUser
            );
            ServerMain.broadcast(broadcastJson);
        } else {
            sendError("Tiền ít hoặc chậm tay rồi, đứa khác đặt cao hơn!");
        }
    }

    // ================= TIỆN ÍCH GỬI TIN =================

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void sendError(String errorMsg) {
        sendMessage("{\"action\": \"ERROR\", \"message\": \"" + errorMsg + "\"}");
    }

    private void closeEverything() {
        ServerMain.clients.remove(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}