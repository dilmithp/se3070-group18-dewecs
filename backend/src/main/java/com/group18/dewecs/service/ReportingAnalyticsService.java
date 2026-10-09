package com.group18.dewecs.service;

import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReportDetails;
import com.group18.dewecs.domain.ReportKpi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-02 reporting on top of the plain post-event report: analysis window, optional donor filter, performance
 * indicators, the incomplete-data check, the provisional draft and the approval.
 */
public interface ReportingAnalyticsService {

    /** A null window end means now, a null start means the start of the incident, a null donor means all agencies. */
    record Parameters(Long hazardEventId, LocalDateTime from, LocalDateTime to, Long donorOrganizationId) {
    }

    /** What to do when baseline data is missing: a flagged draft, or certified estimates with a justification. */
    enum GapHandling {
        PROVISIONAL,
        OVERRIDE
    }

    /** The missing baseline data, in plain words. Empty when the report can be generated as it is. */
    List<String> findDataGaps(Parameters parameters);

    /**
     * Generates the report snapshot and its indicators. Throws {@code IncompleteDataException} when there are data gaps
     * and {@code handling} is null; an OVERRIDE needs a justification.
     */
    PostEventReport generate(Parameters parameters, GapHandling handling, String justification);

    /** Approves the district summary. A provisional draft cannot be approved. */
    ReportDetails approve(Long reportId, String officer);

    Optional<ReportDetails> details(Long reportId);

    List<ReportKpi> kpis(Long reportId);

    /** The organisations that can be chosen as donor filter. */
    List<Organization> listOrganizations();
}
