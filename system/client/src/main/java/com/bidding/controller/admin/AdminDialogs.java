package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AdminBidHistoryRow;
import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.util.JsonUtil;
import com.google.gson.JsonObject;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * Modal dialogs used by the admin auctions panel.
 */
public final class AdminDialogs {

    private AdminDialogs() {
    }

    public static void showAuctionItemDetails(AuctionRow auction, Class<?> resourceClass) {
        JsonObject item = auction.getItemJson();
        if (item == null) {
            AdminUiHelper.showAlert(Alert.AlertType.WARNING, "Không có dữ liệu",
                    "Không tìm thấy thông tin sản phẩm của phiên này.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết vật phẩm");
        dialog.setHeaderText(JsonUtil.getString(item, "name"));
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        VBox content = new VBox(12);
        content.setPrefWidth(580);
        content.setStyle("-fx-padding: 20; -fx-font-size: 15px;");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(540);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(false);
        if (item.has("images") && item.get("images").isJsonArray() && item.getAsJsonArray("images").size() > 0) {
            String imagePath = item.getAsJsonArray("images").get(0).getAsString();
            File file = new File(imagePath);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
                content.getChildren().add(imageView);
            } else {
                content.getChildren().add(new Label("(Không tìm thấy ảnh)"));
            }
        } else {
            content.getChildren().add(new Label("(Chưa có hình ảnh)"));
        }

        Label type = new Label("Loại: " + JsonUtil.getString(item, "type"));
        String conditionText = JsonUtil.getString(item, "condition");
        if ("NEW".equals(conditionText)) {
            conditionText = "Mới 100%";
        } else if ("USED".equals(conditionText)) {
            conditionText = "Đã sử dụng";
        }
        Label condition = new Label("Tình trạng: " + conditionText);
        condition.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b;");

        double startingPrice = item.has("startingPrice") && !item.get("startingPrice").isJsonNull()
                ? item.get("startingPrice").getAsDouble() : 0.0;
        Label price = new Label(String.format("Giá khởi điểm: %,.0f đ", startingPrice));
        price.setStyle("-fx-text-fill: #b08a1e; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label desc = new Label("Mô tả: " + JsonUtil.getString(item, "description"));
        desc.setWrapText(true);
        content.getChildren().addAll(type, condition, price, new Separator(), desc);

        if (item.has("specifications") && item.get("specifications").isJsonObject()) {
            VBox specsBox = new VBox(8);
            specsBox.setStyle("-fx-padding: 16; -fx-background-color: #f8fafc; -fx-background-radius: 12; "
                    + "-fx-border-color: #e5e7eb; -fx-border-radius: 12;");
            specsBox.getChildren().add(new Label("Thông số chi tiết:"));
            JsonObject specs = item.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                specsBox.getChildren().add(new Label("• " + key + ": " + specs.get(key).getAsString()));
            }
            content.getChildren().add(specsBox);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public static void showAuctionBidHistoryDialog(String auctionId, String auctionTitle,
                                                    ObservableList<AdminBidHistoryRow> rows,
                                                    Class<?> resourceClass) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử đấu giá");
        dialog.setHeaderText(auctionTitle + " - ID: " + auctionId);
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/admin.css").toExternalForm());
        } catch (Exception ignored) {
        }

        VBox content = new VBox(12);
        content.setPrefWidth(760);
        content.setPrefHeight(460);
        content.setStyle("-fx-padding: 12;");

        Label summary = new Label(rows.isEmpty()
                ? "Chưa có lượt đấu giá nào cho phiên này."
                : "Tổng số lượt đấu giá: " + rows.size());
        summary.setStyle("-fx-font-weight: bold;");

        TableView<AdminBidHistoryRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Chưa có dữ liệu lịch sử đấu giá."));

        TableColumn<AdminBidHistoryRow, Integer> colNo = new TableColumn<>("#");
        colNo.setCellValueFactory(new PropertyValueFactory<>("index"));
        colNo.setPrefWidth(45);

        TableColumn<AdminBidHistoryRow, String> colBidder = new TableColumn<>("Người đấu giá");
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colBidder.setPrefWidth(210);

        TableColumn<AdminBidHistoryRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(180);

        TableColumn<AdminBidHistoryRow, String> colAmount = new TableColumn<>("Số tiền");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setPrefWidth(130);
        colAmount.setStyle("-fx-alignment: CENTER_RIGHT;");

        TableColumn<AdminBidHistoryRow, String> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTime.setPrefWidth(150);

        TableColumn<AdminBidHistoryRow, String> colType = new TableColumn<>("Loại");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(80);

        TableColumn<AdminBidHistoryRow, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(100);

        table.getColumns().addAll(colNo, colBidder, colEmail, colAmount, colTime, colType, colStatus);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(summary, table);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
