package com.bidding.server.utils;

import com.bidding.server.model.item.Item;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Serializes {@link Item} instances to JSON objects used by the socket API.
 */
public final class ItemJsonMapper {

    private ItemJsonMapper() {
    }

    public static JsonObject toJson(Item item) {
        JsonObject itemObj = new JsonObject();
        itemObj.addProperty("id", item.getId());
        itemObj.addProperty("name", item.getName());
        itemObj.addProperty("startingPrice", item.getStartingPrice());
        itemObj.addProperty("condition", item.getCondition() != null ? item.getCondition().toString() : "NEW");
        itemObj.addProperty("type", item.getCategory());
        itemObj.addProperty("description", item.getDescription());
        itemObj.addProperty("sellerId", item.getSellerId());
        itemObj.addProperty("sellerFullName", item.getSellerFullName());
        itemObj.addProperty("approvalStatus", item.getApprovalStatus() != null ? item.getApprovalStatus().name() : "PENDING");
        itemObj.addProperty("rejectionReason", item.getRejectionReason());

        if (item.getImages() != null && !item.getImages().isEmpty()) {
            JsonArray imgArray = new JsonArray();
            for (String img : item.getImages()) {
                imgArray.add(img);
            }
            itemObj.add("images", imgArray);
        }

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
        return itemObj;
    }
}
