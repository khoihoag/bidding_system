package com.bidding.controller.admin.model;

/**
 * Table row model for the admin audit log panel.
 */
public class AuditRow {
    private final String time;
    private final String actor;
    private final String action;
    private final String target;
    private final String detail;

    public AuditRow(String time, String actor, String action, String target, String detail) {
        this.time = time;
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.detail = detail;
    }

    public String getTime() {
        return time;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getDetail() {
        return detail;
    }
}
