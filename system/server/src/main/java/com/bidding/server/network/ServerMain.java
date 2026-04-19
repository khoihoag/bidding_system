package com.bidding.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ServerMain {

    public static void main(String[] args) {
        System.out.println("--- BẮT ĐẦU KHỞI ĐỘNG SERVER ---");

        // =======================================================
        // BƯỚC 1: KHỞI TẠO BỘ SẬU QUẢN LÝ
        // =======================================================
        
        // Tạo Tổng Quản (Nó sẽ tự động chọc DB và nạp RAM ở ngay Constructor)
        AuctionService tongQuan = new AuctionService(); 
        tongQuan.loadAllAuctions(); // Đảm bảo nạp hết hàng lên RAM trước khi mở cửa
        // Tạo Loa Phường (Chuyên giữ danh sách khách và phát JSON)
        ClientManager loaPhuong = new ClientManager();

        // Ký hợp đồng: Ép thằng Loa phường hóng tin từ Tổng quản
        tongQuan.addObserver(loaPhuong);
        UserService baoVe = new UserService();
        // =======================================================
        // BƯỚC 2: MỞ CỬA ĐÓN KHÁCH (Dùng ThreadPool xịn của ông)
        // =======================================================
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("✅ Server đấu giá đã mở cửa ở port 8080. Đang đợi khách...");

            while (true) {
                // Sếp đứng cửa đón khách
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[+] Giang hồ mạng mới vào: " + clientSocket.getInetAddress());

                // THUÊ NHÂN VIÊN: Dúi vào tay nó cái Card Visit của Tổng quản
                // Chú ý: constructor của ClientHandler phải có 2 tham số này
                // Dúi vào tay nhân viên cả thẻ Tổng quản và thẻ Loa phường
                ClientHandler nhanVien = new ClientHandler(clientSocket, tongQuan, loaPhuong, baoVe);

                // ĐĂNG KÝ QUẢN LÝ: Ném nhân viên này cho Loa phường quản lý
                // Để lỡ có biến gì thì Loa phường còn biết đường gửi JSON cho nó
                loaPhuong.addClient(nhanVien);

                // Giao việc cho nhân viên, ném vào Thread Pool chạy
                pool.execute(nhanVien);
            }
        } catch (IOException e) {
            System.err.println("❌ Cổng 8080 bị kẹt cmnr: " + e.getMessage());
            e.printStackTrace();
        }
    }
}