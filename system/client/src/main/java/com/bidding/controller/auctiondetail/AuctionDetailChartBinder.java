package com.bidding.controller.auctiondetail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;

/**
 * Price line chart setup and updates for auction detail.
 */
public class AuctionDetailChartBinder {

    private final AuctionDetailState state;
    private final AreaChart<String, Number> priceChart;
    private final AuctionDetailUiPresenter ui;

    public AuctionDetailChartBinder(AuctionDetailState state,
                                    AreaChart<String, Number> priceChart,
                                    AuctionDetailUiPresenter ui) {
        this.state = state;
        this.priceChart = priceChart;
        this.ui = ui;
    }

    public void setupChart() {
        state.priceSeries = new XYChart.Series<>();
        state.priceSeries.setName("Giá đấu giá");
        priceChart.getData().add(state.priceSeries);
        priceChart.setTitle(null);
        priceChart.setAnimated(false);
        priceChart.getStyleClass().add("custom-chart");
    }

    public void loadHistory(JsonArray data) {
        state.priceSeries.getData().clear();
        state.totalBidCount = 0;

        for (JsonElement element : data) {
            JsonObject point = element.getAsJsonObject();
            String rawTime = AuctionDetailFormats.getStringSafe(point, "timestamp");
            double price = point.has("price") ? point.get("price").getAsDouble() : 0.0;
            String displayTime = AuctionDetailFormats.formatTimestampForDisplay(rawTime);
            state.priceSeries.getData().add(new XYChart.Data<>(displayTime, price));
            state.totalBidCount++;
        }

        ui.updateTotalBids(state.totalBidCount);

        if (!state.priceSeries.getData().isEmpty()) {
            XYChart.Data<String, Number> lastPoint =
                    state.priceSeries.getData().get(state.priceSeries.getData().size() - 1);
            ui.updateCurrentPrice(lastPoint.getYValue().doubleValue());
        }
    }

    public void addRealtimePoint(String displayTime, double newPrice) {
        state.priceSeries.getData().add(new XYChart.Data<>(displayTime, newPrice));
        state.totalBidCount++;
        ui.updateTotalBids(state.totalBidCount);
        ui.updateLastUpdate(displayTime);
        ui.clearResult();
    }
}
