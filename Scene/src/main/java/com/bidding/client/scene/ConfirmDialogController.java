package com.bidding.client.scene;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class ConfirmDialogController {

    @FXML private StackPane dialogRoot;
    @FXML private Label lblTitle;
    @FXML private Label lblMessage;
    @FXML private Label lblDbNotice;
    @FXML private Button btnConfirm;

    private Runnable onConfirmAction;
    private Runnable onCancelAction;

    public void show(String title, String message, String confirmText, String dbNotice, Runnable onConfirmAction, Runnable onCancelAction) {
        this.onConfirmAction = onConfirmAction;
        this.onCancelAction = onCancelAction;
        lblTitle.setText(title);
        lblMessage.setText(message);
        btnConfirm.setText(confirmText);
        boolean hasDbNotice = dbNotice != null && !dbNotice.isBlank();
        lblDbNotice.setVisible(hasDbNotice);
        lblDbNotice.setManaged(hasDbNotice);
        lblDbNotice.setText(dbNotice == null ? "" : dbNotice);
        dialogRoot.setVisible(true);
        dialogRoot.setManaged(true);
    }

    @FXML
    private void handleConfirm() {
        hide();
        if (onConfirmAction != null) onConfirmAction.run();
    }

    @FXML
    private void handleCancel() {
        hide();
        if (onCancelAction != null) onCancelAction.run();
    }

    public void hide() {
        dialogRoot.setVisible(false);
        dialogRoot.setManaged(false);
    }
}
