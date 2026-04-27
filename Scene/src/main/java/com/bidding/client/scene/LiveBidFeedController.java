package com.bidding.client.scene;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LiveBidFeedController implements Initializable {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox bidListContainer;
    @FXML private VBox vboxEmpty;

    private static final int MAX_ROWS = 50;
    private final NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
    private int rowCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // [DB] loadInitialBids();
    }

    /**
     * Thêm một hàng bid mới vào đầu danh sách.
     * Phải được gọi từ Platform.runLater() khi đến từ WebSocket thread.
     *
     * @param bidder   Tên người đặt (ẩn danh)
     * @param amount   Số tiền
     * @param time     Định dạng HH:mm:ss
     * @param isLeader true nếu đây là giá dẫn đầu hiện tại
     */
    public void addRow(String bidder, long amount, String time, boolean isLeader) {
        Platform.runLater(() -> {
            // Ẩn trạng thái trống (empty state) khi có dữ liệu
            if (vboxEmpty.isVisible()) {
                vboxEmpty.setVisible(false);
                vboxEmpty.setManaged(false);
            }

            // Giới hạn số lượng hàng hiển thị (tối đa 50 dòng)
            if (bidListContainer.getChildren().size() >= MAX_ROWS) {
                bidListContainer.getChildren().remove(bidListContainer.getChildren().size() - 1);
            }

            HBox row = buildRow(bidder, amount, time, isLeader);
            bidListContainer.getChildren().add(0, row); // Thêm vào trên cùng
            rowCount++;

            // Hiệu ứng Fade in (mờ dần sang hiện rõ)
            FadeTransition ft = new FadeTransition(Duration.millis(250), row);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            // Tự động cuộn lên trên cùng để xem giá mới nhất
            scrollPane.setVvalue(0);
        });
    }

    private HBox buildRow(String bidder, long amount, String time, boolean isLeader) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOpacity(0);

        String bgColor = isLeader ? "#E8F5E9" : "#FFFFFF";
        row.setStyle("-fx-padding: 10 16; -fx-border-color: #F5F5F5; -fx-border-width: 0 0 1 0; -fx-background-color: " + bgColor + ";");

        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #9E9E9E; -fx-min-width: 70px;");

        Label lblBidder = new Label(bidder);
        lblBidder.setStyle("-fx-font-size: 12px; -fx-text-fill: #424242;");
        HBox.setHgrow(lblBidder, Priority.ALWAYS);

        Label lblAmount = new Label(nf.format(amount) + " VND");
        lblAmount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (isLeader ? "#2E7D32" : "#1565C0") + "; -fx-min-width: 120px;");

        // Sửa Label trạng thái: "Dẫn đầu" hoặc "Hợp lệ"
        Label lblStatus = new Label(isLeader ? "Dẫn đầu" : "Hợp lệ");
        lblStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + (isLeader ? "#2E7D32" : "#757575") + "; -fx-min-width: 70px;");

        row.getChildren().addAll(lblTime, lblBidder, lblAmount, lblStatus);
        return row;
    }


    public void clearFeed() {
        bidListContainer.getChildren().clear();
        vboxEmpty.setVisible(true);
        vboxEmpty.setManaged(true);
        rowCount = 0;
    }
}
