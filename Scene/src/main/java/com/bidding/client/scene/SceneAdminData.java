package com.bidding.client.scene;

import java.time.LocalDateTime;
import java.util.List;

public final class SceneAdminData {

    private SceneAdminData() {
    }

    public record UserLoginRecord(String time, String ip, String device) {
    }

    public record UserActivityRecord(String time, String type, String item) {
    }

    public record UserRow(
            String id,
            String fullName,
            String email,
            String role,
            String status,
            boolean banned,
            LocalDateTime createdAt,
            List<UserLoginRecord> loginHistory,
            List<UserActivityRecord> activityHistory
    ) {
        public UserRow withStatus(String newStatus, boolean newBanned) {
            return new UserRow(id, fullName, email, role, newStatus, newBanned, createdAt, loginHistory, activityHistory);
        }
    }

    public record AuctionRow(
            String id,
            String title,
            String seller,
            String status,
            long currentPrice,
            int bidCount,
            LocalDateTime endTime,
            boolean closedEmergency
    ) {
        public AuctionRow withForceClosed() {
            return new AuctionRow(id, title, seller, "Da dong khan cap", currentPrice, bidCount, endTime, true);
        }
    }

    public record AuditRow(LocalDateTime time, String actor, String action, String target, String detail) {
    }
}
