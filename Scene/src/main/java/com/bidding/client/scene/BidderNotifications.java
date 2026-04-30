package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
    // Gắn renderer và chỉ nhận thông báo thật từ GLOBAL_NOTIFY.
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
                
                // TỐI ƯU: Bind trực tiếp trạng thái hiển thị của chấm xanh với biến "status" của model
                // Khi item.setStatus() được gọi, chấm xanh tự động ẩn/hiện mà không cần refresh lại ListView
                dot.visibleProperty().bind(Bindings.createBooleanBinding(
                        () -> "Chua doc".equals(item.getStatus()), 
                        item.statusProperty()
                ));
                // Ẩn chấm xanh khỏi layout nếu nó không visible để tiết kiệm khoảng trống (tuỳ chọn)
                dot.managedProperty().bind(dot.visibleProperty());

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
        notificationListView.setPlaceholder(new Label("Chưa có thông báo realtime từ server."));
        lblServerNote.setText("Thông báo trên màn này chỉ nhận từ push GLOBAL_NOTIFY. Không còn fake data.");
        NetworkClient.notificationListener = this::handleNotificationResponse;
    }

    // Nhận GLOBAL_NOTIFY từ server và thêm vào list.
    private void handleNotificationResponse(JsonObject response) {
        String action = getString(response, "action", "");
        
        if ("GLOBAL_NOTIFY".equals(action)) {
            // QUAN TRỌNG: Đẩy việc cập nhật giao diện (UI) vào luồng của JavaFX
            Platform.runLater(() -> {
                notificationList.add(0, new Notification(
                        "Chua doc",
                        getString(response, "source", "Hệ thống"),
                        LocalDateTime.now().format(TIME_FORMAT),
                        getString(response, "message", "Có thông báo mới từ server.")
                ));
                // Tự động cuộn lên đầu danh sách để thấy thông báo mới nhất
                notificationListView.scrollTo(0);
            });
            return;
        }
        
        
        if ("ERROR".equals(action)) {
            Platform.runLater(() -> {
                lblServerNote.setText("Thông báo realtime gặp lỗi: " + getString(response, "message", "Server trả về lỗi."));
            });
        }
    }

    @FXML
    // Đánh dấu 1 thông báo đã đọc trên UI.
    void handleMarkAsRead(ActionEvent event) {
        Notification selected = notificationListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("Da doc");
            // Đã bỏ notificationListView.refresh() vì Binding sẽ tự lo việc thay đổi UI.
            return;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Chú ý");
        alert.setHeaderText(null);
        alert.setContentText("Vui lòng chọn một thông báo để đánh dấu.");
        alert.showAndWait();
    }

    @FXML
    // Đánh dấu toàn bộ thông báo đã đọc trên UI.
    void handleMarkAllAsRead(ActionEvent event) {
        for (Notification notification : notificationList) {
            notification.setStatus("Da doc");
        }
        // Đã bỏ notificationListView.refresh() vì Binding sẽ tự lo việc thay đổi UI.
    }

    @FXML
    // Quay lại bidder dashboard.
    void handleBackToBidder(ActionEvent event) {
        try {
            Stage stage = (Stage) notificationListView.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nền tảng đấu giá trực tuyến");
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi điều hướng");
            alert.setHeaderText(null);
            alert.setContentText("Không thể quay lại SceneBidder1.fxml");
            alert.showAndWait();
        }
    }

    // Đọc chuỗi từ JSON response.
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

        // Tạo model thông báo để bind list view.
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

        // Bổ sung các Property Getter để phục vụ việc Binding giao diện
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty sourceProperty() { return source; }
        public SimpleStringProperty timeProperty() { return time; }
        public SimpleStringProperty messageProperty() { return message; }
    }
}