package com.bidding.server.controller;

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
public class ItemController {
    private final ClientHandler client;
    private final ItemService quanLyKho;
    private final AuctionService tongQuan;
    private final Gson gson = new Gson();

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
        List<Item> myItems = quanLyKho.getMyItems(client.getLoggedInUser());
        client.sendMessage("{\"action\": \"ITEMS_LIST\", \"items\": " + gson.toJson(myItems) + "}");
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
            if (tongQuan.isItemInActiveAuction(itemId)) {
                client.sendError("Món hàng này đang được đấu giá, không được phép sửa đổi thông tin!");
                return;
            }

            Item updateData = new com.bidding.server.model.item.Art();
            updateData.setName(request.get("name").getAsString());
            updateData.setDescription(request.get("description").getAsString());

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
            if (tongQuan.isItemInActiveAuction(itemId)) {
                client.sendError("Đồ đang đấu giá, xóa là ăn phạt đấy! Không cho xóa.");
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
            // Bắn kết quả về cho UI của Khôi
            client.sendMessage("{\"action\": \"AI_PRICE_REPLY\", \"data\": " + aiResult + "}");
        } else {
            client.sendError("Hệ thống AI hiện đang bảo trì, vui lòng thử lại sau!");
        }
    }
}