package com.group18.dewecs.service;

/** Downloadable copies of a post-event report: CSV and PDF. Both carry the provisional disclaimer on a draft. */
public interface ReportExportService {

    Export csv(Long reportId);

    Export pdf(Long reportId);

    record Export(String filename, String contentType, byte[] content) {
    }
}
