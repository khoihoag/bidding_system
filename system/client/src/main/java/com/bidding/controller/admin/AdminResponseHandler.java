package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AdminBidHistoryRow;
import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.PendingItemRow;
import com.bidding.controller.admin.model.UserRow;
import com.bidding.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

/**
 * Parses inbound admin socket messages and updates state/UI bindings.
 */
public class AdminResponseHandler {

    private final AdminViewState state;
    private final AdminAuditLog auditLog;
    private final AdminCharts charts;
    private final AdminFilters filters;
    private final AdminNetworkGateway network;
    private final Runnable requestAllUsers;
    private final Runnable requestAllAuctions;
    private final Runnable requestPendingItems;
    private final Runnable clearCreateAdminForm;
    private final BidHistoryDialogOpener bidHistoryDialogOpener;

    @FunctionalInterface
    public interface BidHistoryDialogOpener {
        void open(String auctionId, String auctionTitle, ObservableList<AdminBidHistoryRow> rows);
    }

    public AdminResponseHandler(AdminViewState state,
                                AdminAuditLog auditLog,
                                AdminCharts charts,
                                AdminFilters filters,
                                AdminNetworkGateway network,
                                Runnable requestAllUsers,
                                Runnable requestAllAuctions,
                                Runnable requestPendingItems,
                                Runnable clearCreateAdminForm,
                                BidHistoryDialogOpener bidHistoryDialogOpener) {
        this.state = state;
        this.auditLog = auditLog;
        this.charts = charts;
        this.filters = filters;
        this.network = network;
        this.requestAllUsers = requestAllUsers;
        this.requestAllAuctions = requestAllAuctions;
        this.requestPendingItems = requestPendingItems;
        this.clearCreateAdminForm = clearCreateAdminForm;
        this.bidHistoryDialogOpener = bidHistoryDialogOpener;
    }

    public void handle(JsonObject response) {
        String action = AdminUiHelper.getString(response, "action");
        switch (action) {
            case "LOGIN_REPLY" -> handleLoginReply(response);
            case "USERS_LIST" -> handleUsersList(response);
            case "BAN_USER_REPLY" -> handleBanUserReply(response);
            case "UNBAN_USER_REPLY" -> handleUnbanUserReply(response);
            case "FORCE_CLOSE_REPLY" -> handleForceCloseReply(response);
            case "AUCTIONS_LIST" -> handleAuctionsList(response);
            case "PENDING_ITEMS_LIST" -> handlePendingItemsList(response);
            case "APPROVE_ITEM_REPLY" -> handleApprovalMutationReply(response, "APPROVE_ITEM");
            case "REJECT_ITEM_REPLY" -> handleApprovalMutationReply(response, "REJECT_ITEM");
            case "CREATE_ADMIN_LEVEL1_REPLY" -> handleCreateAdminLevel1Reply(response);
            case "HISTORY_REPLY" -> handleHistoryReply(response);
            case "ADMIN_AUCTION_BID_HISTORY_REPLY" -> handleAdminAuctionBidHistoryReply(response);
            case "GLOBAL_NOTIFY" -> handleGlobalNotify(response);
            case "ERROR" -> handleError(response);
            default -> { }
        }
    }

