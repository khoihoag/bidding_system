package com.bidding.client.scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Model ProductData dùng chung giữa SceneSeller1 và SceneAddItem.
 * Tách ra để tránh inner-class, giúp 2 controller trao đổi dữ liệu.
 */
public class ProductShared {

    public static class ProductData {
        public int id;
        public String itemIdRef;
        public String auctionId;
        public String name;
        public String startPrice;
        public String currentPrice;
        public String description;
        public String startTimeStr;
        public String endTimeStr;
        public List<String> imagePaths;
        public int bidCount;

        public ProductData(int id, String itemIdRef, String auctionId,
                           String name, String startPrice, String currentPrice,
                           String description, String startTimeStr, String endTimeStr,
                           List<String> imagePaths, int bidCount) {
            this.id = id;
            this.itemIdRef = itemIdRef;
            this.auctionId = auctionId;
            this.name = name;
            this.startPrice = startPrice;
            this.currentPrice = currentPrice;
            this.description = description;
            this.startTimeStr = startTimeStr;
            this.endTimeStr = endTimeStr;
            this.imagePaths = imagePaths == null ? new ArrayList<>() : new ArrayList<>(imagePaths);
            this.bidCount = bidCount;
        }
    }
}
