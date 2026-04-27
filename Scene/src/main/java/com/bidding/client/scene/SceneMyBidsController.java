package com.bidding.client.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
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

    private static class BidItem {
        String name;
        String currentPrice;
        String statusType;
        String timeInfo;

        BidItem(String name, String currentPrice, String statusType, String timeInfo) {
            this.name = name;
            this.currentPrice = currentPrice;
            this.statusType = statusType;
            this.timeInfo = timeInfo;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMockData();
        summaryLabel.setText("2 phien dang tham gia, 1 phien sap mo, 2 phien da ket thuc");
    }

    private void loadMockData() {
        activeBidsGrid.getChildren().add(createCard(new BidItem("Tranh son dau co", "5.500.000 d", "ACTIVE", "Con 04:57:16")));
        activeBidsGrid.getChildren().add(createCard(new BidItem("Dong ho Rolex Thuy Sy", "120.000.000 d", "ACTIVE", "Con 01:20:05")));

        upcomingBidsGrid.getChildren().add(createCard(new BidItem("Binh gom su Minh", "Khoi diem: 2.000.000 d", "UPCOMING", "Bat dau luc 20:00 toi nay")));

        pastBidsGrid.getChildren().add(createCard(new BidItem("Laptop Dell XPS 15", "Trung thau: 25.000.000 d", "PAST", "Da ket thuc hom qua")));
        pastBidsGrid.getChildren().add(createCard(new BidItem("Tai nghe Sony WH-1000XM5", "Thua thau: 5.200.000 d", "PAST", "Da ket thuc tuan truoc")));
    }

    private VBox createCard(BidItem item) {
        VBox card = new VBox(8);
        card.setPrefWidth(240);
        card.setPrefHeight(140);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 14; "
                + "-fx-border-color: #d9e2ec; -fx-border-radius: 14;");

        Label lblName = new Label(item.name);
        lblName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #12263a;");
        lblName.setWrapText(true);

        Label lblPrice = new Label(item.currentPrice);
        lblPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #d97706;");

        Label lblTime = new Label(item.timeInfo);

        if ("ACTIVE".equals(item.statusType)) {
            lblTime.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(event -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thong bao");
                alert.setHeaderText("Tinh nang dang phat trien");
                alert.setContentText("Tinh nang vao phong dau gia chi tiet dang duoc cap nhat. Vui long quay lai sau.");
                alert.showAndWait();
            });
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8fbff; -fx-padding: 15; -fx-background-radius: 14; -fx-border-color: #9fc5ea; -fx-border-radius: 14;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 14; -fx-border-color: #d9e2ec; -fx-border-radius: 14;"));
        } else if ("UPCOMING".equals(item.statusType)) {
            lblTime.setStyle("-fx-text-fill: #d97706; -fx-font-style: italic;");
        } else {
            lblTime.setStyle("-fx-text-fill: #64748b;");
            if (item.currentPrice.startsWith("Trung")) {
                lblPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #15803d;");
            } else {
                lblPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");
            }
        }

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(lblName, spacer, lblPrice, lblTime);
        return card;
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) activeBidsGrid.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loi dieu huong");
            alert.setHeaderText(null);
            alert.setContentText("Khong mo duoc SceneBidder1.fxml");
            alert.showAndWait();
        }
    }
}
