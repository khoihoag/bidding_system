package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

public class AuctionCardController {

    @FXML private VBox cardRoot;
    @FXML private ImageView productImageView;
    @FXML private Label lblImageFallback;
    @FXML private Label lblStatus;
    @FXML private Label lblCategory;
    @FXML private Label lblName;
    @FXML private Label lblPrimaryPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private CountdownTimer countdownTimer;
    @FXML private Label lblMeta;
    @FXML private Label lblSeller;
    @FXML private Button btnWatch;

    private SceneBidder1.ProductData currentProduct;
    private Runnable onOpenAction;
    private Consumer<Integer> onToggleWatchAction;

    @FXML
    private void handleOpenCard() {
        if (onOpenAction != null) {
            onOpenAction.run();
        }
    }

    @FXML
    private void handleToggleWatch() {
        if (currentProduct == null) {
            return;
        }

        String json = String.format("{\"action\": \"TOGGLE_WATCH\", \"itemId\": %d}", currentProduct.id);
        NetworkClient.send(json);

        if (onToggleWatchAction != null) {
            onToggleWatchAction.accept(currentProduct.id);
        }
    }

    public void configure(SceneBidder1.ProductData product, boolean watched, Runnable onOpenAction, Consumer<Integer> onToggleWatchAction) {
        this.currentProduct = product;
        this.onOpenAction = onOpenAction;
        this.onToggleWatchAction = onToggleWatchAction;

        lblCategory.setText(product.category);
        lblName.setText(product.name);
        lblPrimaryPrice.setText("Khoi diem: " + formatPrice(safeParseLong(product.startPrice)) + " d");

        boolean hasCurrentBid = product.bidCount > 0 || safeParseLong(product.currentPrice) > safeParseLong(product.startPrice);
        lblCurrentPrice.setVisible(hasCurrentBid);
        lblCurrentPrice.setManaged(hasCurrentBid);
        if (hasCurrentBid) {
            lblCurrentPrice.setText("Gia hien tai: " + formatPrice(safeParseLong(product.currentPrice)) + " d");
        } else {
            lblCurrentPrice.setText("");
        }

        lblMeta.setText(product.bidCount + " luot bid  |  " + product.participants + " nguoi");
        lblSeller.setText("Nguoi ban: " + product.seller);

        if (countdownTimer != null) {
            countdownTimer.configure(product.startTime, product.endTime, true);
        }

        updateStatus(product.getStatusType());
        updateWatchButton(watched);
        loadImage(product);
    }

    public VBox getRoot() {
        return cardRoot;
    }

    public void updateWatchButton(boolean watched) {
        btnWatch.setText(watched ? "Bo theo doi" : "Theo doi");
        btnWatch.setStyle(watched
                ? "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 6 12; -fx-cursor: hand;"
                : "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 6 12; -fx-cursor: hand;");
    }

    private void updateStatus(String status) {
        switch (status) {
            case "RUNNING" -> lblStatus.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 10; -fx-background-radius: 999;");
            case "UPCOMING" -> lblStatus.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 10; -fx-background-radius: 999;");
            default -> lblStatus.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 10; -fx-background-radius: 999;");
        }
        lblStatus.setText(status);
    }

    private void loadImage(SceneBidder1.ProductData product) {
        if (product.imagePaths == null || product.imagePaths.isEmpty()) {
            showImageFallback();
            return;
        }

        try {
            String imagePath = product.imagePaths.get(0);
            Image image = imagePath.startsWith("http") || imagePath.startsWith("file:")
                    ? new Image(imagePath, true)
                    : new Image(new File(imagePath).toURI().toString(), true);
            productImageView.setImage(image);
            lblImageFallback.setVisible(false);
            lblImageFallback.setManaged(false);
        } catch (Exception exception) {
            showImageFallback();
        }
    }

    private void showImageFallback() {
        productImageView.setImage(null);
        lblImageFallback.setVisible(true);
        lblImageFallback.setManaged(true);
    }

    private String formatPrice(long value) {
        try {
            return String.format("%,d", value).replace(',', '.');
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    public void updatePriceLive(long newPrice, int newBidCount) {
        currentProduct.currentPrice = String.valueOf(newPrice);
        currentProduct.bidCount = newBidCount;

        lblCurrentPrice.setText("Gia hien tai: " + formatPrice(newPrice) + " d");
        lblCurrentPrice.setVisible(true);
        lblCurrentPrice.setManaged(true);
        lblMeta.setText(newBidCount + " luot bid  |  " + currentProduct.participants + " nguoi");
        cardRoot.setStyle("-fx-border-color: #1d4ed8; -fx-border-width: 2; -fx-background-color: white;");
    }

    public void setFinished(String winnerName, long finalPrice) {
        updateStatus("FINISHED");
        lblCurrentPrice.setText("Gia chung cuoc: " + formatPrice(finalPrice) + " d");
        lblCurrentPrice.setVisible(true);
        lblCurrentPrice.setManaged(true);
        lblMeta.setText("Winner: " + (winnerName.equals(Scene1.myname) ? "BAN" : winnerName));
        cardRoot.setOpacity(0.7);
        btnWatch.setDisable(true);
    }

    private long safeParseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
