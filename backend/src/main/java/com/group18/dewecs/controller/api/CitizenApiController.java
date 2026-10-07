package com.group18.dewecs.controller.api;

import com.group18.dewecs.dto.api.CitizenResponse;
import com.group18.dewecs.dto.api.IdentifyCitizenRequest;
import com.group18.dewecs.dto.api.ReportPageResponse;
import com.group18.dewecs.mapper.GroundReportApiMapper;
import com.group18.dewecs.service.CitizenService;
import com.group18.dewecs.service.CitizenService.IdentifyResult;
import com.group18.dewecs.service.GroundReportSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/citizens")
public class CitizenApiController {

    private final CitizenService citizenService;
    private final GroundReportSubmissionService submissionService;
    private final GroundReportApiMapper mapper;

    public CitizenApiController(CitizenService citizenService, GroundReportSubmissionService submissionService,
                                GroundReportApiMapper mapper) {
        this.citizenService = citizenService;
        this.submissionService = submissionService;
        this.mapper = mapper;
    }

    @PostMapping("/identify")
    public ResponseEntity<CitizenResponse> identify(@Valid @RequestBody IdentifyCitizenRequest request) {
        IdentifyResult result = citizenService.identify(
                request.nic(), request.fullName(), request.phone(), request.districtId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(mapper.toCitizen(result));
    }

    @GetMapping("/{id}/ground-reports")
    public ReportPageResponse reports(@PathVariable Long id,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return mapper.toPage(submissionService.listForCitizen(id, page, size));
    }
}
