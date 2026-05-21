package com.bidding.controller.auctiondetail;

import com.google.gson.JsonObject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Item detail dialog for the auction detail screen.
 */
public final class AuctionDetailItemDialog {

    private AuctionDetailItemDialog() {
    }

    public static void show(JsonObject item, Class<?> resourceClass) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi Tiết Vật Phẩm Đấu Giá");
        dialog.setHeaderText(item.has("name") ? item.get("name").getAsString() : "Không tên");

        try {
            dialog.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/style.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception ignored) {
        }

        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-padding: 20; -fx-font-size: 15px;");
        vbox.setPrefWidth(500);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(460);
        imgView.setFitHeight(300);
        imgView.setPreserveRatio(true);
        if (item.has("images") && item.getAsJsonArray("images").size() > 0) {
            String imgPath = item.getAsJsonArray("images").get(0).getAsString();
            try {
                File file = new File(imgPath);
                if (file.exists()) {
                    imgView.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception e) {
                System.err.println("Lỗi load ảnh!");
            }
        } else {
            vbox.getChildren().add(new Label("(Chưa có hình ảnh)"));
        }

        Label lblType = new Label("Loại: " + (item.has("type") ? item.get("type").getAsString() : "---"));

        String conditionText = item.has("condition") ? item.get("condition").getAsString() : "---";
        if ("NEW".equals(conditionText)) {
            conditionText = "Mới 100%";
        } else if ("USED".equals(conditionText)) {
            conditionText = "Đã sử dụng";
        }

        Label lblCondition = new Label("Tình trạng: " + conditionText);
        lblCondition.setStyle("-fx-font-style: italic; -fx-text-fill: #6b7a90;");

        Label lblPrice = new Label("Giá khởi điểm: 0 đ");
        lblPrice.getStyleClass().add("price-value");
        if (item.has("startingPrice")) {
            NumberFormat fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
            lblPrice.setText("Giá khởi điểm: " + fmt.format(item.get("startingPrice").getAsDouble()) + " đ");
        }

        Label lblDesc = new Label("Mô tả: " + (item.has("description") ? item.get("description").getAsString() : ""));
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(480);

        vbox.getChildren().addAll(imgView, lblType, lblCondition, lblPrice, new Separator(), lblDesc);

        if (item.has("specifications")) {
            JsonObject specs = item.getAsJsonObject("specifications");
            VBox extraBox = new VBox(8);
            extraBox.getStyleClass().add("info-card");
            extraBox.setStyle("");
            extraBox.getChildren().add(new Label("📋 Thông số chi tiết:"));
            for (String key : specs.keySet()) {
                String value = specs.get(key).getAsString();
                extraBox.getChildren().add(new Label("  • " + key + ": " + value));
            }
            vbox.getChildren().add(extraBox);
        }

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public static void showWarning(Class<?> resourceClass, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            alert.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/style.css").toExternalForm());
            alert.getDialogPane().setStyle("-fx-background-color: #05070a;");
        } catch (Exception ignored) {
        }
        alert.showAndWait();
    }
}
