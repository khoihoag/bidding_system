package com.bidding.server.controller;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.item.Item;
import com.bidding.server.service.ItemService;
import com.bidding.server.service.AuctionService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.bidding.server.utils.GsonUtil;
import java.util.Map;
import java.util.HashMap;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.item.Art;
import com.bidding.server.model.item.Vehicle;
import com.bidding.server.model.item.Electronics;
public class ItemController {
    private final ClientHandler client;
    private final ItemService quanLyKho;
    private final AuctionService tongQuan;
    Gson gson = GsonUtil.getInstance();
    public ItemController(ClientHandler client, ItemService quanLyKho, AuctionService tongQuan) {
        this.client = client;
        this.quanLyKho = quanLyKho;
        this.tongQuan = tongQuan;
    }

    public void handleGetItems() {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để xem kho đồ!");
            return;
        }

        try {
            List<Item> myItems = quanLyKho.getMyItems(client.getLoggedInUser());
            com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();

            for (Item item : myItems) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("id", item.getId());
                itemObj.addProperty("name", item.getName());
                itemObj.addProperty("startingPrice", item.getStartingPrice());
                itemObj.addProperty("condition", item.getCondition() != null ? item.getCondition().toString() : "NEW");
                itemObj.addProperty("type", item.getCategory());
                itemObj.addProperty("description", item.getDescription());

                // Xác định trạng thái đấu giá của vật phẩm
                String status = "NONE";
                String dbStatus = tongQuan.getAuctionStatusForItem(item.getId());
                if (dbStatus != null) {
                    status = dbStatus; // This will return "RUNNING", "PENDING" (scheduled), "FINISHED", "CANCELLED"
                }
                itemObj.addProperty("status", status);

                if (item.getImages() != null && !item.getImages().isEmpty()) {
                    com.google.gson.JsonArray imgArray = new com.google.gson.JsonArray();
                    for(String img : item.getImages()){
                        imgArray.add(img);
                    }
                    itemObj.add("images", imgArray);
                }

                // ================= VŨ KHÍ TỐI THƯỢNG CỦA SẾP =================
                // Lấy toàn bộ Map thông số và đóng gói thành JSON tự động
                JsonObject specsObj = new JsonObject();
                Map<String, String> specs = item.getSpecifications();
                if (specs != null) {
                    for (Map.Entry<String, String> entry : specs.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                            specsObj.addProperty(entry.getKey(), entry.getValue());
                        }
                    }
                }
                itemObj.add("specifications", specsObj);
                // =============================================================

