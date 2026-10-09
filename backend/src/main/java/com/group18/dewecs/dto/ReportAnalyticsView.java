package com.group18.dewecs.dto;

import java.time.LocalDateTime;
import java.util.List;

/** The analytics half of a post-event report page: window, status, indicators and the two charts. */
public class ReportAnalyticsView {

    public final boolean available;
    public final LocalDateTime windowFrom;
    public final LocalDateTime windowTo;
    public final String donorName;
    public final boolean provisional;
    public final String provisionalReason;
    public final String dataGaps;
    public final String approvedBy;
    public final LocalDateTime approvedAt;
    public final boolean canApprove;
    public final List<Kpi> kpis;
    public final List<Bar> agencyShares;
    public final List<NeedBar> needVsDistributed;

    public ReportAnalyticsView(boolean available, LocalDateTime windowFrom, LocalDateTime windowTo, String donorName,
                               boolean provisional, String provisionalReason, String dataGaps, String approvedBy,
                               LocalDateTime approvedAt, boolean canApprove, List<Kpi> kpis, List<Bar> agencyShares,
                               List<NeedBar> needVsDistributed) {
        this.available = available;
        this.windowFrom = windowFrom;
        this.windowTo = windowTo;
        this.donorName = donorName;
        this.provisional = provisional;
        this.provisionalReason = provisionalReason;
        this.dataGaps = dataGaps;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.canApprove = canApprove;
        this.kpis = kpis;
        this.agencyShares = agencyShares;
        this.needVsDistributed = needVsDistributed;
    }

    /** One indicator tile. */
    public static class Kpi {
        public final String label;
        public final String value;
        public final String detail;

        public Kpi(String label, String value, String detail) {
            this.label = label;
            this.value = value;
            this.detail = detail;
        }
    }

    /** One horizontal bar: its length is {@code percent} of the full width. */
    public static class Bar {
        public final String label;
        public final String valueText;
        public final int percent;

        public Bar(String label, String valueText, int percent) {
            this.label = label;
            this.valueText = valueText;
            this.percent = percent;
        }
    }

    /** Needed versus dispatched units of one type of supply. */
    public static class NeedBar {
        public final String label;
        public final double needed;
        public final double distributed;
        public final int neededPercent;
        public final int distributedPercent;

        public NeedBar(String label, double needed, double distributed, int neededPercent, int distributedPercent) {
            this.label = label;
            this.needed = needed;
            this.distributed = distributed;
            this.neededPercent = neededPercent;
            this.distributedPercent = distributedPercent;
        }
    }
}
