package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReportDetails;
import com.group18.dewecs.domain.ReportKpi;
import com.group18.dewecs.dto.PostEventReportResponse;
import com.group18.dewecs.mapper.PostEventReportMapper;
import com.group18.dewecs.service.PostEventReportService;
import com.group18.dewecs.service.ReportExportService;
import com.group18.dewecs.service.ReportingAnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    static final String DISCLAIMER = "PROVISIONAL AUDIT DISCLAIMER: generated with flagged data gaps; not an official audit document.";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final PostEventReportService reportService;
    private final ReportingAnalyticsService analyticsService;
    private final PostEventReportMapper mapper;

    public ReportExportServiceImpl(PostEventReportService reportService, ReportingAnalyticsService analyticsService,
                                   PostEventReportMapper mapper) {
        this.reportService = reportService;
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    /** Everything an export needs, read once. */
    private record Data(PostEventReport report, PostEventReportResponse view, Optional<ReportDetails> details,
                        List<ReportKpi> kpis) {
        boolean provisional() {
            return details.map(d -> Boolean.TRUE.equals(d.getProvisional())).orElse(false);
        }
    }

    private Data load(Long reportId) {
        PostEventReport report = reportService.getById(reportId);
        PostEventReportResponse view = mapper.toResponse(report, reportService.listMetrics(reportId));
        return new Data(report, view, analyticsService.details(reportId), analyticsService.kpis(reportId));
    }

    private String title(Data d) {
        return "Post-event analysis report #" + d.report().getId() + " - " + label(d.view().getHazardType()) + " in "
                + d.view().getDistrictName();
    }

    private List<String[]> summary(Data d) {
        List<String[]> rows = new ArrayList<>();
        PostEventReportResponse v = d.view();
        rows.add(new String[] {"Hazard", label(v.getHazardType())});
        rows.add(new String[] {"Region", v.getDistrictName()});
        rows.add(new String[] {"Severity", label(v.getSeverity())});
        rows.add(new String[] {"Event status", label(v.getEventStatus())});
        rows.add(new String[] {"Event started", time(v.getOccurredAt())});
        rows.add(new String[] {"Report generated", time(v.getGeneratedAt())});
        d.details().ifPresent(det -> {
            rows.add(new String[] {"Analysis window", time(det.getWindowFrom()) + " to " + time(det.getWindowTo())});
            rows.add(new String[] {"Donor filter", det.getDonor() == null ? "All agencies" : det.getDonor().getName()});
            rows.add(new String[] {"Approval", det.getApprovedAt() == null ? "Not approved"
                    : "Approved by " + det.getApprovedBy() + " on " + time(det.getApprovedAt())});
            if (det.getProvisionalReason() != null) {
                rows.add(new String[] {"Data note", det.getProvisionalReason()});
            }
            if (det.getDataGaps() != null) {
                rows.add(new String[] {"Data gaps", det.getDataGaps()});
            }
        });
        return rows;
    }

    // ---------- CSV ----------

    @Override
    public Export csv(Long reportId) {
        Data d = load(reportId);
        StringBuilder sb = new StringBuilder("﻿");
        if (d.provisional()) {
            row(sb, "DISCLAIMER", DISCLAIMER, "", "");
        }
        row(sb, "section", "name", "value", "note");
        row(sb, "report", "title", title(d), "");
        for (String[] r : summary(d)) {
            row(sb, "summary", r[0], r[1], "");
        }
        d.view().getMetrics().forEach(m -> row(sb, "metric", m.getTitle(), m.getValue(), m.getExplanation()));
        for (ReportKpi k : d.kpis()) {
            row(sb, "kpi:" + k.getKpiKey(), k.getLabel(), k.getValue() == null ? "n/a" : number(k.getValue()) + (k.getUnit() == null ? "" : " " + k.getUnit()),
                    k.getDetail() == null ? "" : k.getDetail());
        }
        d.view().getWarningLines().forEach(w -> row(sb, "warning", w, "", ""));
        d.view().getShelterLines().forEach(s -> row(sb, "shelter", s, "", ""));
        if (d.provisional()) {
            row(sb, "DISCLAIMER", DISCLAIMER, "", "");
        }
        return new Export("post-event-report-" + reportId + ".csv", "text/csv; charset=UTF-8",
                sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cell(cells[i]));
        }
        sb.append("\r\n");
    }

    /** Quotes a cell and neutralises spreadsheet formulas (a cell must not start with = + - or @). */
    static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    // ---------- PDF ----------

    @Override
    public Export pdf(Long reportId) {
        Data d = load(reportId);
        SimplePdf pdf = new SimplePdf("DEWECS - Post-event analysis report #" + reportId, d.provisional() ? DISCLAIMER : null);
        pdf.heading(title(d));
        for (String[] r : summary(d)) {
            pdf.text(r[0] + ": " + r[1]);
        }
        pdf.heading("Metrics");
        d.view().getMetrics().forEach(m -> {
            pdf.bold(m.getTitle() + ": " + m.getValue());
            pdf.text("   " + m.getExplanation());
        });
        pdf.heading("Performance indicators");
        for (ReportKpi k : d.kpis()) {
            if (k.getKpiKey().equals("share") || k.getKpiKey().equals("need") || k.getKpiKey().equals("distributed")) {
                continue;
            }
            pdf.text(k.getLabel() + ": " + (k.getValue() == null ? "n/a" : number(k.getValue()) + (k.getUnit() == null ? "" : " " + k.getUnit()))
                    + (k.getDetail() == null ? "" : "  (" + k.getDetail() + ")"));
        }
        List<ReportKpi> shares = d.kpis().stream().filter(k -> k.getKpiKey().equals("share")).toList();
        if (!shares.isEmpty()) {
            pdf.heading("Contribution by agency (share of units dispatched)");
            for (ReportKpi k : shares) {
                pdf.bar(k.getLabel(), k.getValue() == null ? 0 : k.getValue() / 100.0,
                        (k.getValue() == null ? "n/a" : number(k.getValue()) + " %"));
            }
        }
        List<ReportKpi> needs = d.kpis().stream().filter(k -> k.getKpiKey().equals("need")).toList();
        List<ReportKpi> dists = d.kpis().stream().filter(k -> k.getKpiKey().equals("distributed")).toList();
        if (!needs.isEmpty() || !dists.isEmpty()) {
            pdf.heading("Distribution versus need (units)");
            double max = d.kpis().stream().filter(k -> k.getKpiKey().equals("need") || k.getKpiKey().equals("distributed"))
                    .mapToDouble(k -> k.getValue() == null ? 0 : k.getValue()).max().orElse(1);
            for (ReportKpi k : needs) {
                pdf.bar(k.getLabel() + " - needed", max == 0 ? 0 : k.getValue() / max, number(k.getValue()));
            }
            for (ReportKpi k : dists) {
                pdf.bar(k.getLabel() + " - dispatched", max == 0 ? 0 : k.getValue() / max, number(k.getValue()));
            }
        }
        pdf.heading("Warnings published for the event");
        if (d.view().getWarningLines().isEmpty()) {
            pdf.text("No warning was published for this event.");
        }
        d.view().getWarningLines().forEach(pdf::text);
        pdf.heading("Shelters in the region");
        if (d.view().getShelterLines().isEmpty()) {
            pdf.text("The region has no shelters.");
        }
        d.view().getShelterLines().forEach(pdf::text);
        if (d.provisional()) {
            pdf.gap();
            pdf.bold(DISCLAIMER);
        }
        return new Export("post-event-report-" + reportId + ".pdf", "application/pdf", pdf.build());
    }

    // ---------- small helpers ----------

    private String label(String enumName) {
        String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String time(LocalDateTime t) {
        return t == null ? "-" : TIME.format(t);
    }

    private String number(Double v) {
        if (v == null) {
            return "n/a";
        }
        return v == Math.floor(v) ? String.valueOf(v.longValue()) : String.valueOf(v);
    }
}
