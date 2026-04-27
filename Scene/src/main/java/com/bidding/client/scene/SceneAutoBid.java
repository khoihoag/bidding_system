package com.bidding.client.scene;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SceneAutoBid implements Initializable {

    @FXML private TextField txtProductId;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Label lblMessage;
    @FXML private TableView<AutoBidItem> tableAutoBid;
    @FXML private TableColumn<AutoBidItem, String> colProductId;
    @FXML private TableColumn<AutoBidItem, String> colProductName;
    @FXML private TableColumn<AutoBidItem, String> colCurrentPrice;
    @FXML private TableColumn<AutoBidItem, String> colMaxBid;
    @FXML private TableColumn<AutoBidItem, String> colIncrement;
    @FXML private TableColumn<AutoBidItem, String> colStatus;
    @FXML private Button btnBack;

    private ObservableList<AutoBidItem> autoBidList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colMaxBid.setCellValueFactory(new PropertyValueFactory<>("maxBid"));
        colIncrement.setCellValueFactory(new PropertyValueFactory<>("increment"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        autoBidList = FXCollections.observableArrayList(
                new AutoBidItem("SP001", "Laptop Dell XPS 15", "15.000.000", "20.000.000", "500.000", "Dang chay"),
                new AutoBidItem("SP005", "iPhone 14 Pro Max", "22.500.000", "22.000.000", "200.000", "Da vuot MaxBid"),
                new AutoBidItem("SP012", "Dong ho Rolex", "45.000.000", "50.000.000", "1.000.000", "Tam dung")
        );

        tableAutoBid.setItems(autoBidList);
    }

    @FXML
    void handleStartBot(ActionEvent event) {
        lblMessage.setText("");
        String pId = txtProductId.getText();
        String maxBid = txtMaxBid.getText();
        String inc = txtIncrement.getText();

        if (pId.isEmpty() || maxBid.isEmpty() || inc.isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: #b93832; -fx-font-weight: bold;");
            lblMessage.setText("Vui long nhap day du thong tin.");
            return;
        }

        AutoBidItem newItem = new AutoBidItem(pId, "San pham moi test", "0", maxBid, inc, "Dang chay");
        autoBidList.add(0, newItem);

        lblMessage.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
        lblMessage.setText("Da khoi dong bot Auto Bid.");

        txtProductId.clear();
        txtMaxBid.clear();
        txtIncrement.clear();
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            AppNavigator.openPrimary(stage, "/com/bidding/client/scene/SceneBidder1.fxml", "BidViet - Nen tang dau gia truc tuyen");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: #b93832; -fx-font-weight: bold;");
            lblMessage.setText("Khong the quay lai SceneBidder1.fxml");
        }
    }

    public static class AutoBidItem {
        private final String productId;
        private final String productName;
        private final String currentPrice;
        private final String maxBid;
        private final String increment;
        private final String status;

        public AutoBidItem(String productId, String productName, String currentPrice, String maxBid, String increment, String status) {
            this.productId = productId;
            this.productName = productName;
            this.currentPrice = currentPrice;
            this.maxBid = maxBid;
            this.increment = increment;
            this.status = status;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCurrentPrice() { return currentPrice; }
        public String getMaxBid() { return maxBid; }
        public String getIncrement() { return increment; }
        public String getStatus() { return status; }
    }
}
