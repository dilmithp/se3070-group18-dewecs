package com.group18.dewecs.controller;

import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.mapper.GroundReportMapper;
import com.group18.dewecs.service.GroundReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ground-reports")
public class GroundReportController {

    private final GroundReportService groundReportService;
    private final GroundReportMapper groundReportMapper;

    public GroundReportController(GroundReportService groundReportService, GroundReportMapper groundReportMapper) {
        this.groundReportService = groundReportService;
        this.groundReportMapper = groundReportMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(required = false) Long districtId,
                        @RequestParam(required = false) String category,
                        Model model) {
        GroundReportStatus statusFilter = parseEnum(GroundReportStatus.class, status);
        HazardType categoryFilter = parseEnum(HazardType.class, category);

        model.addAttribute("reports", groundReportMapper.toResponseList(
                groundReportService.list(statusFilter, districtId, categoryFilter)));
        model.addAttribute("statuses", GroundReportStatus.values());
        model.addAttribute("categories", HazardType.values());
        model.addAttribute("districts", groundReportService.listDistricts());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDistrictId", districtId);
        model.addAttribute("selectedCategory", category);
        return "ground-reports/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("report", groundReportMapper.toResponse(groundReportService.getById(id)));
        model.addAttribute("users", groundReportService.listUsersForSelection());
        return "ground-reports/detail";
    }

    @PostMapping("/{id}/review")
    // TODO: restrict to DMC-officer role once auth lands
    public String markReviewed(@PathVariable Long id, @RequestParam Long reviewingUserId,
                                RedirectAttributes redirectAttributes) {
        try {
            groundReportService.markReviewed(id, reviewingUserId);
            redirectAttributes.addFlashAttribute("message", "Report marked reviewed.");
        } catch (GroundReportValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ground-reports/" + id;
    }

    @PostMapping("/{id}/action")
    // TODO: restrict to DMC-officer role once auth lands
    public String action(@PathVariable Long id, @RequestParam Long reviewingUserId, @RequestParam String note,
                          RedirectAttributes redirectAttributes) {
        try {
            groundReportService.action(id, reviewingUserId, note);
            redirectAttributes.addFlashAttribute("message", "Report actioned.");
        } catch (GroundReportValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ground-reports/" + id;
    }

    @PostMapping("/{id}/dismiss")
    // TODO: restrict to DMC-officer role once auth lands
    public String dismiss(@PathVariable Long id, @RequestParam Long reviewingUserId,
                           RedirectAttributes redirectAttributes) {
        try {
            groundReportService.dismiss(id, reviewingUserId);
            redirectAttributes.addFlashAttribute("message", "Report dismissed.");
        } catch (GroundReportValidationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/ground-reports/" + id;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
