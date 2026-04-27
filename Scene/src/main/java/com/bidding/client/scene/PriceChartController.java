package com.bidding.client.scene;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

public class PriceChartController {

    @FXML private LineChart<String, Number> bidChart;

    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        priceSeries.setName("Gia dat");
        bidChart.setData(FXCollections.observableArrayList(priceSeries));
        bidChart.setAnimated(false);
        bidChart.setLegendVisible(false);
    }

    public void clear() {
        priceSeries.getData().clear();
    }

    public void addPoint(String time, long amount) {
        priceSeries.getData().add(new XYChart.Data<>(time, amount));
    }
}
