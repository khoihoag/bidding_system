package com.bidding.util;

import com.bidding.model.UserSession;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Premium winner announcement dialog for finished auctions.
 */
public final class WinnerCelebrationDialog {

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    private static final Set<String> SHOWN_DIALOG_KEYS = ConcurrentHashMap.newKeySet();

    private WinnerCelebrationDialog() {
    }

    public static void show(JsonObject payload, Class<?> resourceClass) {
        if (!markDialogPending(payload)) {
            return;
        }
        Platform.runLater(() -> open(payload, resourceClass));
    }

    private static void open(JsonObject json, Class<?> resourceClass) {
        String status = getString(json, "status", "FINISHED");
        if ("CANCELED".equals(status)) {
            return;
        }

        String winnerUserId = getString(json, "winnerUserId", "");
        String winnerUsername = getString(json, "winnerUsername", getString(json, "winnerId", "---"));
        String winnerName = getString(json, "winnerName", winnerUsername);
        String winnerDisplay = buildWinnerDisplay(winnerName, winnerUsername);
        String itemName = getString(json, "itemName", "Vật phẩm đấu giá");
        double finalPrice = json.has("finalPrice") && !json.get("finalPrice").isJsonNull()
                ? json.get("finalPrice").getAsDouble()
                : 0;

        boolean noWinner = isEmptyWinner(winnerDisplay);
        boolean currentUserWon = !winnerUserId.isBlank()
                && winnerUserId.equals(UserSession.getInstance().getUserId());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("BidVault Auction House");
        dialog.setResizable(false);

        ButtonType closeType = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(closeType);

        Label emblem = new Label("🏆");
        emblem.getStyleClass().add("winner-dialog-emblem");
        StackPane emblemWrap = new StackPane(emblem);
        emblemWrap.getStyleClass().add("winner-dialog-emblem-wrap");

        Label kicker = new Label("AUCTION CLOSED");
        kicker.getStyleClass().add("winner-dialog-kicker");

        Label title = new Label(currentUserWon ? "Bạn đã thắng phiên đấu giá" : "Phiên đấu giá đã kết thúc");
        title.getStyleClass().add("winner-dialog-title");

        Label subtitle = new Label(noWinner
                ? "Không có ai đặt giá trong phiên này."
                : "Kết quả phiên đấu giá đã được xác nhận chính thức.");
        subtitle.getStyleClass().add("winner-dialog-subtitle");
        subtitle.setWrapText(true);

        Label itemCaption = new Label("Vật phẩm");
        itemCaption.getStyleClass().add("winner-dialog-field-caption");
        Label itemValue = new Label(itemName);
        itemValue.getStyleClass().add("winner-dialog-item-name");
        itemValue.setWrapText(true);

        Label winnerCaption = new Label("Người chiến thắng");
        winnerCaption.getStyleClass().add("winner-dialog-field-caption");
        Label winnerValue = new Label(noWinner ? "Chưa có" : winnerDisplay);
        winnerValue.getStyleClass().add(noWinner ? "winner-dialog-winner-muted" : "winner-dialog-winner-name");

        Label priceCaption = new Label("Giá chốt");
        priceCaption.getStyleClass().add("winner-dialog-field-caption");
        Label priceValue = new Label(finalPrice > 0 ? CURRENCY_FMT.format(finalPrice) + " đ" : "---");
        priceValue.getStyleClass().add("winner-dialog-price-value");

        VBox spotlight = new VBox(12,
                row("winner-dialog-spotlight-row", itemCaption, itemValue),
                row("winner-dialog-spotlight-row", winnerCaption, winnerValue),
                row("winner-dialog-spotlight-row", priceCaption, priceValue)
        );
        spotlight.getStyleClass().add("winner-dialog-spotlight");

        Label note = new Label(currentUserWon
                ? "Bạn có thể vào chi tiết phiên để thanh toán trong thời hạn quy định."
                : "Cảm ơn bạn đã tham gia phiên đấu giá.");
        note.getStyleClass().add("winner-dialog-note");
        note.setWrapText(true);

        VBox content = new VBox(16, emblemWrap, kicker, title, subtitle, spotlight, note);
        content.getStyleClass().add("winner-dialog-content");
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(4, 4, 0, 4));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setHeader(null);
        applyStylesheet(dialog.getDialogPane(), resourceClass);
        dialog.getDialogPane().getStyleClass().add("winner-dialog-pane");

        dialog.setOnShown(e -> {
            var closeBtn = dialog.getDialogPane().lookupButton(closeType);
            if (closeBtn != null) {
                closeBtn.getStyleClass().add("winner-dialog-close");
            }
        });

        dialog.showAndWait();
    }

    private static VBox row(String styleClass, Label caption, Label value) {
        VBox box = new VBox(4, caption, value);
        box.getStyleClass().add(styleClass);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static boolean markDialogPending(JsonObject json) {
        String status = getString(json, "status", "FINISHED");
        if ("CANCELED".equals(status)) {
            return false;
        }

        String auctionId = getString(json, "auctionId", "");
        if (auctionId.isBlank()) {
            auctionId = getString(json, "itemName", "unknown") + ":" + getString(json, "finalPrice", "0");
        }

        UserSession session = UserSession.getInstance();
        String accountKey = session.getUserId() != null && !session.getUserId().isBlank()
                ? session.getUserId()
                : session.getUsername();
        if (accountKey == null || accountKey.isBlank()) {
            accountKey = "anonymous";
        }

        return SHOWN_DIALOG_KEYS.add(accountKey + ":" + auctionId);
    }

    private static String getString(JsonObject json, String key, String fallback) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return fallback;
    }

    private static String buildWinnerDisplay(String fullName, String username) {
        if (isEmptyWinner(username) && isEmptyWinner(fullName)) {
            return "Không có người đặt giá";
        }
        if (fullName != null && !fullName.isBlank()
                && username != null && !username.isBlank()
                && !fullName.equals(username)
                && !isEmptyWinner(username)) {
            return fullName + " (" + username + ")";
        }
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return username;
    }

    private static boolean isEmptyWinner(String winner) {
        if (winner == null || winner.isBlank()) {
            return true;
        }
        return "---".equals(winner)
                || "NONE".equalsIgnoreCase(winner)
                || "Không có người đặt giá".equalsIgnoreCase(winner);
    }

    private static void applyStylesheet(DialogPane pane, Class<?> resourceClass) {
        try {
            pane.getStylesheets().add(resourceClass.getResource("/css/style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Lỗi load CSS cho dialog chiến thắng!");
        }
    }
}
