package com.group18.dewecs.controller;

import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.exception.IncompleteDataException;
import com.group18.dewecs.exception.ReportValidationException;
import com.group18.dewecs.mapper.PostEventReportMapper;
import com.group18.dewecs.mapper.ReportAnalyticsMapper;
import com.group18.dewecs.service.PostEventReportService;
import com.group18.dewecs.service.ReportExportService;
import com.group18.dewecs.service.ReportExportService.Export;
import com.group18.dewecs.service.ReportingAnalyticsService;
import com.group18.dewecs.service.ReportingAnalyticsService.GapHandling;
import com.group18.dewecs.service.ReportingAnalyticsService.Parameters;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** Post-event analytics and reporting: define the parameters, preview the dashboard, approve, export. */
@Controller
@RequestMapping("/post-event-reports")
public class PostEventReportController {

    private final PostEventReportService reportService;
    private final PostEventReportMapper reportMapper;
    private final ReportingAnalyticsService analyticsService;
    private final ReportAnalyticsMapper analyticsMapper;
    private final ReportExportService exportService;

    public PostEventReportController(PostEventReportService reportService,
                                     PostEventReportMapper reportMapper,
                                     ReportingAnalyticsService analyticsService,
                                     ReportAnalyticsMapper analyticsMapper,
                                     ReportExportService exportService) {
        this.reportService = reportService;
        this.reportMapper = reportMapper;
        this.analyticsService = analyticsService;
        this.analyticsMapper = analyticsMapper;
        this.exportService = exportService;
    }

    @GetMapping
    public String list(Model model) {
        addListData(model);
        return "post-event-reports/list";
    }

    @PostMapping
    // TODO: restrict to DMC-officer role once auth lands
    public String generate(@RequestParam(required = false) Long hazardEventId,
                           @RequestParam(required = false) LocalDateTime from,
                           @RequestParam(required = false) LocalDateTime to,
                           @RequestParam(required = false) Long donorOrganizationId,
                           @RequestParam(required = false) String gapHandling,
                           @RequestParam(required = false) String justification,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (hazardEventId == null) {
            redirectAttributes.addFlashAttribute("error", "Choose a hazard event to report on.");
            return "redirect:/post-event-reports";
        }
        Parameters parameters = new Parameters(hazardEventId, from, to, donorOrganizationId);
        try {
            PostEventReport report = analyticsService.generate(parameters, parseHandling(gapHandling), justification);
            redirectAttributes.addFlashAttribute("message", "Post-event report generated.");
            return "redirect:/post-event-reports/" + report.getId();
        } catch (IncompleteDataException ex) {
            keepForm(model, parameters, gapHandling, justification);
            model.addAttribute("gaps", ex.getGaps());
            addListData(model);
            return "post-event-reports/list";
        } catch (ReportValidationException ex) {
            keepForm(model, parameters, gapHandling, justification);
            model.addAttribute("gaps", analyticsService.findDataGaps(parameters));
            model.addAttribute("error", ex.getMessage());
            addListData(model);
            return "post-event-reports/list";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var report = reportService.getById(id);
        model.addAttribute("report", reportMapper.toResponse(report, reportService.listMetrics(id)));
        model.addAttribute("analytics", analyticsMapper.toView(analyticsService.details(id), analyticsService.kpis(id)));
        return "post-event-reports/detail";
    }

    @PostMapping("/{id}/approve")
    // TODO: restrict to DMC-officer role once auth lands
    public String approve(@PathVariable Long id, @RequestParam(required = false) String officerName,
                          RedirectAttributes redirectAttributes) {
        try {
            analyticsService.approve(id, officerName);
            redirectAttributes.addFlashAttribute("message", "Report approved. It can now be exported as the official summary.");
        } catch (ReportValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/post-event-reports/" + id;
    }

    @GetMapping("/{id}/export.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        return download(exportService.csv(id));
    }

    @GetMapping("/{id}/export.pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        return download(exportService.pdf(id));
    }

    private ResponseEntity<byte[]> download(Export export) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }

    private void addListData(Model model) {
        model.addAttribute("reports", reportMapper.toSummaryList(reportService.list()));
        model.addAttribute("events", reportService.listHazardEvents());
        model.addAttribute("donors", analyticsService.listOrganizations());
    }

    private void keepForm(Model model, Parameters p, String gapHandling, String justification) {
        model.addAttribute("selectedEventId", p.hazardEventId());
        model.addAttribute("fromValue", p.from());
        model.addAttribute("toValue", p.to());
        model.addAttribute("selectedDonorId", p.donorOrganizationId());
        model.addAttribute("selectedHandling", gapHandling);
        model.addAttribute("justificationValue", justification);
    }

    private GapHandling parseHandling(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return GapHandling.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
