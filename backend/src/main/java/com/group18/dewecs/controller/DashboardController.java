package com.group18.dewecs.controller;

import com.group18.dewecs.mapper.HazardEventMapper;
import com.group18.dewecs.service.DashboardService;
import com.group18.dewecs.service.HazardEventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final HazardEventService hazardEventService;
    private final HazardEventMapper hazardEventMapper;

    public DashboardController(DashboardService dashboardService, HazardEventService hazardEventService,
                                HazardEventMapper hazardEventMapper) {
        this.dashboardService = dashboardService;
        this.hazardEventService = hazardEventService;
        this.hazardEventMapper = hazardEventMapper;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("activeHazards", hazardEventMapper.toSummaryList(hazardEventService.listActiveSummaries()));
        return "dashboard";
    }
}
