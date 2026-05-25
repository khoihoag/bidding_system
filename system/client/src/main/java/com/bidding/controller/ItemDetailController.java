package com.bidding.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemDetailController {

    @FXML private ImageView itemImageView;
    @FXML private Button prevImageButton;
    @FXML private Button nextImageButton;
    @FXML private Label imageCounterLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox specsContainer;

    private final List<String> imagePaths = new ArrayList<>();
    private int currentImageIndex = 0;
    private Runnable goBackCallback;

    public void setGoBackCallback(Runnable goBackCallback) {
        this.goBackCallback = goBackCallback;
    }

    public void setItemData(JsonObject itemJson) {
        itemNameLabel.setText(itemJson.has("name") ? itemJson.get("name").getAsString() : "Chưa có tên");
        descriptionLabel.setText(itemJson.has("description") ? itemJson.get("description").getAsString() : "Chưa có mô tả");

        imagePaths.clear();
        currentImageIndex = 0;
        itemImageView.setImage(null);

        if (itemJson.has("images") && itemJson.get("images").isJsonArray()) {
            for (JsonElement imageElement : itemJson.getAsJsonArray("images")) {
                if (!imageElement.isJsonNull()) {
                    String imagePath = imageElement.getAsString();
                    if (imagePath != null && !imagePath.isBlank()) {
                        imagePaths.add(imagePath);
                    }
                }
            }
        }

        loadImageAt(currentImageIndex);
        updateImageNavigation();

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
    private void showPreviousImage() {
        if (imagePaths.size() <= 1) {
            return;
        }

        currentImageIndex = (currentImageIndex - 1 + imagePaths.size()) % imagePaths.size();
        loadImageAt(currentImageIndex);
        updateImageNavigation();
    }

    @FXML
    private void showNextImage() {
        if (imagePaths.size() <= 1) {
            return;
        }

        currentImageIndex = (currentImageIndex + 1) % imagePaths.size();
        loadImageAt(currentImageIndex);
        updateImageNavigation();
    }

    private void loadImageAt(int index) {
        if (imagePaths.isEmpty() || index < 0 || index >= imagePaths.size()) {
            itemImageView.setImage(null);
            return;
        }

        try {
            File file = new File(imagePaths.get(index));
            itemImageView.setImage(file.exists() ? new Image(file.toURI().toString()) : null);
        } catch (Exception e) {
            itemImageView.setImage(null);
            System.out.println("Lỗi load ảnh: " + e.getMessage());
        }
    }

    private void updateImageNavigation() {
        boolean hasMultipleImages = imagePaths.size() > 1;

        prevImageButton.setVisible(hasMultipleImages);
        prevImageButton.setManaged(hasMultipleImages);
        nextImageButton.setVisible(hasMultipleImages);
        nextImageButton.setManaged(hasMultipleImages);
        imageCounterLabel.setVisible(hasMultipleImages);
        imageCounterLabel.setManaged(hasMultipleImages);

        imageCounterLabel.setText(hasMultipleImages ? (currentImageIndex + 1) + " / " + imagePaths.size() : "");
    }

    @FXML
    private void onBackButtonClicked() {
        if (goBackCallback != null) {
            goBackCallback.run();
        }
    }
}
