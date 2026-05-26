package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.AuditRow;
import com.bidding.controller.admin.model.UserRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

/**
 * Search, filter, and pagination logic for admin tables.
 */
public class AdminFilters {

    private final AdminViewState state;
    private final TextField userSearchField;
    private final ComboBox<String> userRoleFilter;
    private final Label userPageInfo;
    private final Label userCountLabel;
    private final TextField aucSearchField;
    private final ComboBox<String> aucStatusFilter;
    private final Label aucPageInfo;
    private final Label aucCountLabel;
    private final DatePicker auditDateFilter;
    private final TableView<AuditRow> auditTable;

    public AdminFilters(AdminViewState state,
                        TextField userSearchField,
                        ComboBox<String> userRoleFilter,
                        Label userPageInfo,
                        Label userCountLabel,
                        TextField aucSearchField,
                        ComboBox<String> aucStatusFilter,
                        Label aucPageInfo,
                        Label aucCountLabel,
                        DatePicker auditDateFilter,
                        TableView<AuditRow> auditTable) {
        this.state = state;
        this.userSearchField = userSearchField;
        this.userRoleFilter = userRoleFilter;
        this.userPageInfo = userPageInfo;
        this.userCountLabel = userCountLabel;
        this.aucSearchField = aucSearchField;
        this.aucStatusFilter = aucStatusFilter;
        this.aucPageInfo = aucPageInfo;
        this.aucCountLabel = aucCountLabel;
        this.auditDateFilter = auditDateFilter;
        this.auditTable = auditTable;
    }

    public void applyUserFilter() {
        String keyword = userSearchField != null ? userSearchField.getText().trim().toLowerCase() : "";
        String role = userRoleFilter != null ? userRoleFilter.getValue() : "Tất cả";
        state.filteredUsers.clear();
        for (UserRow user : state.allUsers) {
            boolean matches = (keyword.isEmpty()
                    || user.getUsername().toLowerCase().contains(keyword)
                    || user.getEmail().toLowerCase().contains(keyword)
                    || user.getId().toLowerCase().contains(keyword))
                    && (role == null || role.equals("Tất cả") || user.getRole().equals(role));
            if (matches) {
                state.filteredUsers.add(user);
            }
        }
        state.userCurrentPage = 1;
        updateUserPageInfo();
    }

    public void clearUserSearch() {
        userSearchField.clear();
        userRoleFilter.getSelectionModel().selectFirst();
        applyUserFilter();
    }

    public void applyAucFilter() {
        String keyword = aucSearchField != null ? aucSearchField.getText().trim().toLowerCase() : "";
        String status = aucStatusFilter != null ? aucStatusFilter.getValue() : "Tất cả";
        state.filteredAuctions.clear();
        for (AuctionRow auction : state.allAuctions) {
            boolean matches = (keyword.isEmpty()
                    || auction.getTitle().toLowerCase().contains(keyword)
                    || auction.getId().toLowerCase().contains(keyword))
                    && (status == null || status.equals("Tất cả") || auction.getStatus().equals(status));
            if (matches) {
                state.filteredAuctions.add(auction);
            }
        }
        state.aucCurrentPage = 1;
        updateAucPageInfo();
    }

    public void clearAucSearch() {
        aucSearchField.clear();
        aucStatusFilter.getSelectionModel().selectFirst();
        applyAucFilter();
    }

    public void filterAuditLog() {
        if (auditDateFilter.getValue() == null) {
            return;
        }
        String date = auditDateFilter.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        ObservableList<AuditRow> filtered = FXCollections.observableArrayList();
        for (AuditRow row : state.allAuditLogs) {
            if (row.getTime().startsWith(date)) {
                filtered.add(row);
            }
        }
        auditTable.setItems(filtered);
    }

    public void resetAuditFilter() {
        auditDateFilter.setValue(null);
        auditTable.setItems(state.allAuditLogs);
    }

    public void userPrevPage() {
        if (state.userCurrentPage > 1) {
            state.userCurrentPage--;
            updateUserPageInfo();
        }
    }

    public void userNextPage() {
        int totalPages = (int) Math.ceil((double) state.filteredUsers.size() / AdminViewState.PAGE_SIZE);
        if (state.userCurrentPage < totalPages) {
            state.userCurrentPage++;
            updateUserPageInfo();
        }
    }

    public void aucPrevPage() {
        if (state.aucCurrentPage > 1) {
            state.aucCurrentPage--;
            updateAucPageInfo();
        }
    }

    public void aucNextPage() {
        int totalPages = (int) Math.ceil((double) state.filteredAuctions.size() / AdminViewState.PAGE_SIZE);
        if (state.aucCurrentPage < totalPages) {
            state.aucCurrentPage++;
            updateAucPageInfo();
        }
    }

    private void updateUserPageInfo() {
        int total = state.filteredUsers.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / AdminViewState.PAGE_SIZE));
        userPageInfo.setText("Trang " + state.userCurrentPage + " / " + totalPages);
        userCountLabel.setText(total + " người dùng");
    }

    private void updateAucPageInfo() {
        int total = state.filteredAuctions.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / AdminViewState.PAGE_SIZE));
        aucPageInfo.setText("Trang " + state.aucCurrentPage + " / " + totalPages);
        aucCountLabel.setText(total + " phiên");
    }
}
