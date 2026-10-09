package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Warning;

import java.time.LocalDateTime;
import java.util.List;

/** Registering a hazard event and reviewing what is known about it before a warning is issued (UC-01). */
public interface HazardEventService {

    /** Starts an ACTIVE event. A null start time means now; a start time in the future is refused. */
    HazardEvent create(String hazardType, String severity, Long districtId, LocalDateTime occurredAt);

    /** Moves an event forward only: ACTIVE, then CONTAINED, then RESOLVED. */
    HazardEvent updateStatus(Long eventId, String status);

    HazardEvent getById(Long eventId);

    /** Newest first. */
    List<HazardEvent> list();

    /** Events that are still ACTIVE, each with its verified field reports and live warnings, for the dashboard. */
    List<HazardSummary> listActiveSummaries();

    /** The event with the verified or actioned ground reports of its district (from 24 h before it began) and its warnings. */
    HazardReview review(Long eventId);

    List<District> listDistricts();

    record HazardSummary(HazardEvent event, int verifiedReports, int liveWarnings) {
    }

    record HazardReview(HazardEvent event, List<GroundReport> verifiedReports, List<Warning> warnings) {
    }
}
