package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AuctionRow;
import com.bidding.controller.admin.model.UserRow;
import com.bidding.model.UserSession;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

/**
 * Custom table cell factories for admin action columns.
 */
public final class AdminTableCellFactory {

    public interface UserActionHandler {
        void onBan(UserRow user);

        void onUnban(UserRow user);
    }

    public interface AuctionActionHandler {
        void onViewItem(AuctionRow auction);

        void onViewHistory(AuctionRow auction);

        void onForceClose(AuctionRow auction);
    }

    private AdminTableCellFactory() {
    }

    public static Callback<TableColumn<UserRow, Void>, TableCell<UserRow, Void>> banButtonColumn(UserActionHandler handler) {
        return param -> new TableCell<>() {
            private final Button banBtn = new Button("Khóa");

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                UserRow row = getTableView().getItems().get(getIndex());
                banBtn.getStyleClass().removeAll("btn-danger", "btn-outline");
                if (row.isActive()) {
                    banBtn.setText("Ban");
                    banBtn.getStyleClass().add("btn-danger");
                    banBtn.setOnAction(e -> handler.onBan(row));
                } else {
                    banBtn.setText("Unban");
                    banBtn.getStyleClass().add("btn-outline");
                    banBtn.setOnAction(e -> handler.onUnban(row));
                }
                banBtn.setDisable(row.getId().equals(UserSession.getInstance().getUserId()));
                setGraphic(banBtn);
            }
        };
    }

    public static Callback<TableColumn<AuctionRow, Void>, TableCell<AuctionRow, Void>> auctionActionColumn(
            AuctionActionHandler handler) {
        return param -> new TableCell<>() {
            private final Button closeBtn = new Button("Đóng phiên");
            private final Button viewBtn = new Button("Xem SP");
            private final Button historyBtn = new Button("Lịch sử");
            private final HBox actions = new HBox(8, viewBtn, historyBtn, closeBtn);

            {
                viewBtn.getStyleClass().add("btn-outline");
                historyBtn.getStyleClass().add("btn-outline");
                closeBtn.getStyleClass().add("btn-warn");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AuctionRow row = getTableView().getItems().get(getIndex());
                viewBtn.setOnAction(e -> handler.onViewItem(row));
                historyBtn.setOnAction(e -> handler.onViewHistory(row));
                if ("RUNNING".equals(row.getStatus())) {
                    closeBtn.setOnAction(e -> handler.onForceClose(row));
                    closeBtn.setVisible(true);
                    closeBtn.setManaged(true);
                } else {
                    closeBtn.setVisible(false);
                    closeBtn.setManaged(false);
                }
                setGraphic(actions);
            }
        };
    }
}
