package com.bidding.client.scene;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller cho BidResultOverlay.fxml.
 *
 * KÃ­ch hoáº¡t khi nháº­n WebSocket event AUCTION_CLOSED.
 *
 * âš ï¸ Káº¿t ná»‘i DB:
 *  - Sau khi hiá»ƒn thá»‹ "winner" â†’ btnPrimary dáº«n Ä‘áº¿n mÃ n hÃ¬nh Payment
 *  - [DB] GET /api/auctions/{id}/result Ä‘á»ƒ láº¥y thÃ´ng tin ngÆ°á»i tháº¯ng cuá»™c
 */
public class BidResultOverlayController implements Initializable {

    @FXML private StackPane overlayRoot;
    @FXML private HBox hboxBanner;
    @FXML private Label lblBannerIcon;
    @FXML private Label lblResultTitle;
    @FXML private Label lblResultSubtitle;
    @FXML private VBox vboxWinnerInfo;
    @FXML private Label lblWinningPrice;
    @FXML private Label lblProductLabel;
    @FXML private Label lblDbNote;
    @FXML private Button btnPrimary;
    @FXML private Button btnSecondary;

    private final NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Máº·c Ä‘á»‹nh áº©n
    }

    /**
     * Gá»i tá»« BiddingViewController khi nháº­n AUCTION_CLOSED event.
     *
     * @param isWinner     true náº¿u ngÆ°á»i dÃ¹ng hiá»‡n táº¡i tháº¯ng
     * @param productName  TÃªn sáº£n pháº©m
     * @param winningAmount GiÃ¡ tháº¯ng cuá»™c (0 náº¿u khÃ´ng liÃªn quan)
     */
    public void show(boolean isWinner, String productName, long winningAmount) {
        overlayRoot.setVisible(true);
        overlayRoot.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), overlayRoot);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        if (isWinner) {
            // Winner UI
            hboxBanner.setStyle("-fx-background-color: #2E7D32; -fx-background-radius: 8 8 0 0;");
            lblBannerIcon.setText("ðŸ†");
            lblResultTitle.setText("Báº¡n Ä‘Ã£ tháº¯ng!");
            lblResultSubtitle.setText("ChÃºc má»«ng! Báº¡n Ä‘Ã£ chiáº¿n tháº¯ng phiÃªn Ä‘áº¥u giÃ¡.");
            vboxWinnerInfo.setVisible(true);
            vboxWinnerInfo.setManaged(true);
            lblWinningPrice.setText(nf.format(winningAmount) + " VND");
            lblProductLabel.setText(productName);

            btnPrimary.setText("XÃ¡c nháº­n thanh toÃ¡n");
            btnPrimary.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 18; -fx-background-radius: 4; -fx-cursor: hand;");

            // DB note
            lblDbNote.setText("ðŸ—„ [DB] Gá»i POST /api/payment Ä‘á»ƒ khá»Ÿi táº¡o giao dá»‹ch thanh toÃ¡n.");
            lblDbNote.setVisible(true);
            lblDbNote.setManaged(true);

        } else {
            // Loser / viewer UI
            hboxBanner.setStyle("-fx-background-color: #757575; -fx-background-radius: 8 8 0 0;");
            lblBannerIcon.setText("ðŸ””");
            lblResultTitle.setText("PhiÃªn Ä‘áº¥u giÃ¡ Ä‘Ã£ káº¿t thÃºc");
            lblResultSubtitle.setText("Ráº¥t tiáº¿c, báº¡n khÃ´ng tháº¯ng phiÃªn Ä‘áº¥u giÃ¡ nÃ y.\nHÃ£y thá»­ sáº£n pháº©m khÃ¡c nhÃ©!");

            btnPrimary.setText("TÃ¬m sáº£n pháº©m khÃ¡c");
            btnPrimary.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 18; -fx-background-radius: 4; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handlePrimary() {
        // [NAV] Winner â†’ má»Ÿ PaymentView | Others â†’ má»Ÿ AuctionListView
        System.out.println("[NAV] Primary action: navigate to PaymentView or AuctionListView");
        hide();
    }

    @FXML
    private void handleSecondary() {
        // [NAV] Quay vá» danh sÃ¡ch Ä‘áº¥u giÃ¡
        System.out.println("[NAV] Navigate to AuctionListView");
        hide();
    }

    private void hide() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlayRoot);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            overlayRoot.setVisible(false);
            overlayRoot.setManaged(false);
        });
        ft.play();
    }
}
