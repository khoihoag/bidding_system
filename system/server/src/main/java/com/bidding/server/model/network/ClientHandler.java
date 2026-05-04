package com.bidding.server.model.network;
import com.bidding.server.model.item.Art;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bidding.server.model.user.User;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.service.UserService;
import com.bidding.server.service.AuctionService;
import com.bidding.server.model.auction.Auction; // Nhớ import cái này
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import com.bidding.server.model.item.Item;
import com.bidding.server.service.ItemService;
import com.bidding.server.service.AdminService;
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();
    private AdminService adminService;
    // BIẾN QUYỀN LỰC
    private User loggedInUser = null;

    private AuctionService tongQuan;
    private ClientManager loaPhuong;
    private UserService baoVe;
    private ItemService quanLyKho;

    public ClientHandler(Socket socket, AuctionService tongQuan, ClientManager loaPhuong, UserService baoVe, ItemService quanLyKho, AdminService adminService) {
        this.socket = socket;
        this.tongQuan = tongQuan;
        this.loaPhuong = loaPhuong;
        this.baoVe = baoVe;
        this.quanLyKho = quanLyKho;
        this.adminService = adminService; // Gán quyền năng Admin
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

                    // MENU CHUYỂN MẠCH THUẦN LOGIC (Đã đập bỏ Tường lửa)
                    switch (action) {
                        case "GET_ALL_USERS":
                            handleGetAllUsers();
                            break;
                        case "START_AUCTION":
                            handleStartAuction(request);
                            break;
                        case "FORCE_CLOSE":
                            handleForceClose(request);
                            break;
                        case "BAN_USER":
                            handleBanUser(request);
                            break;
                        case "DELETE_ITEM":
                            handleDeleteItem(request);
                            break;
                        case "UPDATE_ITEM":
                            handleUpdateItem(request);
                            break;
                        case "ADD_ITEM":
                            handleAddItem(request);
                            break;
                        case "REGISTER":
                            handleRegister(request);
                            break;
                        case "LOGIN":
                            handleLogin(request);
                            break;
                        case "GET_AUCTIONS": // LUỒNG MỚI: Lấy danh sách đang đấu giá
                            handleGetAuctions();
                            break;
                        case "GET_ITEMS":
                            handleGetItems();
                            break;
                        case "GET_ALL_ITEMS": // Lấy tất cả item trên sàn (cho Buyer)
                            handleGetAllItems();
                            break;
                        case "SCHEDULE_AUCTION": // Lên lịch đấu giá
                            handleScheduleAuction(request);
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
                    System.err.println("[SERVER ERROR] Khong xu ly duoc request: " + clientMessage);
                    e.printStackTrace();
                    sendError("Server loi khi xu ly request: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Khách rớt mạng.");
        } finally {
            closeEverything();
        }
    }

    // ================= CÁC HÀM XỬ LÝ (CONTROLLERS) =================

    private void handleRegister(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();
        String email = request.get("email").getAsString();
        String fullName = request.get("fullName").getAsString();

        try {
            baoVe.register(user, pass, email, fullName);
            sendMessage("{\"action\": \"REGISTER_REPLY\", \"status\": \"SUCCESS\"}");
        } catch (Exception e) {
            sendError("Lỗi khi tạo tài khoản: " + e.getMessage());
        }
    }

    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        try {
            this.loggedInUser = baoVe.login(user, pass);
            sendMessage(String.format(
                    "{\"action\": \"LOGIN_REPLY\", \"status\": \"SUCCESS\", \"myId\": \"%s\", \"role\": \"%s\"}",
                    loggedInUser.getId(),
                    loggedInUser.getRole().name()
            ));
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    private void handleGetAuctions() {
        try {
            // Xin Tổng quản danh sách các phiên đang chạy trên RAM
            List<Auction> activeAuctions = tongQuan.getActiveAuctions();
            sendMessage("{\"action\": \"AUCTIONS_LIST\", \"data\": " + buildAuctionsJson(activeAuctions) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Loi khi tai danh sach dau gia: " + e.getMessage());
        }
    }

    // Tra toan bo items tren san cho Buyer (khong loc theo seller)
    private void handleGetAllItems() {
        try {
            List<Item> allItems = quanLyKho.getAllItems();
            sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + buildItemsJson(allItems) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Loi khi tai toan bo san pham: " + e.getMessage());
        }
    }

    // Len lich dau gia: tao Auction voi startTime/endTime cu the
    private void handleScheduleAuction(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Phai dang nhap moi duoc len lich dau gia!");
            return;
        }
        try {
            String itemId    = request.get("itemId").getAsString();
            String startTime = request.get("startTime").getAsString();
            String endTime   = request.get("endTime").getAsString();

            Item item = quanLyKho.findById(itemId);
            if (item == null) { sendError("Khong tim thay san pham!"); return; }
            if (!item.getSeller().getId().equals(this.loggedInUser.getId())) {
                sendError("Ban khong the dua do cua nguoi khac len lich!"); return;
            }

            // Parse startTime / endTime (ISO: yyyy-MM-ddTHH:mm:ss hoac co space)
            java.time.LocalDateTime dtStart = parseDateTime(startTime);
            java.time.LocalDateTime dtEnd   = parseDateTime(endTime);
            if (dtEnd == null || dtStart == null) {
                sendError("Dinh dang thoi gian khong hop le. Dung: yyyy-MM-ddTHH:mm:ss"); return;
            }
            if (!dtEnd.isAfter(dtStart)) {
                sendError("Thoi gian ket thuc phai sau thoi gian bat dau!"); return;
            }

            Auction newAuction = tongQuan.scheduleAuction(item, dtStart, dtEnd);
            sendMessage("{\"action\": \"SCHEDULE_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");

            String notifyJson = String.format(
                    "{\"action\": \"GLOBAL_NOTIFY\", \"message\": \"Da len lich dau gia cho: %s!\"}",
                    item.getName());
            loaPhuong.broadcast(notifyJson);
        } catch (Exception e) {
            sendError("Loi khi len lich dau gia: " + e.getMessage());
        }
    }

    private java.time.LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return java.time.LocalDateTime.parse(s.replace(" ", "T"),
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) { return null; }
    }

    private void handleGetHistory() {
        if (this.loggedInUser == null) {
            sendError("Vui lòng đăng nhập để xem lịch sử!");
            return;
        }
        List<BiddingTransactionEntity> history = baoVe.getBidHistory(this.loggedInUser.getId());
        String jsonHistory = gson.toJson(history);
        sendMessage("{\"action\": \"HISTORY_REPLY\", \"data\": " + jsonHistory + "}");
    }

    private void handleGetItems() {
        if (this.loggedInUser == null) {
            sendError("Vui lòng đăng nhập để xem kho đồ!");
            return;
        }
        try {
            List<Item> myItems = quanLyKho.getMyItems(this.loggedInUser);
            sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + buildItemsJson(myItems) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Loi khi tai danh sach san pham: " + e.getMessage());
        }
    }

    private void handleBid(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Chưa đăng nhập mà đòi đấu giá à?");
            return;
        }
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

    private void sendMessage(JsonObject jsonObject) {
        if (out != null) out.println(jsonObject.toString());
    }

    private void sendError(String errorMsg) {
        JsonObject error = new JsonObject();        
        error.addProperty("action", "ERROR");
        error.addProperty("message", errorMsg);
        sendMessage(error);
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
    private void handleUpdateItem(JsonObject request) {
        // 1. Check giấy tờ
        if (this.loggedInUser == null) {
            sendError("Phải đăng nhập mới được sửa đồ!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();

            // 2. LOGIC QUAN TRỌNG: Check xem hàng có đang trên sàn không
            if (tongQuan.isItemInActiveAuction(itemId)) {
                sendError("Món hàng này đang được đấu giá, không được phép sửa đổi thông tin!");
                return;
            }

            // 3. Chuẩn bị dữ liệu mới (Ví dụ sửa tên và mô tả)
            // Tận dụng ItemService đã có
            String newName = request.get("name").getAsString();
            String newDesc = request.get("description").getAsString();

            // Tạo một object tạm chứa thông tin cần update
            // (Chỉ cần name và description để ItemService copy qua)
            Item updateData = new com.bidding.server.model.item.Art(); // Class nào cũng được vì chỉ lấy name/desc
            updateData.setName(newName);
            updateData.setDescription(newDesc);

            // 4. Gọi Quản Lý Kho thực hiện cập nhật
            // ItemService sẽ tự check xem thằng đang login có phải chủ món đồ không
            quanLyKho.updateItem(this.loggedInUser, itemId, updateData);

            sendMessage("{\"action\": \"UPDATE_ITEM_REPLY\", \"status\": \"SUCCESS\"}");

        } catch (SecurityException se) {
            sendError("Lỗi bảo mật: " + se.getMessage()); // Ví dụ: Sửa đồ của người khác
        } catch (Exception e) {
            sendError("Lỗi hệ thống: " + e.getMessage());
        }
    }
    private void handleAddItem(JsonObject request) {
        // 1. Chặn cửa bọn chưa login
        if (this.loggedInUser == null) {
            sendError("Phải đăng nhập mới được đăng bán đồ chứ!");
            return;
        }

        try {
            // 2. Bóc tách các thông tin cơ bản (Món nào cũng phải có)
            String name = request.get("name").getAsString();
            String desc = request.get("description").getAsString();
            double price = request.get("startingPrice").getAsDouble();
            String type = request.get("type").getAsString(); // Chìa khóa phân loại: Art, Vehicle, Electronics

            // Xử lý tình trạng đồ (Nếu client không gửi thì gán mặc định là null hoặc NEW)
            com.bidding.server.enums.ItemCondition condition = null;
            if (request.has("condition")) {
                condition = com.bidding.server.enums.ItemCondition.valueOf(request.get("condition").getAsString());
            }

            Item newItem = null;

            // 3. Phân loại và dùng hàm khởi tạo đầy đủ (Truyền ID = null để nó tự sinh UUID)
            switch (type) {
                case "Art":
                    String artist = request.get("artist").getAsString();
                    String medium = request.get("medium").getAsString();
                    int yearCreated = request.get("yearCreated").getAsInt();
                    String dimensions = request.get("dimensions").getAsString();

                    newItem = new com.bidding.server.model.item.Art(
                            null, name, desc, price, readImages(request), this.loggedInUser, condition,
                            artist, medium, yearCreated, dimensions
                    );
                    break;

                case "Electronics":
                    String brand = request.get("brand").getAsString();
                    String modelE = request.get("model").getAsString();
                    int warranty = request.get("warrantyMonths").getAsInt();
                    int watts = request.get("powerWatts").getAsInt();

                    newItem = new com.bidding.server.model.item.Electronics(
                            null, name, desc, price, readImages(request), this.loggedInUser, condition,
                            brand, modelE, warranty, watts
                    );
                    break;

                case "Vehicle":
                    String make = request.get("make").getAsString();
                    String modelV = request.get("model").getAsString();
                    int yearV = request.get("year").getAsInt();
                    int mileage = request.get("mileage").getAsInt();
                    String fuelType = request.get("fuelType").getAsString();

                    newItem = new com.bidding.server.model.item.Vehicle(
                            null, name, desc, price, readImages(request), this.loggedInUser, condition,
                            make, modelV, yearV, mileage, fuelType
                    );
                    break;

                default:

                    sendError("Loại mặt hàng không hợp lệ! Hệ thống chỉ nhận Art, Electronics, hoặc Vehicle.");
                    return;
            }

            // 4. Quăng cho ông Quản Lý Kho cất xuống Database
            quanLyKho.createItem(this.loggedInUser, newItem);

            // 5. Báo tin vui về cho khách
            sendMessage("{\"action\": \"ADD_ITEM_REPLY\", \"status\": \"SUCCESS\"}");

        } catch (Exception e) {
            e.printStackTrace(); // In ra console để dev dễ debug
            sendError("Lỗi khi đăng bán vật phẩm: Kiểm tra lại các trường JSON đã gửi đủ chưa. " + e.getMessage());
        }
    }
    private List<String> readImages(JsonObject request) {
        List<String> images = new ArrayList<>();
        if (!request.has("images") || !request.get("images").isJsonArray()) {
            return images;
        }

        for (JsonElement image : request.getAsJsonArray("images")) {
            if (!image.isJsonNull()) {
                images.add(image.getAsString());
            }
        }
        return images;
    }

    private String buildItemsJson(List<Item> items) {
        JsonArray array = new JsonArray();
        for (Item item : items) {
            array.add(buildItemJson(item));
        }
        return array.toString();
    }

    private String buildAuctionsJson(List<Auction> auctions) {
        JsonArray array = new JsonArray();
        for (Auction auction : auctions) {
            JsonObject json = new JsonObject();
            json.addProperty("id", auction.getId());
            json.addProperty("auctionId", auction.getId());
            json.addProperty("status", auction.getStatus() == null ? "" : auction.getStatus().name());
            json.addProperty("startTime", auction.getStartTime() == null ? "" : auction.getStartTime().toString());
            json.addProperty("endTime", auction.getEndTime() == null ? "" : auction.getEndTime().toString());
            json.addProperty("currentPrice", auction.getCurrentPrice() == null ? 0.0 : auction.getCurrentPrice().get());
            json.addProperty("winnerId", auction.getCurrentWinner() == null ? "" : auction.getCurrentWinner().getUsername());

            Item item = auction.getItem();
            if (item != null) {
                json.addProperty("itemId", item.getId());
                json.add("item", buildItemJson(item));
            }
            array.add(json);
        }
        return array.toString();
    }

    private JsonObject buildItemJson(Item item) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item.getId());
        json.addProperty("itemId", item.getId());
        json.addProperty("name", item.getName());
        json.addProperty("description", item.getDescription());
        json.addProperty("startingPrice", item.getStartingPrice());
        json.addProperty("type", item.getCategory());
        json.addProperty("category", item.getCategory());
        json.addProperty("item_type", item.getCategory());
        json.addProperty("seller", item.getSeller() == null ? "" : item.getSeller().getUsername());
        json.addProperty("condition", item.getCondition() == null ? "" : item.getCondition().name());

        JsonArray images = new JsonArray();
        if (item.getImages() != null) {
            for (String image : item.getImages()) {
                images.add(image);
            }
        }
        json.add("images", images);

        JsonObject specs = new JsonObject();
        item.getSpecifications().forEach(specs::addProperty);
        json.add("specifications", specs);
        return json;
    }

    private void handleDeleteItem(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Đăng nhập đi rồi mới cho xóa đồ!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();

            // 1. Kiểm tra xem đồ có đang trên bục đấu giá không
            if (tongQuan.isItemInActiveAuction(itemId)) {
                sendError("Đồ đang đấu giá, xóa là ăn phạt đấy! Không cho xóa.");
                return;
            }

            // 2. Nhờ ông Quản lý kho thực hiện lệnh xóa
            boolean success = quanLyKho.deleteItem(this.loggedInUser, itemId);

            if (success) {
                sendMessage("{\"action\": \"DELETE_ITEM_REPLY\", \"status\": \"SUCCESS\"}");
            } else {
                sendError("Không tìm thấy món đồ này trong kho.");
            }

        } catch (SecurityException se) {
            sendError("Đồ của người ta mà ông đòi xóa à? Quên đi!");
        } catch (Exception e) {
            sendError("Lỗi khi xóa đồ: " + e.getMessage());

        }
    }
    private void handleBanUser(JsonObject request) {
        // 1. Kiểm tra xem người đang gọi lệnh có online không
        if (this.loggedInUser == null) {
            sendError("Bạn phải đăng nhập tài khoản Admin!");
            return;
        }

        try {
            // 2. Lấy ID thằng "số hưởng" sắp bị khóa mõm
            String targetUserId = request.get("targetUserId").getAsString();

            // 3. Gọi AdminService xử lý
            // Bên trong AdminService.banUser đã có hàm requireAdmin(actor) để check role rồi
            boolean success = adminService.banUser(this.loggedInUser, targetUserId);

            if (success) {
                sendMessage("{\"action\": \"BAN_USER_REPLY\", \"status\": \"SUCCESS\", \"message\": \"Đã khóa tài khoản người dùng thành công!\"}");

                // (Tùy chọn) Nếu muốn gắt hơn, ông có thể ra lệnh cho Loa Phường đá thằng bị ban ra khỏi server ngay lập tức
                System.out.println("[Admin] " + loggedInUser.getUsername() + " đã thực thi lệnh cấm với ID: " + targetUserId);
            } else {
                sendError("Không tìm thấy người dùng có ID này.");
            }

        } catch (SecurityException se) {
            // Nếu một thằng User thường cố tình gửi lệnh này, nó sẽ bị văng lỗi ở đây
            sendError("Cảnh báo: Bạn không có quyền quản trị viên để thực hiện lệnh này!");
        } catch (RuntimeException re) {
            sendError(re.getMessage()); // Ví dụ: "Không thể tự khóa chính mình"
        } catch (Exception e) {
            sendError("Lỗi hệ thống khi thực hiện lệnh cấm: " + e.getMessage());
        }
    }
    private void handleForceClose(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Admin chưa đăng nhập!");
            return;
        }

        try {
            String auctionId = request.get("auctionId").getAsString();

            // Gọi AdminService xử lý cưỡng chế
            adminService.forceCloseAuction(this.loggedInUser, auctionId);

            sendMessage("{\"action\": \"FORCE_CLOSE_REPLY\", \"status\": \"SUCCESS\"}");

            // Loa phường sẽ tự động bắn tin kết thúc cho mọi người
            // nhờ cái notifyObservers(closeEvent) đã có sẵn trong AuctionService.closeAuction()

        } catch (SecurityException se) {
            sendError("Bạn không có quyền cưỡng chế phiên đấu giá này!");
        } catch (Exception e) {
            sendError("Lỗi khi đóng phiên: " + e.getMessage());
        }
    }
    private void handleStartAuction(JsonObject request) {
        if (this.loggedInUser == null) {
            sendError("Phải đăng nhập mới được mở phiên đấu giá!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();
            int duration = request.get("durationMinutes").getAsInt();

            // SỬA TẠI ĐÂY: Dùng quanLyKho (ItemService) thay vì itemRepo
            Item item = quanLyKho.findById(itemId);

            if (item == null) {
                sendError("Không tìm thấy sản phẩm này!");
                return;
            }

            if (!item.getSeller().getId().equals(this.loggedInUser.getId())) {
                sendError("Bạn không thể mang đồ của người khác đi đấu giá!");
                return;
            }

            // 2. Gọi Tổng Quản mở phiên
            Auction newAuction = tongQuan.startAuction(item, duration);

            // 3. Báo thành công cho chủ hàng
            sendMessage("{\"action\": \"START_AUCTION_REPLY\", \"status\": \"SUCCESS\", \"auctionId\": \"" + newAuction.getId() + "\"}");

            // 4. Bắn loa phường (Nhớ đổi broadcast sang public như tui nói nhé)
            String notifyJson = String.format(
                    "{\"action\": \"GLOBAL_NOTIFY\", \"message\": \"Món hàng mới lên sàn: %s!\"}",
                    item.getName()
            );
            loaPhuong.broadcast(notifyJson);

        } catch (Exception e) {
            sendError("Lỗi khi mở phiên đấu giá: " + e.getMessage());
        }
    }
    private void handleGetAllUsers() {
        // 1. Check xem đã Login chưa
        if (this.loggedInUser == null) {
            sendError("Admin vui lòng đăng nhập!");
            return;
        }

        try {
            // 2. Gọi AdminService để lấy danh sách
            List<User> userList = adminService.getAllUsers(this.loggedInUser);

            // 3. Ép nguyên cái List sang JSON
            String jsonUsers = gson.toJson(userList);

            // 4. Bắn về cho Admin
            sendMessage("{\"action\": \"USERS_LIST\", \"data\": " + jsonUsers + "}");

        } catch (SecurityException se) {
            sendError("Cảnh báo: Bạn không có quyền truy cập danh sách người dùng!");
        } catch (Exception e) {
            sendError("Lỗi khi tải danh sách người dùng: " + e.getMessage());
        }
    }
}
