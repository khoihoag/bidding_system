package com.bidding.client.scene;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class LoadingOverlayController {

    @FXML private StackPane overlayRoot;
    @FXML private Label lblMessage;

    public void show(String message) {
        lblMessage.setText(message);
        overlayRoot.setVisible(true);
        overlayRoot.setManaged(true);
    }

    public void hide() {
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
    }
}
