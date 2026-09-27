package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.dto.DashboardSummary;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.DashboardService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final WarningRepository warningRepository;
    private final ShelterRepository shelterRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final ResourceRepository resourceRepository;
    private final GroundReportRepository groundReportRepository;

    public DashboardServiceImpl(WarningRepository warningRepository,
                                 ShelterRepository shelterRepository,
                                 RescueRequestRepository rescueRequestRepository,
                                 ResourceRepository resourceRepository,
                                 GroundReportRepository groundReportRepository) {
        this.warningRepository = warningRepository;
        this.shelterRepository = shelterRepository;
        this.rescueRequestRepository = rescueRequestRepository;
        this.resourceRepository = resourceRepository;
        this.groundReportRepository = groundReportRepository;
    }

    @Override
    public DashboardSummary getSummary() {
        return new DashboardSummary(
                warningRepository.countByStatus(WarningStatus.ISSUED),
                shelterRepository.countByStatus(ShelterStatus.FULL),
                rescueRequestRepository.countByStatus(RescueRequestStatus.PENDING),
                resourceRepository.countByQuantityLessThan(Resource.LOW_STOCK_THRESHOLD),
                groundReportRepository.countByStatus(GroundReportStatus.PENDING_REVIEW)
        );
    }
}
