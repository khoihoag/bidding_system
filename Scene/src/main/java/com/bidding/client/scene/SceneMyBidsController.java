package com.bidding.client.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SceneMyBidsController implements Initializable {

    @FXML private FlowPane activeBidsGrid;
    @FXML private FlowPane upcomingBidsGrid;
    @FXML private FlowPane pastBidsGrid;
    @FXML private Label summaryLabel;

    @Override
    // Hien ghi chu vi doc JSON hien chua co action My Bids.
    public void initialize(URL location, ResourceBundle resources) {
        summaryLabel.setText("json.docx hien chua co action lay 'Dau Gia Cua Toi', nen man nay khong dung fake data.");
        populateUnsupportedState();
    }

    // Do man nay chua co JSON list, chi hien note thay cho du lieu gia.
    private void populateUnsupportedState() {
        activeBidsGrid.getChildren().setAll(createNoteCard("Dang tham gia", "Can action JSON de lay danh sach phien bidder dang tham gia tu server."));
        upcomingBidsGrid.getChildren().setAll(createNoteCard("Sap tham gia", "Can action JSON de lay danh sach phien sap mo ma bidder da dang ky."));
        pastBidsGrid.getChildren().setAll(createNoteCard("Da tham gia", "Can action JSON de lay lich su phien bidder da tham gia."));
    }

    // Tao card ghi chu thieu JSON cho tung nhom.
    private VBox createNoteCard(String title, String message) {
        VBox card = new VBox(8);
        card.setPrefWidth(300);
        card.setPrefHeight(150);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 14; "
                + "-fx-border-color: #d9e2ec; -fx-border-radius: 14;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #12263a;");
        lblTitle.setWrapText(true);

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-text-fill: #64748b;");
        lblMessage.setWrapText(true);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(lblTitle, spacer, lblMessage);
        return card;
    }

    @FXML
    // Quay ve trang bidder.
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) activeBidsGrid.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loi dieu huong");
            alert.setHeaderText(null);
            alert.setContentText("Khong mo duoc SceneBidder1.fxml");
            alert.showAndWait();
        }
    }
}
