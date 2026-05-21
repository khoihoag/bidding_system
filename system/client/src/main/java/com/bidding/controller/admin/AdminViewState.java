package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.AuditRow;
import com.bidding.controller.admin.model.UserRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * In-memory state for the admin panel (lists, pagination, session flags).
 */
public class AdminViewState {

    public static final int PAGE_SIZE = 20;

    public final ObservableList<UserRow> allUsers = FXCollections.observableArrayList();
    public final ObservableList<AuctionRow> allAuctions = FXCollections.observableArrayList();
    public final ObservableList<AuditRow> allAuditLogs = FXCollections.observableArrayList();
    public final ObservableList<UserRow> filteredUsers = FXCollections.observableArrayList();
    public final ObservableList<AuctionRow> filteredAuctions = FXCollections.observableArrayList();

    public int userCurrentPage = 1;
    public int aucCurrentPage = 1;
    public boolean adminLoggedIn;
    public boolean connectionStarted;
    public String pendingHistoryAuctionId = "";
    public String pendingHistoryAuctionTitle = "";
}
