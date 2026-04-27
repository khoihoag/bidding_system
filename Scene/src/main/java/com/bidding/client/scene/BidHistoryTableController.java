package com.bidding.client.scene;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class BidHistoryTableController {

    @FXML private TableView<SceneBidder1.BidEntry> bidHistoryTable;
    @FXML private TableColumn<SceneBidder1.BidEntry, String> colBidder;
    @FXML private TableColumn<SceneBidder1.BidEntry, String> colBidPrice;
    @FXML private TableColumn<SceneBidder1.BidEntry, String> colBidTime;

    @FXML
    public void initialize() {
        colBidder.setCellValueFactory(cell -> new SimpleStringProperty(maskBidder(cell.getValue().bidder)));
        colBidPrice.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().price) + " d"));
        colBidTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().time.toString().replace('T', ' ')));
        bidHistoryTable.setPlaceholder(new Label("Chua co du lieu lich su bid."));
    }

    public void setBidHistory(List<SceneBidder1.BidEntry> entries) {
        bidHistoryTable.setItems(FXCollections.observableArrayList(entries));
    }

    private String maskBidder(String bidder) {
        if (bidder == null || bidder.isBlank() || "â€”".equals(bidder)) {
            return "â€”";
        }
        return bidder.length() <= 3 ? bidder.charAt(0) + "***" : bidder.substring(0, 3) + "***";
    }

    private String formatPrice(long price) {
        return String.format("%,d", price).replace(',', '.');
    }
}
