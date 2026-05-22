package com.bidding.server.network;

import com.bidding.server.controller.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bidding.server.model.user.User;
import com.bidding.server.service.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientManager loaPhuong;

    // BIẾN QUYỀN LỰC
    private User loggedInUser = null;

    // CÁC ĐỆ TỬ (CONTROLLERS)
    private AuthController authController;
    private ItemController itemController;
    private AuctionController auctionController;
    private AdminController adminController;

    public ClientHandler(Socket socket, AuctionService tongQuan, ClientManager loaPhuong, UserService baoVe, ItemService quanLyKho, AdminService adminService) {
        this.socket = socket;
        this.loaPhuong = loaPhuong;

        // Cấp phát vũ khí cho các đệ tử
        this.authController = new AuthController(this, baoVe);
        this.itemController = new ItemController(this, quanLyKho, tongQuan);
        this.auctionController = new AuctionController(this, tongQuan, quanLyKho, loaPhuong, baoVe);
        this.adminController = new AdminController(this, adminService, tongQuan, loaPhuong);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            System.out.println("Khách kết nối: " + socket.getInetAddress());

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[RAW DATA TỪ CLIENT]: " + clientMessage);
                try {
                    JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                    String action = request.get("action").getAsString();

                    // MENU CHUYỂN MẠCH THUẦN LOGIC
                    switch (action) {
                        // Nhóm Auth
                        case "LOGIN": authController.handleLogin(request); break;
                        case "REGISTER": authController.handleRegister(request); break;
                        case "DEPOSIT": authController.handleDeposit(request);break;
                        // Nhóm Kho Đồ (Item)
                        case "GET_ITEMS": itemController.handleGetItems(); break;
                        case "ADD_ITEM": itemController.handleAddItem(request); break;
                        case "UPDATE_ITEM": itemController.handleUpdateItem(request); break;
                        case "DELETE_ITEM": itemController.handleDeleteItem(request); break;
                        case "GET_AI_PRICE": itemController.handleGetAIPrice(request); break;
                        // Nhóm Đấu Giá (Auction)
                        case "START_AUCTION": auctionController.handleStartAuction(request); break;
                        case "SCHEDULE_AUCTION": auctionController.handleScheduleAuction(request); break;
                        case "GET_AUCTIONS": auctionController.handleGetAuctions(); break;
                        case "BID": auctionController.handleBid(request); break;
                        case "REGISTER_AUTO_BID": auctionController.handleRegisterAutoBid(request);break;
                        //lich su ca nhan
                        case "GET_HISTORY": auctionController.handleGetHistory(); break;
                        // Thêm vào switch trong ClientHandler.java
                        //lich su de vẽ biểu đồ
                        case "GET_AUCTION_HISTORY": auctionController.handleGetAuctionHistory(request);break;
                        case "GET_WON_ITEMS": itemController.handleGetWonItems();break;

                        // Nhóm Quản Trị (Admin)
                        case "GET_ALL_USERS": adminController.handleGetAllUsers(); break;
                        case "FORCE_CLOSE": adminController.handleForceClose(request); break;
                        case "BAN_USER": adminController.handleBanUser(request); break;
                        case "UNBAN_USER": adminController.handleUnbanUser(request); break;
                        case "GET_PENDING_ITEMS": adminController.handleGetPendingItems(); break;
                        case "APPROVE_ITEM": adminController.handleApproveItem(request); break;
                        case "REJECT_ITEM": adminController.handleRejectItem(request); break;
                        case "CREATE_ADMIN_LEVEL1": adminController.handleCreateAdminLevel1(request); break;

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

    // ================== CÁC HÀM PUBLIC CHO CONTROLLER XÀI KÉ ==================
    public User getLoggedInUser() { return loggedInUser; }
    public void setLoggedInUser(User loggedInUser) { this.loggedInUser = loggedInUser; }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public void sendError(String errorMsg) {
        // Tự động xử lý ký tự ngoặc kép (") để không bao giờ bị gãy JSON
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("action", "ERROR");
        errorJson.addProperty("message", errorMsg != null ? errorMsg : "Lỗi không xác định");
        sendMessage(errorJson.toString());
    }

    private void closeEverything() {
        if (loaPhuong != null) loaPhuong.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
