package com.bidding.controller; // Sếp sửa lại package cho đúng

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.File;

public class ItemDetailController {

    @FXML private ImageView itemImageView;
    @FXML private Label itemNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox specsContainer; // Chỗ chứa thông số chi tiết
    private Runnable goBackCallback;

    public void setGoBackCallback(Runnable goBackCallback) {
        this.goBackCallback = goBackCallback;
    }
    // Hàm này sẽ được gọi khi sếp bấm "Xem chi tiết" từ Kho đồ
    public void setItemData(JsonObject itemJson) {
        // 1. Tên đồ vật
        itemNameLabel.setText(itemJson.has("name") ? itemJson.get("name").getAsString() : "Chưa có tên");

        // 2. Mô tả
        descriptionLabel.setText(itemJson.has("description") ? itemJson.get("description").getAsString() : "Chưa có mô tả");

        // 3. Load ảnh to chà bá
        if (itemJson.has("images") && itemJson.getAsJsonArray("images").size() > 0) {
            String imagePath = itemJson.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    itemImageView.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception e) {
                System.out.println("Lỗi load ảnh: " + e.getMessage());
            }
        }

        // 4. Load thông số chi tiết (Giống y hệt bên Sảnh đấu giá)
        specsContainer.getChildren().clear();
        if (itemJson.has("specifications")) {
            JsonObject specs = itemJson.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                Label specLabel = new Label("• " + key + ": " + specs.get(key).getAsString());
                specLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5 0;");
                specsContainer.getChildren().add(specLabel);
            }
        }
    }

    @FXML
    private void onBackButtonClicked() {
        // Gọi dây cáp để báo cho màn hình chính biết là "Ê, quay xe về Kho đồ đi!"
        if (goBackCallback != null) {
            goBackCallback.run();
        }
    }
}