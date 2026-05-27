package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AdminBidHistoryRow;
import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.PendingItemRow;
import com.bidding.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
        dialog.setTitle("Chi tiết sản phẩm");
        dialog.setHeaderText(null);
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        HBox content = new HBox(34);
        content.setPrefWidth(900);
        content.setStyle("-fx-padding: 10 12 14 12; -fx-background-color: #f8fafc; -fx-font-size: 13px;");
        content.getChildren().addAll(
                createProductMediaPane(item, auction.getStatus()),
                createProductInfoPane(item));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public static void showPendingItemDetails(PendingItemRow row,
                                              Class<?> resourceClass,
                                              Consumer<PendingItemRow> approveHandler,
                                              BiConsumer<PendingItemRow, String> rejectHandler) {
        JsonObject item = row.getItemJson();
        if (item == null) {
            AdminUiHelper.showAlert(Alert.AlertType.WARNING, "Không có dữ liệu",
                    "Không tìm thấy thông tin sản phẩm cần duyệt.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết sản phẩm cần duyệt");
        dialog.setHeaderText(null);
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(resourceClass.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        HBox detailsContent = new HBox(34);
        detailsContent.setStyle("-fx-padding: 10 12 14 12; -fx-background-color: #f8fafc; -fx-font-size: 13px;");
        detailsContent.getChildren().addAll(
                createProductMediaPane(item, null),
                createProductInfoPane(item));

        Button approveButton = new Button("Duyệt");
        Button rejectButton = new Button("Từ chối");
        Button closeButton = new Button("Đóng");
        stylePendingReviewButtons(approveButton, rejectButton, closeButton);

        approveButton.setOnAction(e -> {
            if (confirmApprove(row)) {
                closeDialog(dialog);
                approveHandler.accept(row);
            }
        });
        rejectButton.setOnAction(e -> {
            Optional<String> reason = promptRejectionReason(row);
            reason.ifPresent(value -> {
                closeDialog(dialog);
                rejectHandler.accept(row, value);
            });
        });
        closeButton.setOnAction(e -> closeDialog(dialog));

        HBox reviewButtons = new HBox(16, approveButton, rejectButton);
        reviewButtons.setAlignment(Pos.CENTER);

        StackPane footer = new StackPane();
        footer.setStyle("-fx-padding: 16 14 14 14; -fx-background-color: #ffffff; "
                + "-fx-border-color: #edf0f4 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.getChildren().addAll(reviewButtons, closeButton);
        StackPane.setAlignment(reviewButtons, Pos.CENTER);
        StackPane.setAlignment(closeButton, Pos.CENTER_RIGHT);

        VBox content = new VBox(detailsContent, footer);
        content.setPrefWidth(900);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private static boolean confirmApprove(PendingItemRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Duyệt sản phẩm");
        confirm.setHeaderText("Duyệt: " + row.getName());
        confirm.setContentText("Bạn có chắc chắn muốn duyệt sản phẩm này không?"
                + "\n\nID: " + row.getId()
                + "\nNgười bán: " + row.getSeller());
        return confirm.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .isPresent();
    }

    private static Optional<String> promptRejectionReason(PendingItemRow row) {
        TextInputDialog dialog = new TextInputDialog("Sản phẩm chưa đạt yêu cầu.");
        dialog.setTitle("Từ chối sản phẩm");
        dialog.setHeaderText("Từ chối: " + row.getName());
        dialog.setContentText("Lý do:");
        return dialog.showAndWait()
                .map(String::trim)
                .filter(reason -> !reason.isEmpty());
    }

    private static void closeDialog(Dialog<?> dialog) {
        if (dialog.getDialogPane().getScene() != null
                && dialog.getDialogPane().getScene().getWindow() != null) {
            dialog.getDialogPane().getScene().getWindow().hide();
            return;
        }
        dialog.close();
    }

    private static void stylePendingReviewButtons(Button approve, Button reject, Button close) {
        approve.setStyle("-fx-background-color: #16a34a; -fx-text-fill: #ffffff; "
                + "-fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 10 34;");

        reject.setStyle("-fx-background-color: #dc2626; -fx-text-fill: #ffffff; "
                + "-fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 10 30;");

        close.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #ffffff; "
                + "-fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 10 34;");
    }

    private static VBox createProductMediaPane(JsonObject item, String auctionStatus) {
        VBox mediaPane = new VBox(12);
        mediaPane.setPrefWidth(390);

        List<String> imagePaths = item.has("images") && item.get("images").isJsonArray()
                ? getExistingImagePaths(item.getAsJsonArray("images"))
                : List.of();

        ImageView mainImage = new ImageView();
        mainImage.setFitWidth(390);
        mainImage.setFitHeight(360);
        mainImage.setPreserveRatio(true);
        mainImage.setSmooth(true);

        StackPane imageFrame = new StackPane();
        imageFrame.setPrefSize(390, 360);
        imageFrame.setMinSize(390, 360);
        imageFrame.setMaxSize(390, 360);
        imageFrame.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; "
                + "-fx-border-color: #edf0f4; -fx-border-radius: 8;");

        if (imagePaths.isEmpty()) {
            Label missingImageLabel = new Label(item.has("images") ? "(Không tìm thấy ảnh)" : "(Chưa có hình ảnh)");
            missingImageLabel.setStyle("-fx-text-fill: #64748b;");
            imageFrame.getChildren().add(missingImageLabel);
        } else {
            mainImage.setImage(loadImage(imagePaths.get(0)));
            imageFrame.getChildren().add(mainImage);
        }

        if ("RUNNING".equals(auctionStatus)) {
            Label badge = new Label("LIVE NOW");
            badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; "
                    + "-fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 5 10; -fx-background-radius: 12;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            badge.setTranslateX(12);
            badge.setTranslateY(12);
            imageFrame.getChildren().add(badge);
        }

        HBox thumbnails = new HBox(10);
        thumbnails.setPrefHeight(70);
        List<StackPane> thumbnailNodes = new ArrayList<>();
        for (int i = 0; i < imagePaths.size(); i++) {
            String imagePath = imagePaths.get(i);
            StackPane thumbnail = createThumbnail(imagePath);
            int selectedIndex = i;
            thumbnail.setOnMouseClicked(e -> {
                mainImage.setImage(loadImage(imagePath));
                updateThumbnailSelection(thumbnailNodes, selectedIndex);
            });
            thumbnailNodes.add(thumbnail);
            thumbnails.getChildren().add(thumbnail);
        }
        updateThumbnailSelection(thumbnailNodes, 0);

        mediaPane.getChildren().add(imageFrame);
        if (!thumbnails.getChildren().isEmpty()) {
            ScrollPane thumbnailScroll = new ScrollPane(thumbnails);
            thumbnailScroll.setFitToHeight(true);
            thumbnailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            thumbnailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            thumbnailScroll.setPannable(true);
            thumbnailScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            mediaPane.getChildren().add(thumbnailScroll);
        }

        return mediaPane;
    }

    private static StackPane createThumbnail(String imagePath) {
        ImageView thumbImage = new ImageView(loadImage(imagePath));
        thumbImage.setFitWidth(72);
        thumbImage.setFitHeight(62);
        thumbImage.setPreserveRatio(true);
        thumbImage.setSmooth(true);

        StackPane thumbnail = new StackPane(thumbImage);
        thumbnail.setPrefSize(76, 66);
        thumbnail.setMinSize(76, 66);
        thumbnail.setMaxSize(76, 66);
        thumbnail.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 5; "
                + "-fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-cursor: hand;");
        return thumbnail;
    }

    private static void updateThumbnailSelection(List<StackPane> thumbnailNodes, int selectedIndex) {
        for (int i = 0; i < thumbnailNodes.size(); i++) {
            String borderColor = i == selectedIndex ? "#d4af37" : "#d1d5db";
            thumbnailNodes.get(i).setStyle("-fx-background-color: #ffffff; -fx-background-radius: 5; "
                    + "-fx-border-color: " + borderColor + "; -fx-border-width: 2; "
                    + "-fx-border-radius: 5; -fx-cursor: hand;");
        }
    }

    private static VBox createProductInfoPane(JsonObject item) {
        VBox infoPane = new VBox(14);
        infoPane.setPrefWidth(430);

        Label descriptionTitle = sectionTitle("Description");
        Label description = new Label(JsonUtil.getString(item, "description"));
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #374151; -fx-line-spacing: 3;");

        Label detailsTitle = sectionTitle("Details");
        VBox details = new VBox(10);
        details.getChildren().add(detailRow("Tên sản phẩm", JsonUtil.getString(item, "name")));
        details.getChildren().add(detailRow("Loại", JsonUtil.getString(item, "type")));
        details.getChildren().add(detailRow("Tình trạng", formatCondition(JsonUtil.getString(item, "condition"))));
        details.getChildren().add(detailRow("Giá khởi điểm", String.format("%,.0f đ", getStartingPrice(item))));
        details.getChildren().add(detailRow("Bước giá", String.format("%,.0f đ", getBidStep(item))));
        details.getChildren().add(detailRow("Người bán", JsonUtil.getString(item, "sellerFullName")));

        if (item.has("specifications") && item.get("specifications").isJsonObject()) {
            JsonObject specs = item.getAsJsonObject("specifications");
            for (String key : specs.keySet()) {
                details.getChildren().add(detailRow(key, specs.get(key).getAsString()));
            }
        }

        infoPane.getChildren().addAll(descriptionTitle, description, new Separator(), detailsTitle, details);
        return infoPane;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 16px; -fx-font-weight: 800;");
        return label;
    }

    private static HBox detailRow(String name, String value) {
        Label keyLabel = new Label(name);
        keyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label valueLabel = new Label(value == null || value.isBlank() ? "N/A" : value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-text-fill: #020617; -fx-font-size: 12px;");
        valueLabel.setMaxWidth(250);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, keyLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String formatCondition(String conditionText) {
        if (conditionText == null || conditionText.isBlank()) {
            return "N/A";
        }

        return switch (conditionText) {
            case "NEW" -> "Mới 100%";
            case "USED" -> "Đã sử dụng";
            default -> conditionText;
        };
    }

    private static double getStartingPrice(JsonObject item) {
        return item.has("startingPrice") && !item.get("startingPrice").isJsonNull()
                ? item.get("startingPrice").getAsDouble() : 0.0;
    }

    private static double getBidStep(JsonObject item) {
        return item.has("bidStep") && !item.get("bidStep").isJsonNull()
                ? item.get("bidStep").getAsDouble() : 0.0;
    }

    private static Image loadImage(String imagePath) {
        return new Image(new File(imagePath).toURI().toString());
    }

    private static List<String> getExistingImagePaths(JsonArray images) {
        List<String> imagePaths = new ArrayList<>();
        for (JsonElement imageElement : images) {
            if (imageElement == null || imageElement.isJsonNull()) {
                continue;
            }

            String imagePath = imageElement.getAsString();
            if (imagePath == null || imagePath.isBlank()) {
                continue;
            }

            File file = new File(imagePath);
            if (!file.exists()) {
                continue;
            }

            imagePaths.add(imagePath);
        }
        return imagePaths;
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
        content.setPrefWidth(1120);
        content.setPrefHeight(520);
        content.setStyle("-fx-padding: 12;");

        Label summary = new Label(rows.isEmpty()
                ? "Chưa có lượt đấu giá nào cho phiên này."
                : "Tổng số lượt đấu giá: " + rows.size());
        summary.setStyle("-fx-font-weight: bold;");

        TableView<AdminBidHistoryRow> table = new TableView<>(rows);
        table.setPrefWidth(1096);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có dữ liệu lịch sử đấu giá."));

        TableColumn<AdminBidHistoryRow, Integer> colNo = new TableColumn<>("#");
        colNo.setCellValueFactory(new PropertyValueFactory<>("index"));
        colNo.setPrefWidth(55);

        TableColumn<AdminBidHistoryRow, String> colBidder = new TableColumn<>("Người đấu giá");
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colBidder.setPrefWidth(240);

        TableColumn<AdminBidHistoryRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(220);

        TableColumn<AdminBidHistoryRow, String> colAmount = new TableColumn<>("Số tiền");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setPrefWidth(140);
        colAmount.setStyle("-fx-alignment: CENTER_RIGHT;");

        TableColumn<AdminBidHistoryRow, String> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTime.setPrefWidth(210);

        TableColumn<AdminBidHistoryRow, String> colType = new TableColumn<>("Loại");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(110);

        TableColumn<AdminBidHistoryRow, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(120);
        setupBidHistoryWrapColumn(colBidder);
        setupBidHistoryWrapColumn(colEmail);
        setupBidHistoryWrapColumn(colTime);
        setupBidHistoryWrapColumn(colType);
        setupBidHistoryWrapColumn(colStatus);

        table.getColumns().addAll(colNo, colBidder, colEmail, colAmount, colTime, colType, colStatus);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(summary, table);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static void setupBidHistoryWrapColumn(TableColumn<AdminBidHistoryRow, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(col.widthProperty().subtract(18));
                label.setStyle("-fx-text-fill: #111827;");
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                label.setText(value);
                setGraphic(label);
                setText(null);
            }
        });
    }
}
