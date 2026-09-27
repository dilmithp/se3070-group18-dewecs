package com.group18.dewecs.dto;

public class DashboardSummary {

    private final long openWarnings;
    private final long fullShelters;
    private final long pendingRescueRequests;
    private final long lowStockSupplies;
    private final long unreviewedReports;

    public DashboardSummary(long openWarnings, long fullShelters, long pendingRescueRequests,
                             long lowStockSupplies, long unreviewedReports) {
        this.openWarnings = openWarnings;
        this.fullShelters = fullShelters;
        this.pendingRescueRequests = pendingRescueRequests;
        this.lowStockSupplies = lowStockSupplies;
        this.unreviewedReports = unreviewedReports;
    }

    public long getOpenWarnings() {
        return openWarnings;
    }

    public long getFullShelters() {
        return fullShelters;
    }

    public long getPendingRescueRequests() {
        return pendingRescueRequests;
    }

    public long getLowStockSupplies() {
        return lowStockSupplies;
    }

    public long getUnreviewedReports() {
        return unreviewedReports;
    }
}
