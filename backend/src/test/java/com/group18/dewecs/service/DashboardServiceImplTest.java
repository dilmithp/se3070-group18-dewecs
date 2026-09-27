package com.group18.dewecs.service;

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
import com.group18.dewecs.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private WarningRepository warningRepository;
    @Mock
    private ShelterRepository shelterRepository;
    @Mock
    private RescueRequestRepository rescueRequestRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private GroundReportRepository groundReportRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(warningRepository, shelterRepository, rescueRequestRepository,
                resourceRepository, groundReportRepository);
    }

    @Test
    void getSummary_aggregatesCountsFromEachRepository() {
        when(warningRepository.countByStatus(WarningStatus.ISSUED)).thenReturn(3L);
        when(shelterRepository.countByStatus(ShelterStatus.FULL)).thenReturn(2L);
        when(rescueRequestRepository.countByStatus(RescueRequestStatus.PENDING)).thenReturn(7L);
        when(resourceRepository.countByQuantityLessThan(Resource.LOW_STOCK_THRESHOLD)).thenReturn(4L);
        when(groundReportRepository.countByStatus(GroundReportStatus.PENDING_REVIEW)).thenReturn(9L);

        DashboardSummary summary = dashboardService.getSummary();

        assertThat(summary.getOpenWarnings()).isEqualTo(3L);
        assertThat(summary.getFullShelters()).isEqualTo(2L);
        assertThat(summary.getPendingRescueRequests()).isEqualTo(7L);
        assertThat(summary.getLowStockSupplies()).isEqualTo(4L);
        assertThat(summary.getUnreviewedReports()).isEqualTo(9L);
    }
}
