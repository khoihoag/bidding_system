package com.bidding.controller.admin;

import com.bidding.controller.admin.model.AuditRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Local audit log storage for admin actions.
 */
public class AdminAuditLog {

    private final AdminViewState state;

    public AdminAuditLog(AdminViewState state) {
        this.state = state;
    }

    public void append(String action, String actor, String target, String detail) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        state.allAuditLogs.add(0, new AuditRow(time, actor, action, target, detail));
    }

    public void updateLastDetail(String actionType, String newDetail) {
        for (int i = 0; i < Math.min(5, state.allAuditLogs.size()); i++) {
            AuditRow row = state.allAuditLogs.get(i);
            if (actionType.equals(row.getAction())) {
                state.allAuditLogs.set(i, new AuditRow(
                        row.getTime(), row.getActor(), row.getAction(), row.getTarget(), newDetail));
                break;
            }
        }
    }
}
