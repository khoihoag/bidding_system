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