package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BidderNotifications implements Initializable {

    @FXML private ListView<Notification> notificationListView;
    @FXML private Label lblServerNote;

    private final ObservableList<Notification> notificationList = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    // Gan renderer va chi nhan thong bao that tu GLOBAL_NOTIFY.
    public void initialize(URL location, ResourceBundle resources) {
        notificationListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Notification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                HBox root = new HBox(15);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 14; -fx-border-color: #d9e2ec; -fx-border-radius: 14;");

                Circle dot = new Circle(6, Color.web("#1d5fa7"));
                dot.setVisible("Chua doc".equals(item.getStatus()));

                VBox textContainer = new VBox(5);
                Label sourceTimeLabel = new Label(item.getSource() + "  ·  " + item.getTime());
                sourceTimeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 13px;");

                Label messageLabel = new Label(item.getMessage());
                messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #12263a;");
                messageLabel.setWrapText(true);

                textContainer.getChildren().addAll(sourceTimeLabel, messageLabel);
                root.getChildren().addAll(dot, textContainer);

                setGraphic(root);
                setStyle("-fx-background-color: transparent; -fx-padding: 6 0 6 0;");
            }
        });

        notificationListView.setItems(notificationList);
        notificationListView.setPlaceholder(new Label("Chua co thong bao realtime tu server. Doc JSON hien chua co action lay lich su thong bao."));
        lblServerNote.setText("Thong bao tren man nay chi nhan tu push GLOBAL_NOTIFY. Khong con fake data.");
        NetworkClient.notificationListener = this::handleNotificationResponse;
    }

    // Nhan GLOBAL_NOTIFY tu server va them vao list.
    private void handleNotificationResponse(JsonObject response) {
        String action = getString(response, "action", "");
        if ("GLOBAL_NOTIFY".equals(action)) {
            notificationList.add(0, new Notification(
                    "Chua doc",
                    getString(response, "source", "He thong"),
                    LocalDateTime.now().format(TIME_FORMAT),
                    getString(response, "message", "Co thong bao moi tu server.")
            ));
            return;
        }
        if ("ERROR".equals(action)) {
            lblServerNote.setText("Thong bao realtime gap loi: " + getString(response, "message", "Server tra ve loi."));
        }
    }

    @FXML
    // Danh dau 1 thong bao da doc tren UI.
    void handleMarkAsRead(ActionEvent event) {
        Notification selected = notificationListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("Da doc");
            notificationListView.refresh();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Chu y");
        alert.setHeaderText(null);
        alert.setContentText("Vui long chon mot thong bao de danh dau.");
        alert.showAndWait();
    }

    @FXML
    // Danh dau toan bo thong bao da doc tren UI.
    void handleMarkAllAsRead(ActionEvent event) {
        for (Notification notification : notificationList) {
            notification.setStatus("Da doc");
        }
        notificationListView.refresh();
    }

    @FXML
    // Quay lai bidder dashboard.
    void handleBackToBidder(ActionEvent event) {
        try {
            Stage stage = (Stage) notificationListView.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loi dieu huong");
            alert.setHeaderText(null);
            alert.setContentText("Khong the quay lai SceneBidder1.fxml");
            alert.showAndWait();
        }
    }

    // Doc chuoi tu JSON response.
    private String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    public static class Notification {
        private final SimpleStringProperty status;
        private final SimpleStringProperty source;
        private final SimpleStringProperty time;
        private final SimpleStringProperty message;

        // Tao model thong bao de bind list view.
        public Notification(String status, String source, String time, String message) {
            this.status = new SimpleStringProperty(status);
            this.source = new SimpleStringProperty(source);
            this.time = new SimpleStringProperty(time);
            this.message = new SimpleStringProperty(message);
        }

        public String getStatus() { return status.get(); }
        public String getSource() { return source.get(); }
        public String getTime() { return time.get(); }
        public String getMessage() { return message.get(); }
        public void setStatus(String newStatus) { status.set(newStatus); }
    }
}
