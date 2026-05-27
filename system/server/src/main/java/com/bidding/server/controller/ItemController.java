package com.bidding.server.controller;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.service.ItemService;
import com.bidding.server.service.AuctionService;
import com.google.gson.JsonObject;
import com.bidding.server.utils.ItemJsonMapper;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.bidding.server.utils.GsonUtil;
import com.bidding.server.model.item.Art;
import com.bidding.server.model.item.Vehicle;
import com.bidding.server.model.item.Electronics;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
                JsonObject itemJson = ItemJsonMapper.toJson(item);
                enrichAuctionStatus(itemJson, item.getId());
                itemsArray.add(itemJson);
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
            double price = requireDouble(request, "startingPrice");
            if (price < 0) {
                throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn hoặc bằng 0.");
            }
            double bidStep = requireDouble(request, "bidStep");
            validateBidStep(price, bidStep);
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
                    newItem = new com.bidding.server.model.item.Art(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("artist").getAsString(), request.get("medium").getAsString(), requireInt(request, "yearCreated"), request.get("dimensions").getAsString());
                    break;
                case "Electronics":
                    // Thay chữ null bằng imageList
                    newItem = new com.bidding.server.model.item.Electronics(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("brand").getAsString(), request.get("model").getAsString(), requireInt(request, "warrantyMonths"), requireInt(request, "powerWatts"));
                    break;
                case "Vehicle":
                    // Thay chữ null bằng imageList
                    newItem = new com.bidding.server.model.item.Vehicle(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("make").getAsString(), request.get("model").getAsString(), requireInt(request, "year"), requireInt(request, "mileage"), request.get("fuelType").getAsString());
                    break;
                default:
                    client.sendError("Loại mặt hàng không hợp lệ!");
                    return;
            }

            newItem.setBidStep(bidStep);
            quanLyKho.createItem(client.getLoggedInUser(), newItem);
            client.sendMessage("{\"action\": \"ADD_ITEM_REPLY\", \"status\": \"SUCCESS\", \"message\": \"San pham da duoc gui va dang cho admin duyet.\"}");

        } catch (Exception e) {
            client.sendError("Lỗi khi đăng bán vật phẩm: " + e.getMessage());
        }
    }

    private double requireDouble(JsonObject request, String fieldName) {
        JsonElement value = request.get(fieldName);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(fieldName + " phải là số, không được truyền dạng chuỗi.");
        }

        double result = value.getAsDouble();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
        return result;
    }

    private void validateBidStep(double price, double bidStep) {
        if (bidStep <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0.");
        }
        if (bidStep > price) {
            throw new IllegalArgumentException("Bước giá không được lớn hơn giá trị sản phẩm.");
        }
    }

    private int requireInt(JsonObject request, String fieldName) {
        JsonElement value = request.get(fieldName);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên, không được truyền dạng chuỗi.");
        }

        String rawValue = value.getAsString();
        if (!rawValue.matches("-?\\d+")) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên.");
        }

        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " vượt quá giới hạn số nguyên.");
        }
    }

    public void handleUpdateItem(JsonObject request) {
        if (client.getLoggedInUser() == null) {
            client.sendError("Phải đăng nhập mới được sửa đồ!");
            return;
        }

        try {
            String itemId = request.get("itemId").getAsString();
            if (tongQuan.isItemInAnyAuction(itemId)) {
                client.sendError("Món hàng này đang được đấu giá, không được phép sửa đổi thông tin!");
                return;
            }

            String type = request.get("type").getAsString();
            String name = request.get("name").getAsString();
            String desc = request.get("description").getAsString();
            double price = requireDouble(request, "startingPrice");
            if (price < 0) {
                throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn hoặc bằng 0.");
            }
            double bidStep = requireDouble(request, "bidStep");
            validateBidStep(price, bidStep);

            com.bidding.server.enums.ItemCondition condition = null;
            if (request.has("condition") && !request.get("condition").isJsonNull()) {
                condition = com.bidding.server.enums.ItemCondition.valueOf(request.get("condition").getAsString());
            }

            List<String> imageList = new java.util.ArrayList<>();
            if (request.has("images") && !request.get("images").isJsonNull()) {
                com.google.gson.JsonArray imagesArray = request.getAsJsonArray("images");
                for (com.google.gson.JsonElement imgElem : imagesArray) {
                    imageList.add(imgElem.getAsString());
                }
            }

            Item updateData;
            switch (type) {
                case "Art":
                    updateData = new Art(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("artist").getAsString(), request.get("medium").getAsString(), requireInt(request, "yearCreated"), request.get("dimensions").getAsString());
                    break;
                case "Electronics":
                    updateData = new Electronics(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("brand").getAsString(), request.get("model").getAsString(), requireInt(request, "warrantyMonths"), requireInt(request, "powerWatts"));
                    break;
                case "Vehicle":
                    updateData = new Vehicle(null, name, desc, price, imageList, client.getLoggedInUser().getId(), client.getLoggedInUser().getFullName(), condition, request.get("make").getAsString(), request.get("model").getAsString(), requireInt(request, "year"), requireInt(request, "mileage"), request.get("fuelType").getAsString());
                    break;
                default:
                    client.sendError("Loại mặt hàng không hợp lệ!");
                    return;
            }

            updateData.setBidStep(bidStep);
            quanLyKho.updateItem(client.getLoggedInUser(), itemId, updateData);
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
            if (tongQuan.isItemInAnyAuction(itemId)) {
                client.sendError("Đồ đang hoặc đã được đấu giá rồi thì không xóa được đâu bạn yêu ơi!");
                return;
            }

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
            JsonObject reply = new JsonObject();
            reply.addProperty("action", "AI_PRICE_REPLY");
            reply.add("data", com.google.gson.JsonParser.parseString(aiResult));
            client.sendMessage(reply.toString());
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
                JsonObject itemJson = ItemJsonMapper.toJson(item);
                enrichAuctionStatus(itemJson, item.getId());
                itemsArray.add(itemJson);
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

    private void enrichAuctionStatus(JsonObject itemJson, String itemId) {
        Auction activeAuction = tongQuan.findActiveAuctionByItemId(itemId);
        if (activeAuction != null) {
            String activeStatus = activeAuction.getStatus() != null ? activeAuction.getStatus().name() : "UNKNOWN";
            itemJson.addProperty("auctionId", activeAuction.getId());
            itemJson.addProperty("auctionPrice", activeAuction.getCurrentPrice() != null ? activeAuction.getCurrentPrice().get() : 0.0);
            if (activeAuction.getEndTime() != null) {
                itemJson.addProperty("paymentDeadline", activeAuction.getEndTime().plusMinutes(10).toString());
            }
            itemJson.addProperty("auctionStatus", activeStatus);
            itemJson.addProperty("auctionLabel", toAuctionLabel(activeStatus, activeAuction.getCurrentWinner() != null));
            return;
        }

        AuctionEntity auction = tongQuan.getAuctionRepository().findByItemId(itemId);
        if (auction == null) {
            itemJson.addProperty("auctionStatus", "NONE");
            itemJson.addProperty("auctionLabel", "Chưa đấu giá");
            return;
        }

        String status = auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN";
        itemJson.addProperty("auctionId", auction.getId());
        itemJson.addProperty("auctionPrice", auction.getCurrentPrice() != null ? auction.getCurrentPrice() : 0.0);
        if (auction.getEndTime() != null) {
            itemJson.addProperty("paymentDeadline", auction.getEndTime().plusMinutes(10).toString());
        }
        itemJson.addProperty("auctionStatus", status);
        itemJson.addProperty("auctionLabel", toAuctionLabel(status, auction.getCurrentWinner() != null));
    }

    private String toAuctionLabel(String status, boolean hasWinner) {
        if ("PAID".equals(status)) {
            return "Đã thanh toán";
        }
        if ("FINISHED".equals(status)) {
            return hasWinner ? "Thành công" : "Đã kết thúc";
        }
        if ("FAILED".equals(status)) {
            return "Failed";
        }
        if ("RUNNING".equals(status)) {
            return "Đang đấu giá";
        }
        if ("OPEN".equals(status)) {
            return "Đã đặt lịch";
        }
        if ("NONE".equals(status)) {
            return "Chưa đấu giá";
        }
        return status;
    }
}
