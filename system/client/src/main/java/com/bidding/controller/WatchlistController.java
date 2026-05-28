package com.bidding.controller;

import com.bidding.model.UserSession;
import com.bidding.network.NetworkClient;
import com.bidding.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WatchlistController {
    @FXML private TableView<WatchlistRow> watchlistTable;
    @FXML private TableColumn<WatchlistRow, String> colAuctionId;
    @FXML private TableColumn<WatchlistRow, String> colProductName;
    @FXML private TableColumn<WatchlistRow, String> colCurrentPrice;
    @FXML private TableColumn<WatchlistRow, String> colLeader;
    @FXML private TableColumn<WatchlistRow, String> colTime;
    @FXML private TableColumn<WatchlistRow, Void> colAction;
    @FXML private Label statusLabel;

    private final NetworkClient networkClient = NetworkClient.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter HISTORY_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final ObservableList<WatchlistRow> watchlistRows = FXCollections.observableArrayList();
    private JsonArray followedAuctions = new JsonArray();
    private Timeline countdownTimeline;
    private String pendingHistoryAuctionId;
    private String pendingHistoryAuctionName;

    @FXML
    public void initialize() {
        setupTable();
        networkClient.setMessageHandler(this::handleServerMessage);
        showStatus("\u0110ang t\u1ea3i danh s\u00e1ch theo d\u00f5i...");
        requestAuctions();
    }

    @FXML
    private void handleRefresh() {
        networkClient.setMessageHandler(this::handleServerMessage);
        showStatus("\u0110ang t\u1ea3i l\u1ea1i...");
        requestAuctions();
    }

    private void setupTable() {
        watchlistTable.getStyleClass().add("watchlist-table");
        colAuctionId.setCellValueFactory(data -> data.getValue().auctionIdProperty());
        colProductName.setCellValueFactory(data -> data.getValue().productNameProperty());
        colCurrentPrice.setCellValueFactory(data -> data.getValue().currentPriceProperty());
        colLeader.setCellValueFactory(data -> data.getValue().leaderProperty());
        colTime.setCellValueFactory(data -> data.getValue().timeDisplayProperty());

        colAuctionId.setStyle("-fx-alignment: CENTER_LEFT;");
        colCurrentPrice.setStyle("-fx-alignment: CENTER_RIGHT;");
        colLeader.setStyle("-fx-alignment: CENTER;");
        colTime.setStyle("-fx-alignment: CENTER;");

        setupWrappedHeader(colProductName, "T\u00ean s\u1ea3n ph\u1ea9m");
        setupWrappedHeader(colCurrentPrice, "Gi\u00e1 tr\u1ecb hi\u1ec7n t\u1ea1i");
        setupWrappedHeader(colLeader, "Ng\u01b0\u1eddi \u0111ang d\u1eabn \u0111\u1ea7u");
        setupWrappedHeader(colTime, "Th\u1eddi gian");
        setupWrappedHeader(colAction, "Chi ti\u1ebft");
        setupWrappedTextColumn(colProductName);
        setupWrappedTextColumn(colLeader);

        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button();

            {
                viewButton.getStyleClass().add("secondary-button");
                viewButton.setOnAction(event -> {
                    WatchlistRow row = getTableView().getItems().get(getIndex());
                    if (row.isEnded(LocalDateTime.now())) {
                        handleViewAuctionHistory(row);
                    } else {
                        handleViewAuction(row.getAuctionId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                WatchlistRow row = getTableView().getItems().get(getIndex());
                viewButton.setText(row.isEnded(LocalDateTime.now()) ? "Xem l\u1ecbch s\u1eed" : "Xem chi ti\u1ebft");
                setGraphic(viewButton);
            }
        });

        watchlistTable.setItems(watchlistRows);
        watchlistTable.setPlaceholder(new Label("Ch\u01b0a c\u00f3 s\u1ea3n ph\u1ea9m n\u00e0o trong danh s\u00e1ch theo d\u00f5i."));
    }

    private void setupWrappedHeader(TableColumn<?, ?> column, String text) {
        Label header = new Label(text);
        header.setWrapText(true);
        header.setMaxWidth(140);
        header.setStyle("-fx-text-alignment: center;");
        column.setText(null);
        column.setGraphic(header);
    }

    private void setupWrappedTextColumn(TableColumn<WatchlistRow, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(col.widthProperty().subtract(18));
                label.getStyleClass().add("watchlist-table-cell-label");
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                label.setText(value);
                setText(null);
                setGraphic(label);
            }
        });
    }

    private void requestAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        networkClient.sendJson(request);
    }

    private void handleServerMessage(JsonObject json) {
        if (json.has("balance")) {
            double newBalance = json.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(newBalance);
            if (MainController.getInstance() != null) {
                MainController.getInstance().updateBalanceDisplay();
            }
        }
        if (!json.has("action")) {
            return;
        }

        switch (json.get("action").getAsString()) {
            case "AUCTIONS_LIST" -> handleAuctionsList(json);
            case "AUCTION_HISTORY_REPLY" -> handleAuctionHistoryReply(json);
            case "REALTIME_BID_UPDATE", "NEW_BID" -> handleRealtimeBidUpdate(json);
            case "GLOBAL_NOTIFY" -> requestAuctions();
            case "ERROR" -> Platform.runLater(() -> showStatus(json.has("message")
                    ? json.get("message").getAsString()
                    : "Server error"));
            default -> { }
        }
    }

    private void handleAuctionsList(JsonObject json) {
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            return;
        }
        JsonArray data = json.getAsJsonArray("data");
        Platform.runLater(() -> {
            followedAuctions = filterFollowedAuctions(data);
            renderTable(followedAuctions);
        });
    }

    private JsonArray filterFollowedAuctions(JsonArray source) {
        JsonArray filtered = new JsonArray();
        for (JsonElement element : source) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (auction.has("isFollowing") && auction.get("isFollowing").getAsBoolean()) {
                filtered.add(auction.deepCopy());
            }
        }
        return filtered;
    }

    private void renderTable(JsonArray data) {
        stopCountdowns();
        watchlistRows.clear();

        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            String auctionId = getStringSafe(auction, "id");
            String itemName = "\u0053\u1ea3n ph\u1ea9m kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
            if (auction.has("item") && auction.get("item").isJsonObject()) {
                itemName = getStringSafe(auction.getAsJsonObject("item"), "name");
            }

            watchlistRows.add(new WatchlistRow(
                    auctionId,
                    itemName,
                    formatCurrency(auction.has("currentPrice") ? auction.get("currentPrice").getAsDouble() : 0.0),
                    getLeader(auction),
                    getStringSafe(auction, "status"),
                    parseDateTime(getStringSafe(auction, "startTime")),
                    parseDateTime(getStringSafe(auction, "endTime"))
            ));
        }

        tickCountdowns();
        if (watchlistRows.isEmpty()) {
            showStatus("Ch\u01b0a c\u00f3 s\u1ea3n ph\u1ea9m n\u00e0o trong danh s\u00e1ch theo d\u00f5i.");
        } else {
            showStatus("Hi\u1ec3n th\u1ecb " + watchlistRows.size() + " s\u1ea3n ph\u1ea9m \u0111ang theo d\u00f5i.");
            startCountdowns();
        }
    }

    private void handleRealtimeBidUpdate(JsonObject json) {
        String auctionId = getStringSafe(json, "auctionId");
        double newPrice = json.has("newPrice") ? json.get("newPrice").getAsDouble() : 0.0;
        String winner = getStringSafe(json, "winnerId");
        String newEndTime = getStringSafe(json, "newEndTime");

        Platform.runLater(() -> {
            updateCachedAuction(auctionId, newPrice, winner, newEndTime);
            for (WatchlistRow row : watchlistRows) {
                if (row.getAuctionId().equals(auctionId)) {
                    row.setCurrentPrice(formatCurrency(newPrice));
                    if (!winner.isBlank()) {
                        row.setLeader(winner);
                    }
                    LocalDateTime parsedEndTime = parseDateTime(newEndTime);
                    if (parsedEndTime != null) {
                        row.setEndTime(parsedEndTime);
                    }
                    row.updateTime(LocalDateTime.now());
                    break;
                }
            }
        });
    }

    private void updateCachedAuction(String auctionId, double newPrice, String winner, String newEndTime) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }
        for (JsonElement element : followedAuctions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (auctionId.equals(getStringSafe(auction, "id"))) {
                auction.addProperty("currentPrice", newPrice);
                if (winner != null && !winner.isBlank()) {
                    auction.addProperty("winnerId", winner);
                }
                if (newEndTime != null && !newEndTime.isBlank()) {
                    auction.addProperty("endTime", newEndTime);
                }
                return;
            }
        }
    }

    private void handleViewAuction(String auctionId) {
        AppNavigator.passData(auctionId);
        AppNavigator.navigate("AuctionDetail.fxml");
    }

    private void handleViewAuctionHistory(WatchlistRow row) {
        pendingHistoryAuctionId = row.getAuctionId();
        pendingHistoryAuctionName = row.getProductName();
        showStatus("\u0110ang t\u1ea3i l\u1ecbch s\u1eed phi\u00ean " + pendingHistoryAuctionId + "...");

        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTION_HISTORY");
        request.addProperty("auctionId", pendingHistoryAuctionId);
        networkClient.sendJson(request);
    }

    private void handleAuctionHistoryReply(JsonObject json) {
        JsonArray data = json.has("data") && json.get("data").isJsonArray()
                ? json.getAsJsonArray("data")
                : new JsonArray();
        String auctionId = pendingHistoryAuctionId == null ? "" : pendingHistoryAuctionId;
        String auctionName = pendingHistoryAuctionName == null ? "" : pendingHistoryAuctionName;

        Platform.runLater(() -> {
            showHistoryDialog(auctionId, auctionName, data);
            if (watchlistRows.isEmpty()) {
                showStatus("Ch\u01b0a c\u00f3 s\u1ea3n ph\u1ea9m n\u00e0o trong danh s\u00e1ch theo d\u00f5i.");
            } else {
                showStatus("Hi\u1ec3n th\u1ecb " + watchlistRows.size() + " s\u1ea3n ph\u1ea9m \u0111ang theo d\u00f5i.");
            }
        });
    }

    private void showHistoryDialog(String auctionId, String auctionName, JsonArray data) {
        ObservableList<AuctionHistoryRow> rows = FXCollections.observableArrayList();
        int index = 1;
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject point = element.getAsJsonObject();
            rows.add(new AuctionHistoryRow(
                    String.valueOf(index++),
                    getStringSafe(point, "bidder").isBlank() ? "---" : getStringSafe(point, "bidder"),
                    formatCurrency(point.has("price") ? point.get("price").getAsDouble() : 0.0),
                    formatHistoryTime(getStringSafe(point, "timestamp")),
                    mapBidType(getStringSafe(point, "type")),
                    mapBidStatus(getStringSafe(point, "status"))
            ));
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("L\u1ecbch s\u1eed \u0111\u1ea5u gi\u00e1");
        dialog.setHeaderText((auctionName == null || auctionName.isBlank() ? "Phi\u00ean \u0111\u1ea5u gi\u00e1" : auctionName)
                + "\nID: " + auctionId);
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {
        }

        VBox content = new VBox(12);
        content.getStyleClass().add("watchlist-history-dialog");
        content.setPrefWidth(880);
        content.setPrefHeight(460);

        Label summary = new Label(rows.isEmpty()
                ? "Ch\u01b0a c\u00f3 l\u01b0\u1ee3t \u0111\u1ea5u gi\u00e1 n\u00e0o cho phi\u00ean n\u00e0y."
                : "T\u1ed5ng s\u1ed1 l\u01b0\u1ee3t \u0111\u1ea5u gi\u00e1: " + rows.size());
        summary.getStyleClass().add("watchlist-history-summary");

        TableView<AuctionHistoryRow> table = new TableView<>(rows);
        table.getStyleClass().addAll("table-view", "watchlist-history-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u l\u1ecbch s\u1eed \u0111\u1ea5u gi\u00e1."));

        TableColumn<AuctionHistoryRow, String> colNo = new TableColumn<>("#");
        colNo.setCellValueFactory(dataRow -> dataRow.getValue().indexProperty());
        colNo.setPrefWidth(48);
        colNo.setStyle("-fx-alignment: CENTER;");

        TableColumn<AuctionHistoryRow, String> colBidder = new TableColumn<>("Ng\u01b0\u1eddi \u0111\u1ea5u gi\u00e1");
        colBidder.setCellValueFactory(dataRow -> dataRow.getValue().bidderProperty());
        colBidder.setPrefWidth(190);

        TableColumn<AuctionHistoryRow, String> colAmount = new TableColumn<>("Gi\u00e1 tr\u1ecb");
        colAmount.setCellValueFactory(dataRow -> dataRow.getValue().amountProperty());
        colAmount.setPrefWidth(150);
        colAmount.setStyle("-fx-alignment: CENTER_RIGHT;");

        TableColumn<AuctionHistoryRow, String> colTime = new TableColumn<>("Th\u1eddi gian");
        colTime.setCellValueFactory(dataRow -> dataRow.getValue().timeProperty());
        colTime.setPrefWidth(190);

        TableColumn<AuctionHistoryRow, String> colType = new TableColumn<>("Lo\u1ea1i");
        colType.setCellValueFactory(dataRow -> dataRow.getValue().typeProperty());
        colType.setPrefWidth(110);

        TableColumn<AuctionHistoryRow, String> colStatus = new TableColumn<>("Tr\u1ea1ng th\u00e1i");
        colStatus.setCellValueFactory(dataRow -> dataRow.getValue().statusProperty());
        colStatus.setPrefWidth(130);

        table.getColumns().addAll(colNo, colBidder, colAmount, colTime, colType, colStatus);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(summary, table);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void startCountdowns() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdowns()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdowns() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void tickCountdowns() {
        LocalDateTime now = LocalDateTime.now();
        for (WatchlistRow row : watchlistRows) {
            row.updateTime(now);
        }
        watchlistTable.refresh();
    }

    private String getLeader(JsonObject auction) {
        String winner = getStringSafe(auction, "winnerId");
        return winner.isBlank() ? "---" : winner;
    }

    private String formatCurrency(double value) {
        return currencyFmt.format(value) + " \u0111";
    }

    private String formatHistoryTime(String value) {
        LocalDateTime parsed = parseDateTime(value);
        return parsed == null ? "---" : parsed.format(HISTORY_TIME_FMT);
    }

    private String mapBidType(String type) {
        return switch (type == null ? "" : type.toUpperCase()) {
            case "AUTO" -> "Auto-bid";
            case "MANUAL" -> "Th\u1ee7 c\u00f4ng";
            default -> type == null || type.isBlank() ? "---" : type;
        };
    }

    private String mapBidStatus(String status) {
        return switch (status == null ? "" : status.toUpperCase()) {
            case "ACCEPTED" -> "Ch\u1ea5p nh\u1eadn";
            case "REJECTED" -> "T\u1eeb ch\u1ed1i";
            case "PENDING" -> "Ch\u1edd x\u1eed l\u00fd";
            default -> status == null || status.isBlank() ? "---" : status;
        };
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getStringSafe(JsonObject obj, String key) {
        return JsonUtil.getString(obj, key);
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private static final class WatchlistRow {
        private final StringProperty auctionId;
        private final StringProperty productName;
        private final StringProperty currentPrice;
        private final StringProperty leader;
        private final StringProperty timeDisplay;
        private final String status;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;

        private WatchlistRow(String auctionId,
                             String productName,
                             String currentPrice,
                             String leader,
                             String status,
                             LocalDateTime startTime,
                             LocalDateTime endTime) {
            this.auctionId = new SimpleStringProperty(auctionId);
            this.productName = new SimpleStringProperty(productName);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.leader = new SimpleStringProperty(leader);
            this.timeDisplay = new SimpleStringProperty("");
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private String getAuctionId() {
            return auctionId.get();
        }

        private String getProductName() {
            return productName.get();
        }

        private StringProperty auctionIdProperty() {
            return auctionId;
        }

        private StringProperty productNameProperty() {
            return productName;
        }

        private StringProperty currentPriceProperty() {
            return currentPrice;
        }

        private void setCurrentPrice(String value) {
            currentPrice.set(value);
        }

        private StringProperty leaderProperty() {
            return leader;
        }

        private void setLeader(String value) {
            leader.set(value);
        }

        private StringProperty timeDisplayProperty() {
            return timeDisplay;
        }

        private void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        private void updateTime(LocalDateTime now) {
            if (isEnded(now)) {
                timeDisplay.set("K\u1ebft th\u00fac");
                return;
            }
            if (isUpcoming(now)) {
                timeDisplay.set("Ch\u01b0a di\u1ec5n ra");
                return;
            }
            if (endTime == null) {
                timeDisplay.set("--:--:--");
                return;
            }
            java.time.Duration remaining = java.time.Duration.between(now, endTime);
            if (remaining.isNegative() || remaining.isZero()) {
                timeDisplay.set("K\u1ebft th\u00fac");
                return;
            }

            long days = remaining.toDays();
            long hours = remaining.toHoursPart();
            String clock = String.format("%02d:%02d:%02d",
                    hours, remaining.toMinutesPart(), remaining.toSecondsPart());
            timeDisplay.set(days > 0 ? days + " ng\u00e0y " + clock : clock);
        }

        private boolean isEnded(LocalDateTime now) {
            if ("FINISHED".equals(status) || "CLOSED".equals(status) || "CANCELED".equals(status)
                    || "CANCELLED".equals(status) || "PAID".equals(status) || "FAILED".equals(status)) {
                return true;
            }
            return endTime != null && now.isAfter(endTime);
        }

        private boolean isUpcoming(LocalDateTime now) {
            if ("OPEN".equals(status) || "SCHEDULED".equals(status)) {
                return true;
            }
            return startTime != null && now.isBefore(startTime);
        }
    }

    private static final class AuctionHistoryRow {
        private final StringProperty index;
        private final StringProperty bidder;
        private final StringProperty amount;
        private final StringProperty time;
        private final StringProperty type;
        private final StringProperty status;

        private AuctionHistoryRow(String index, String bidder, String amount, String time, String type, String status) {
            this.index = new SimpleStringProperty(index);
            this.bidder = new SimpleStringProperty(bidder);
            this.amount = new SimpleStringProperty(amount);
            this.time = new SimpleStringProperty(time);
            this.type = new SimpleStringProperty(type);
            this.status = new SimpleStringProperty(status);
        }

        private StringProperty indexProperty() {
            return index;
        }

        private StringProperty bidderProperty() {
            return bidder;
        }

        private StringProperty amountProperty() {
            return amount;
        }

        private StringProperty timeProperty() {
            return time;
        }

        private StringProperty typeProperty() {
            return type;
        }

        private StringProperty statusProperty() {
            return status;
        }
    }
}
