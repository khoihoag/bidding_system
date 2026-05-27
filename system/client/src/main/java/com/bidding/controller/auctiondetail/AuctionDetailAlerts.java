package com.bidding.controller.auctiondetail;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Styled alert dialogs for auction detail.
 */
public final class AuctionDetailAlerts {

    private AuctionDetailAlerts() {
    }

    public static void show(Alert.AlertType type, String title, String content, Class<?> resourceClass) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            try {
                alert.getDialogPane().getStylesheets()
                        .add(resourceClass.getResource("/css/style.css").toExternalForm());
                alert.getDialogPane().setStyle("-fx-background-color: #05070a;");
            } catch (Exception e) {
                System.err.println("Lỗi load CSS cho Alert");
            }
            alert.showAndWait();
        });
    }
}
