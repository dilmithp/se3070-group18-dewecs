package com.group18.dewecs.service;

import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReportMetric;

import java.util.List;

public interface PostEventReportService {

    /**
     * Builds a snapshot report for a hazard event: links the event's published warnings and the district's shelters,
     * and stores the four metrics. Every call creates a new report, so earlier snapshots are kept.
     */
    PostEventReport generate(Long hazardEventId);

    PostEventReport getById(Long reportId);

    /** Newest first. */
    List<PostEventReport> list();

    List<ReportMetric> listMetrics(Long reportId);

    List<HazardEvent> listHazardEvents();
}
