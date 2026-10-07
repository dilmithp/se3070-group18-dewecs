package com.group18.dewecs.controller.api;

import com.group18.dewecs.dto.api.ReferenceDataResponse;
import com.group18.dewecs.mapper.GroundReportApiMapper;
import com.group18.dewecs.service.GroundReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReferenceDataApiController {

    private final GroundReportService groundReportService;
    private final GroundReportApiMapper mapper;

    public ReferenceDataApiController(GroundReportService groundReportService, GroundReportApiMapper mapper) {
        this.groundReportService = groundReportService;
        this.mapper = mapper;
    }

    @GetMapping("/reference-data")
    public ReferenceDataResponse referenceData() {
        return mapper.toReferenceData(groundReportService.listDistricts());
    }
}
