package com.group18.dewecs.dto;

/** One card of the Active Warnings list beside the issue form: "Flood - Colombo", its severity and a one-click escalation. */
public class ActiveWarningCard {

    private final Long id;
    private final String title;
    private final String severity;
    private final String status;
    private final String nextSeverity;

    public ActiveWarningCard(Long id, String title, String severity, String status, String nextSeverity) {
        this.id = id;
        this.title = title;
        this.severity = severity;
        this.status = status;
        this.nextSeverity = nextSeverity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    /** The next higher severity, or null when the warning is already CRITICAL (nothing to escalate to). */
    public String getNextSeverity() {
        return nextSeverity;
    }
}