    private void handleLoginReply(JsonObject res) {
        String status = AdminUiHelper.getString(res, "status");
        String role = AdminUiHelper.getString(res, "role");

        if ("SUCCESS".equals(status) && "ADMIN".equals(role)) {
            state.adminLoggedIn = true;
            auditLog.append("LOGIN", "admin", "Hệ thống", "Đăng nhập admin thành công");
            requestAllUsers.run();
        } else {
            state.adminLoggedIn = false;
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi xác thực",
                    "Đăng nhập admin thất bại.\nStatus: " + status + "  |  Role nhận: " + role
                            + "\nVui lòng kiểm tra credentials.");
        }
    }

    private void handleUsersList(JsonObject res) {
        state.allUsers.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) {
            return;
        }

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject o = el.getAsJsonObject();
            String role = getDisplayRole(
                    AdminUiHelper.getString(o, "role"),
                    o.has("adminLevel") && !o.get("adminLevel").isJsonNull() ? o.get("adminLevel").getAsInt() : 0);
            state.allUsers.add(new UserRow(
                    AdminUiHelper.getString(o, "id"),
                    AdminUiHelper.getString(o, "username"),
                    AdminUiHelper.getString(o, "email"),
                    role,
                    o.has("balance") && !o.get("balance").isJsonNull()
                            ? o.get("balance").getAsDouble() : 0.0,
                    AdminUiHelper.getActiveValue(o)
            ));
        }

        charts.updateUserStats();
        filters.applyUserFilter();
    }

    private void handleAuctionsList(JsonObject res) {
        state.allAuctions.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) {
            return;
        }

        int running = 0;
        int ended = 0;
        int other = 0;
        double totalRevenue = 0.0;

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject o = el.getAsJsonObject();
            String id = AdminUiHelper.getString(o, "id");
            String status = AdminUiHelper.getString(o, "status");
            double price = o.has("currentPrice") && !o.get("currentPrice").isJsonNull()
                    ? o.get("currentPrice").getAsDouble() : 0.0;

            String title = "(Không có tên)";
            JsonObject itemJson = null;
            if (o.has("item") && !o.get("item").isJsonNull() && o.get("item").isJsonObject()) {
                itemJson = o.getAsJsonObject("item");
                String name = AdminUiHelper.getString(itemJson, "name");
                if (!name.isEmpty()) {
                    title = name;
                }
            }

            state.allAuctions.add(new AuctionRow(id, title, status, price, itemJson));
            switch (status) {
                case "RUNNING" -> running++;
                case "FINISHED", "PAID", "FAILED" -> {
                    ended++;
                    totalRevenue += price;
                }
                default -> other++;
            }
        }

        charts.updateAuctionStats(running, ended, totalRevenue, other);
        filters.applyAucFilter();
    }

    private void handlePendingItemsList(JsonObject res) {
        state.pendingItems.clear();
        if (!res.has("data") || res.get("data").isJsonNull()) {
            return;
        }

        for (JsonElement el : res.getAsJsonArray("data")) {
            JsonObject item = el.getAsJsonObject();
            double price = item.has("startingPrice") && !item.get("startingPrice").isJsonNull()
                    ? item.get("startingPrice").getAsDouble() : 0.0;
            state.pendingItems.add(new PendingItemRow(
                    AdminUiHelper.getString(item, "id"),
                    AdminUiHelper.getString(item, "name"),
                    AdminUiHelper.getString(item, "sellerFullName"),
                    AdminUiHelper.getString(item, "type"),
                    price,
                    item.deepCopy()
            ));
        }
    }

    private void handleHistoryReply(JsonObject res) {
        if (!res.has("data") || res.get("data").isJsonNull()) {
            return;
        }
        charts.updateBidHistoryChart(res.getAsJsonArray("data"));
    }

    private void handleAdminAuctionBidHistoryReply(JsonObject res) {
        String auctionId = AdminUiHelper.getString(res, "auctionId");
        if (!state.pendingHistoryAuctionId.isEmpty()
                && !state.pendingHistoryAuctionId.equals(auctionId)) {
            return;
        }

        ObservableList<AdminBidHistoryRow> rows = FXCollections.observableArrayList();
        JsonArray data = res.has("data") && res.get("data").isJsonArray()
                ? res.getAsJsonArray("data") : new JsonArray();

        int index = 1;
        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            double amount = o.has("amount") && !o.get("amount").isJsonNull()
                    ? o.get("amount").getAsDouble() : 0.0;
            String username = AdminUiHelper.getString(o, "bidderUsername");
            String fullName = AdminUiHelper.getString(o, "bidderFullName");
            String bidder = fullName.isEmpty() ? username : fullName + " (" + username + ")";
            if (bidder.isBlank()) {
                bidder = "N/A";
            }

            rows.add(new AdminBidHistoryRow(
                    index++,
                    bidder,
                    AdminUiHelper.getString(o, "bidderEmail"),
                    String.format("VNĐ %,.0f", amount),
                    AdminUiHelper.formatTimestamp(AdminUiHelper.getString(o, "timestamp")),
                    JsonUtil.formatBidType(o, "isAuto"),
                    AdminUiHelper.getString(o, "status")
            ));
        }

        bidHistoryDialogOpener.open(auctionId, state.pendingHistoryAuctionTitle, rows);
    }

    private void handleBanUserReply(JsonObject res) {
        String status = AdminUiHelper.getString(res, "status");
        String message = AdminUiHelper.getString(res, "message");

        auditLog.updateLastDetail("BAN_USER",
                "SUCCESS".equals(status) ? "OK " + message : "Lỗi: " + message);

        if ("SUCCESS".equals(status)) {
            AdminUiHelper.showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    message.isEmpty() ? "Đã khóa tài khoản thành công!" : message);
            requestAllUsers.run();
        } else {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi khóa tài khoản",
                    message.isEmpty() ? "Không thể khóa tài khoản." : message);
        }
    }

    private void handleUnbanUserReply(JsonObject res) {
        String status = AdminUiHelper.getString(res, "status");
        String message = AdminUiHelper.getString(res, "message");

        auditLog.updateLastDetail("UNBAN_USER",
                "SUCCESS".equals(status) ? "OK " + message : "Lỗi: " + message);

        if ("SUCCESS".equals(status)) {
            AdminUiHelper.showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    message.isEmpty() ? "Đã mở khóa tài khoản thành công!" : message);
            requestAllUsers.run();
        } else {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi mở khóa tài khoản",
                    message.isEmpty() ? "Không thể mở khóa tài khoản." : message);
        }
    }

    private void handleForceCloseReply(JsonObject res) {
        String status = AdminUiHelper.getString(res, "status");

        auditLog.updateLastDetail("FORCE_CLOSE",
                "SUCCESS".equals(status) ? "OK Đóng phiên thành công" : "Lỗi");

        if ("SUCCESS".equals(status)) {
            AdminUiHelper.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ép đóng phiên đấu giá thành công!");
            requestAllAuctions.run();
        } else {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đóng phiên đấu giá.");
        }
    }

    private void handleApprovalMutationReply(JsonObject res, String actionName) {
        String status = AdminUiHelper.getString(res, "status");
        String message = AdminUiHelper.getString(res, "message");
        auditLog.updateLastDetail(actionName,
                "SUCCESS".equals(status) ? "OK " + message : "Loi: " + message);

        if ("SUCCESS".equals(status)) {
            AdminUiHelper.showAlert(Alert.AlertType.INFORMATION, "Thanh cong",
                    message.isEmpty() ? "Da cap nhat trang thai san pham." : message);
            requestPendingItems.run();
        } else {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Loi",
                    message.isEmpty() ? "Khong the cap nhat trang thai san pham." : message);
        }
    }

    private void handleCreateAdminLevel1Reply(JsonObject res) {
        String status = AdminUiHelper.getString(res, "status");
        String message = AdminUiHelper.getString(res, "message");
        auditLog.updateLastDetail("CREATE_ADMIN_LEVEL1",
                "SUCCESS".equals(status) ? "OK " + message : "Lỗi: " + message);

        if ("SUCCESS".equals(status)) {
            AdminUiHelper.showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    message.isEmpty() ? "Đã tạo admin level 1." : message);
            clearCreateAdminForm.run();
            requestAllUsers.run();
        } else {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi",
                    message.isEmpty() ? "Không thể tạo admin level 1." : message);
        }
    }

    private String getDisplayRole(String role, int adminLevel) {
        if ("ADMIN".equals(role)) {
            return adminLevel >= 2 ? "ADMIN L2" : "ADMIN L1";
        }
        return role;
    }

    private void handleGlobalNotify(JsonObject res) {
        String message = AdminUiHelper.getString(res, "message");
        auditLog.append("GLOBAL_NOTIFY", "system", "Broadcast", message);
        if (state.adminLoggedIn) {
            requestAllAuctions.run();
        }
    }

    private void handleError(JsonObject res) {
        String msg = AdminUiHelper.getString(res, "message");
        if (!msg.isEmpty()) {
            AdminUiHelper.showAlert(Alert.AlertType.ERROR, "Lỗi từ server", msg);
        }
    }
}
