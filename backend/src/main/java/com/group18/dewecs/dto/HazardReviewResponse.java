package com.group18.dewecs.dto;

import java.util.List;

/** An event with the field evidence and the warnings that exist for it: the page an officer reads before warning. */
public class HazardReviewResponse {

    private final HazardEventResponse event;
    private final List<GroundReportResponse> verifiedReports;
    private final List<WarningResponse> warnings;

    public HazardReviewResponse(HazardEventResponse event, List<GroundReportResponse> verifiedReports,
                                 List<WarningResponse> warnings) {
        this.event = event;
        this.verifiedReports = verifiedReports;
        this.warnings = warnings;
    }

    public HazardEventResponse getEvent() {
        return event;
    }

    public List<GroundReportResponse> getVerifiedReports() {
        return verifiedReports;
    }

    public List<WarningResponse> getWarnings() {
        return warnings;
    }
}
