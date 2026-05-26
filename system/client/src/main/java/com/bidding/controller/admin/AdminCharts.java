package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.UserRow;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates dashboard and stats charts from loaded admin data.
 */
public class AdminCharts {

    private final AdminViewState state;
    private final Label statTotalUsers;
    private final Label statUsersGrowth;
    private final Label statActiveSessions;
    private final Label statSessionsInfo;
    private final Label statTotalBids;
    private final Label statBidsToday;
    private final Label statTotalRevenue;
    private final Label statRevenueGrowth;
    private final BarChart<String, Number> bidBarChart;
    private final PieChart categoryPieChart;
    private final PieChart rolesPieChart;
    private final BarChart<String, Number> revenueChart;
    private final LineChart<String, Number> usersGrowthChart;

    public AdminCharts(AdminViewState state,
                       Label statTotalUsers,
                       Label statUsersGrowth,
                       Label statActiveSessions,
                       Label statSessionsInfo,
                       Label statTotalBids,
                       Label statBidsToday,
                       Label statTotalRevenue,
                       Label statRevenueGrowth,
                       BarChart<String, Number> bidBarChart,
                       PieChart categoryPieChart,
                       PieChart rolesPieChart,
                       BarChart<String, Number> revenueChart,
                       LineChart<String, Number> usersGrowthChart) {
        this.state = state;
        this.statTotalUsers = statTotalUsers;
        this.statUsersGrowth = statUsersGrowth;
        this.statActiveSessions = statActiveSessions;
        this.statSessionsInfo = statSessionsInfo;
        this.statTotalBids = statTotalBids;
        this.statBidsToday = statBidsToday;
        this.statTotalRevenue = statTotalRevenue;
        this.statRevenueGrowth = statRevenueGrowth;
        this.bidBarChart = bidBarChart;
        this.categoryPieChart = categoryPieChart;
        this.rolesPieChart = rolesPieChart;
        this.revenueChart = revenueChart;
        this.usersGrowthChart = usersGrowthChart;
    }

    public void updateUserStats() {
        statTotalUsers.setText(String.valueOf(state.allUsers.size()));
        statUsersGrowth.setText("Tổng tài khoản: " + state.allUsers.size());
        updateRolesPieChart();
    }

    public void updateAuctionStats(int running, int ended, double totalRevenue, int other) {
        statActiveSessions.setText(String.valueOf(running));
        statSessionsInfo.setText("Phiên đang chạy");
        statTotalRevenue.setText(String.format("VNĐ %,.0f", totalRevenue));
        statRevenueGrowth.setText("Từ " + ended + " phiên đã kết thúc");

        categoryPieChart.getData().clear();
        if (running > 0) {
            categoryPieChart.getData().add(new PieChart.Data("RUNNING (" + running + ")", running));
        }
        if (ended > 0) {
            categoryPieChart.getData().add(new PieChart.Data("FINISHED (" + ended + ")", ended));
        }
        if (other > 0) {
            categoryPieChart.getData().add(new PieChart.Data("Khác (" + other + ")", other));
        }
        updateRevenueChart();
    }

    public void updateBidHistoryChart(Iterable<JsonElement> historyElements) {
        int totalBids = 0;
        LinkedHashMap<String, Integer> bidCount = new LinkedHashMap<>();
        for (JsonElement el : historyElements) {
            totalBids++;
            String aucId = AdminUiHelper.getString(el.getAsJsonObject(), "auctionId");
            if (!aucId.isEmpty()) {
                bidCount.merge(aucId, 1, Integer::sum);
            }
        }

        statTotalBids.setText(String.valueOf(totalBids));
        statBidsToday.setText("Tổng lượt đặt giá");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt bid");
        int count = 0;
        for (Map.Entry<String, Integer> entry : bidCount.entrySet()) {
            if (count++ >= 10) {
                break;
            }
            String lbl = entry.getKey().length() > 10
                    ? entry.getKey().substring(0, 10) + "..." : entry.getKey();
            series.getData().add(new XYChart.Data<>(lbl, entry.getValue()));
        }
        bidBarChart.getData().clear();
        if (!series.getData().isEmpty()) {
            bidBarChart.getData().add(series);
        }
    }

    private void updateRolesPieChart() {
        int users = 0;
        int sellers = 0;
        int admins = 0;
        int other = 0;
        for (UserRow row : state.allUsers) {
            switch (row.getRole()) {
                case "USER" -> users++;
                case "SELLER" -> sellers++;
                case "ADMIN" -> admins++;
                default -> other++;
            }
        }
        rolesPieChart.getData().clear();
        if (users > 0) rolesPieChart.getData().add(new PieChart.Data("USER (" + users + ")", users));
        if (sellers > 0) rolesPieChart.getData().add(new PieChart.Data("SELLER (" + sellers + ")", sellers));
        if (admins > 0) rolesPieChart.getData().add(new PieChart.Data("ADMIN (" + admins + ")", admins));
        if (other > 0) rolesPieChart.getData().add(new PieChart.Data("Khác (" + other + ")", other));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng");
        if (users > 0) series.getData().add(new XYChart.Data<>("USER", users));
        if (sellers > 0) series.getData().add(new XYChart.Data<>("SELLER", sellers));
        if (admins > 0) series.getData().add(new XYChart.Data<>("ADMIN", admins));
        usersGrowthChart.getData().clear();
        if (!series.getData().isEmpty()) {
            usersGrowthChart.getData().add(series);
        }
    }

    private void updateRevenueChart() {
        LinkedHashMap<String, Double> revenueByStatus = new LinkedHashMap<>();
        for (AuctionRow row : state.allAuctions) {
            revenueByStatus.merge(row.getStatus(), row.getCurrentPrice(), Double::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tổng giá trị (VNĐ)");
        for (Map.Entry<String, Double> entry : revenueByStatus.entrySet()) {
            if (entry.getValue() > 0) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }
        revenueChart.getData().clear();
        if (!series.getData().isEmpty()) {
            revenueChart.getData().add(series);
        }
    }
}
