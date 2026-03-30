import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static Gson gson = new Gson();
    private static AuctionRoomController auctionController;
    public static void setAuctionController(AuctionRoomController controller) {
        auctionController = controller;
    }
    // ================= 1. HÀM MỞ KẾT NỐI (GỌI LÚC BẬT APP) =================
    public static void connect(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("✅ Đã cắm ống nước tới Server!");

            // Bật ngay cái "Lỗ tai" chạy ngầm để nghe Server la làng
            startListening();

        } catch (IOException e) {
            System.err.println("❌ Lỗi mạng: Không tìm thấy Server. Bật Server chưa ba?");
        }
    }

    // ================= 2. HÀM GỬI LỆNH LÊN SERVER (CÁI MIỆNG) =================
    // Thằng TV3 sẽ gọi cái này. VD: NetworkClient.send("{\"action\":\"GET_ITEMS\"}");
    public static void send(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        } else {
            System.err.println("Chưa connect mà đòi gửi tin hả?");
        }
    }

    // ================= 3. LỖ TAI CHẠY NGẦM (QUAN TRỌNG NHẤT) =================
    private static void startListening() {
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("[SERVER BÁO VỀ]: " + serverMessage);
                    
                    try {
                        // Bóc JSON ra để xem Server muốn gì
                        JsonObject response = JsonParser.parseString(serverMessage).getAsJsonObject();
                        String action = response.get("action").getAsString();

                        // ĐIỀU HƯỚNG SỰ KIỆN GIAO DIỆN Ở ĐÂY
                        handleServerAction(action, response);

                    } catch (Exception e) {
                        System.err.println("Server gửi JSON rác!");
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Mất kết nối cmnr!");
            }
        }).start(); // Bật chạy song song, không làm đơ giao diện
    }

    // ================= 4. ROUTER XỬ LÝ LỆNH TỪ SERVER =================
    private static void handleServerAction(String action, JsonObject response) {
        // GHI CHÚ CHỮ TO CHO THẰNG TV3:
        // MỌI CODE ĐỔI GIAO DIỆN (setText, setVisible...) BẮT BUỘC PHẢI BỌC TRONG:
        // Platform.runLater(() -> { ... });

        switch (action) {
            case "LOGIN_REPLY":
                String status = response.get("status").getAsString();
                if (status.equals("SUCCESS")) {
                    System.out.println("Đăng nhập thành công! Chuyển sang màn hình Home đi.");
                    // TODO (TV3): Platform.runLater(() -> ChuyenManHinh());
                } else {
                    System.out.println("Lỗi: " + response.get("message").getAsString());
                    // TODO (TV3): Platform.runLater(() -> HienThongBaoLoi());
                }
                break;

            case "ITEMS_LIST":
                JsonArray items = response.get("items").getAsJsonArray();
                System.out.println("Đã nhận danh sách: " + items.size() + " món đồ.");
                // TODO (TV3): Platform.runLater(() -> DoDuLieuVaoBang(items));
                break;

            case "UPDATE_PRICE": // Bắt sóng loa phường
                int id = response.get("itemId").getAsInt();
                double price = response.get("newPrice").getAsDouble();
                String winner = response.get("highestBidder").getAsString();
                System.out.println("Đồ ID " + id + " vừa lên giá " + price + " (Bởi: " + winner + ")");
                if (auctionController != null) {
                    auctionController.updatePriceOnScreen(price, winner);
                }
                // TODO (TV3): Platform.runLater(() -> NhaySoTienTrenManHinh(price));
                break;

            case "ERROR":
                System.err.println("SERVER CHỬI: " + response.get("message").getAsString());
                // TODO (TV3): Platform.runLater(() -> ShowAlert(Loi));
                break;

            case "AUCTION_END":
                System.out.println("HẾT GIỜ! Món đồ " + response.get("itemId").getAsInt() + " đã chốt!");
                // TODO (TV3): Platform.runLater(() -> KhoaNutBid_HienNguoiThang());
                break;

            default:
                System.out.println("Lệnh lạ từ Server: " + action);
        }
    }
}