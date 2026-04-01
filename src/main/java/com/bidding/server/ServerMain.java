import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    // Sổ tay lưu OTP: Chìa khóa là Email, Giá trị là Mã OTP
// Dùng ConcurrentHashMap để chống Race Condition khi nhiều thằng cùng xin OTP 1 lúc
    public static java.util.concurrent.ConcurrentHashMap<String, String> otpStorage = new java.util.concurrent.ConcurrentHashMap<>();
    // 1. ĐỂ Ở ĐÂY: Cuốn sổ tay toàn cục (nằm TRONG class, nhưng NGOÀI hàm main)
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // 2. ĐỂ Ở ĐÂY: Hàm Loa Phường (nằm TRONG class, nhưng NGOÀI hàm main)
    public static void broadcast(String message) {
        System.out.println("[BROADCAST TOÀN SERVER]: " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message); // Gọi hàm sendMessage của từng khách
        }
    }

    // 3. Hàm main: Nơi chương trình bắt đầu chạy
    public static void main(String[] args) {
        // Tạo một "Hồ chứa luồng" tự động co giãn theo số lượng khách
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server đấu giá đang chạy ở port 8080...");

            while (true) {
                // Sếp đứng cửa đón khách
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Khách hàng mới đã kết nối: " + clientSocket.getInetAddress());

                // Giao khách cho nhân viên phục vụ, ném vào Thread Pool
                pool.execute(new ClientHandler(clientSocket)); 
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}