                itemsArray.add(itemObj);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "ITEMS_LIST");
            reply.add("items", itemsArray);

            client.sendMessage(reply.toString());

        } catch (Exception e) {
            System.err.println("Lỗi đóng gói JSON kho đồ: " + e.getMessage());
            client.sendError("Lỗi hệ thống khi tải kho đồ!");
        }
    }

    public void handleAddItem(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Phải đăng nhập mới được đăng bán đồ chứ!");
            return;
        }

        try {
            String name = request.get("name").getAsString();
            String desc = request.get("description").getAsString();
            double price = request.get("startingPrice").getAsDouble();
            String type = request.get("type").getAsString();

            com.bidding.server.enums.ItemCondition condition = null;
            if (request.has("condition") && !request.get("condition").isJsonNull()) {
                condition = com.bidding.server.enums.ItemCondition.valueOf(request.get("condition").getAsString());
            }

            // LẤY DANH SÁCH ĐƯỜNG DẪN ẢNH TỪ JSON
            List<String> imageList = new java.util.ArrayList<>();
            if (request.has("images") && !request.get("images").isJsonNull()) {
                com.google.gson.JsonArray imagesArray = request.getAsJsonArray("images");
                for (com.google.gson.JsonElement imgElem : imagesArray) {
                    imageList.add(imgElem.getAsString());
                }
            }

            Item newItem = null;
            switch (type) {
                case "Art":
                    // Thay chữ null bằng imageList
                    newItem = new com.bidding.server.model.item.Art(null, name, desc, price, imageList, client.getLoggedInUser(), condition, request.get("artist").getAsString(), request.get("medium").getAsString(), request.get("yearCreated").getAsInt(), request.get("dimensions").getAsString());
                    break;
                case "Electronics":
                    // Thay chữ null bằng imageList
                    newItem = new com.bidding.server.model.item.Electronics(null, name, desc, price, imageList, client.getLoggedInUser(), condition, request.get("brand").getAsString(), request.get("model").getAsString(), request.get("warrantyMonths").getAsInt(), request.get("powerWatts").getAsInt());
                    break;
                case "Vehicle":
                    // Thay chữ null bằng imageList
                    newItem = new com.bidding.server.model.item.Vehicle(null, name, desc, price, imageList, client.getLoggedInUser(), condition, request.get("make").getAsString(), request.get("model").getAsString(), request.get("year").getAsInt(), request.get("mileage").getAsInt(), request.get("fuelType").getAsString());
                    break;
                default:
                    client.sendError("Loại mặt hàng không hợp lệ!");
                    return;
            }

            quanLyKho.createItem(client.getLoggedInUser(), newItem);
            client.sendMessage("{\"action\": \"ADD_ITEM_REPLY\", \"status\": \"SUCCESS\"}");

        } catch (Exception e) {
            client.sendError("Lỗi khi đăng bán vật phẩm: " + e.getMessage());
        }
    }

    public void handleUpdateItem(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Phải đăng nhập mới được sửa đồ!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();
            // Chặn sửa nếu đang đấu giá RUNNING hoặc đã hoàn thành
            String dbStatus = tongQuan.getAuctionStatusForItem(itemId);
            if ("RUNNING".equals(dbStatus) || "OPEN".equals(dbStatus) || "FINISHED".equals(dbStatus) || "PAID".equals(dbStatus)) {
                client.sendError("Vật phẩm đang hoặc đã được đấu giá, không được phép sửa đổi thông tin!");
                return;
            }

            Item existingItem = quanLyKho.findById(itemId);
            if (existingItem == null) {
                client.sendError("Không tìm thấy vật phẩm!");
                return;
            }
            // Kiểm tra quyền sở hữu
            if (!existingItem.getSeller().getId().equals(client.getLoggedInUser().getId())) {
                client.sendError("Chỉ chủ sở hữu mới được sửa vật phẩm!");
                return;
            }

            // Cập nhật các trường cơ bản
            if (request.has("name") && !request.get("name").isJsonNull()) {
                existingItem.setName(request.get("name").getAsString());
            }
            if (request.has("description") && !request.get("description").isJsonNull()) {
                existingItem.setDescription(request.get("description").getAsString());
            }
            if (request.has("startingPrice") && !request.get("startingPrice").isJsonNull()) {
                existingItem.setStartingPrice(request.get("startingPrice").getAsDouble());
            }
            if (request.has("condition") && !request.get("condition").isJsonNull()) {
                existingItem.setCondition(com.bidding.server.enums.ItemCondition.valueOf(request.get("condition").getAsString()));
            }
            if (request.has("images") && !request.get("images").isJsonNull()) {
                java.util.List<String> imageList = new java.util.ArrayList<>();
                com.google.gson.JsonArray imagesArray = request.getAsJsonArray("images");
                for (com.google.gson.JsonElement imgElem : imagesArray) {
                    imageList.add(imgElem.getAsString());
                }
                existingItem.setImages(imageList);
            }

            // Cập nhật các trường đặc biệt theo loại
            if (existingItem instanceof Art && request.has("artist")) {
                Art artItem = (Art) existingItem;
                if (request.has("artist") && !request.get("artist").isJsonNull()) artItem.setArtist(request.get("artist").getAsString());
                if (request.has("medium") && !request.get("medium").isJsonNull()) artItem.setMedium(request.get("medium").getAsString());
                if (request.has("yearCreated") && !request.get("yearCreated").isJsonNull()) artItem.setYearCreated(request.get("yearCreated").getAsInt());
                if (request.has("dimensions") && !request.get("dimensions").isJsonNull()) artItem.setDimensions(request.get("dimensions").getAsString());
            } else if (existingItem instanceof Vehicle && request.has("make")) {
                Vehicle vehItem = (Vehicle) existingItem;
                if (request.has("make") && !request.get("make").isJsonNull()) vehItem.setMake(request.get("make").getAsString());
                if (request.has("model") && !request.get("model").isJsonNull()) vehItem.setModel(request.get("model").getAsString());
                if (request.has("year") && !request.get("year").isJsonNull()) vehItem.setYear(request.get("year").getAsInt());
                if (request.has("mileage") && !request.get("mileage").isJsonNull()) vehItem.setMileage(request.get("mileage").getAsInt());
                if (request.has("fuelType") && !request.get("fuelType").isJsonNull()) vehItem.setFuelType(request.get("fuelType").getAsString());
            } else if (existingItem instanceof Electronics && request.has("brand")) {
                Electronics elecItem = (Electronics) existingItem;
                if (request.has("brand") && !request.get("brand").isJsonNull()) elecItem.setBrand(request.get("brand").getAsString());
                if (request.has("model") && !request.get("model").isJsonNull()) elecItem.setModel(request.get("model").getAsString());
                if (request.has("warrantyMonths") && !request.get("warrantyMonths").isJsonNull()) elecItem.setWarrantyMonths(request.get("warrantyMonths").getAsInt());
                if (request.has("powerWatts") && !request.get("powerWatts").isJsonNull()) elecItem.setPowerWatts(request.get("powerWatts").getAsInt());
            }

            quanLyKho.createItem(client.getLoggedInUser(), existingItem); // saveOrUpdate
            client.sendMessage("{\"action\": \"UPDATE_ITEM_REPLY\", \"status\": \"SUCCESS\"}");

        } catch (SecurityException se) {
            client.sendError("Lỗi bảo mật: " + se.getMessage());
        } catch (Exception e) {
            client.sendError("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public void handleDeleteItem(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Đăng nhập đi rồi mới cho xóa đồ!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();
            String dbStatus = tongQuan.getAuctionStatusForItem(itemId);
            if ("RUNNING".equals(dbStatus) || "OPEN".equals(dbStatus)) {
                client.sendError("Vật phẩm đang được đấu giá, không xóa được nhé!");
                return;
            }
            // FAILED/CANCELLED: Đã chạy nhưng không ai mua -> CHO PHÉP XÓA
            // FINISHED/PAID: Đấu giá thành công -> CHO PHÉP XÓA (chủ sở hữu muốn dọn kho)
            // NONE: Chưa đấu giá -> CHO PHÉP XÓA

            boolean success = quanLyKho.deleteItem(client.getLoggedInUser(), itemId);
            if (success) {
                client.sendMessage("{\"action\": \"DELETE_ITEM_REPLY\", \"status\": \"SUCCESS\"}");
            } else {
                client.sendError("Không tìm thấy món đồ này trong kho.");
            }

        } catch (SecurityException se) {
            client.sendError("Đồ của người ta mà ông đòi xóa à? Quên đi!");
        } catch (Exception e) {
            client.sendError("Lỗi khi xóa đồ: " + e.getMessage());
        }
    }
    private String callAIPricePredictor(JsonObject itemJson) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Tạo Request bắn sang FastAPI của ông
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(itemJson)))
                    .build();

            // Nhận phản hồi
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body(); // Trả về cục JSON chứa estimated_final_price
            } else {
                System.err.println("AI Server trả lỗi: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Không kết nối được tới AI Server: " + e.getMessage());
            return null;
        }
    }
    public void handleGetAIPrice(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để sử dụng tính năng AI!");
            return;
        }

        // Gửi thẳng cục request (chứa các trường item_type, starting_price,...) sang Python
        // Vì ông đã thiết kế schema của FastAPI khớp với JSON truyền lên rồi
        String aiResult = callAIPricePredictor(request);

        if (aiResult != null) {
            // Bắn kết quả về cho UI của Khôi
            client.sendMessage("{\"action\": \"AI_PRICE_REPLY\", \"data\": " + aiResult + "}");
        } else {
            client.sendError("Hệ thống AI hiện đang bảo trì, vui lòng thử lại sau!");
        }
    }
    public void handleGetWonItems() {
        if (client.getLoggedInUser() == null) {
            client.sendError("Vui lòng đăng nhập để xem chiến lợi phẩm!");
            return;
        }

        try {
            // Gọi Service để lôi đồ từ Repo (cái hàm sếp vừa thêm vào ItemRepository)
            List<Item> wonItems = quanLyKho.getWonItems(client.getLoggedInUser());
            com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();

            for (Item item : wonItems) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("id", item.getId());
                itemObj.addProperty("name", item.getName());
                itemObj.addProperty("startingPrice", item.getStartingPrice());
                itemObj.addProperty("condition", item.getCondition() != null ? item.getCondition().toString() : "NEW");
                itemObj.addProperty("type", item.getCategory());
                itemObj.addProperty("description", item.getDescription());

                if (item.getImages() != null && !item.getImages().isEmpty()) {
                    com.google.gson.JsonArray imgArray = new com.google.gson.JsonArray();
                    for(String img : item.getImages()) { imgArray.add(img); }
                    itemObj.add("images", imgArray);
                }

                // Gói full thông số đặc biệt để Popup hiện đủ chữ
                JsonObject specsObj = new JsonObject();
                java.util.Map<String, String> specs = item.getSpecifications();
                if (specs != null) {
                    for (java.util.Map.Entry<String, String> entry : specs.entrySet()) {
                        specsObj.addProperty(entry.getKey(), entry.getValue());
                    }
                }
                itemObj.add("specifications", specsObj);

                itemsArray.add(itemObj);
            }

            JsonObject reply = new JsonObject();
            reply.addProperty("action", "WON_ITEMS_LIST"); // Nhãn này phải khớp với UI
            reply.add("items", itemsArray);

            client.sendMessage(reply.toString());
            System.out.println("[Server] Đã gửi " + wonItems.size() + " món đồ thắng cuộc cho " + client.getLoggedInUser().getUsername());

        } catch (Exception e) {
            System.err.println("Lỗi đóng gói JSON chiến lợi phẩm: " + e.getMessage());
            client.sendError("Lỗi hệ thống khi tải chiến lợi phẩm!");
        }
    }
}