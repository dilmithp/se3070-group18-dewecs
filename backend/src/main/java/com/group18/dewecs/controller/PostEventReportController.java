package com.group18.dewecs.controller;

import com.group18.dewecs.mapper.PostEventReportMapper;
import com.group18.dewecs.service.PostEventReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/post-event-reports")
public class PostEventReportController {

    private final PostEventReportService reportService;
    private final PostEventReportMapper reportMapper;

    public PostEventReportController(PostEventReportService reportService, PostEventReportMapper reportMapper) {
        this.reportService = reportService;
        this.reportMapper = reportMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("reports", reportMapper.toSummaryList(reportService.list()));
        model.addAttribute("events", reportService.listHazardEvents());
        return "post-event-reports/list";
    }

    @PostMapping
    // TODO: restrict to DMC-officer role once auth lands
    public String generate(@RequestParam(required = false) Long hazardEventId,
                            RedirectAttributes redirectAttributes) {
        if (hazardEventId == null) {
            redirectAttributes.addFlashAttribute("error", "Choose a hazard event to report on.");
            return "redirect:/post-event-reports";
        }
        var report = reportService.generate(hazardEventId);
        redirectAttributes.addFlashAttribute("message", "Post-event report generated.");
        return "redirect:/post-event-reports/" + report.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var report = reportService.getById(id);
        model.addAttribute("report", reportMapper.toResponse(report, reportService.listMetrics(id)));
        return "post-event-reports/detail";
    }
}
