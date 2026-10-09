package com.group18.dewecs.service;

import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.MetricType;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.ReportMetric;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.PostEventReportRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ReportMetricRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.impl.PostEventReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostEventReportServiceImplTest {

    private static final LocalDateTime EVENT_START = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Mock
    private PostEventReportRepository reportRepository;
    @Mock
    private ReportMetricRepository metricRepository;
    @Mock
    private HazardEventRepository hazardEventRepository;
    @Mock
    private WarningRepository warningRepository;
    @Mock
    private ShelterRepository shelterRepository;
    @Mock
    private CitizenRepository citizenRepository;
    @Mock
    private ReliefConsignmentRepository consignmentRepository;

    private PostEventReportServiceImpl service;
    private District district;
    private HazardEvent event;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneId.of("Asia/Colombo"));
        service = new PostEventReportServiceImpl(reportRepository, metricRepository, hazardEventRepository,
                warningRepository, shelterRepository, citizenRepository, consignmentRepository, clock);

        district = new District();
        district.setId(7L);
        district.setName("Galle");
        event = new HazardEvent();
        event.setId(3L);
        event.setDistrict(district);
        event.setOccurredAt(EVENT_START);
    }

    @Test
    void generate_forUnknownEvent_throwsNotFoundAndSavesNothing() {
        when(hazardEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generate_computesAllFourMetricsAndLinksWarningsAndShelters() {
        when(hazardEventRepository.findById(3L)).thenReturn(Optional.of(event));
        Warning early = warning(1L, EVENT_START.plusMinutes(90));
        Warning later = warning(2L, EVENT_START.plusMinutes(300));
        when(warningRepository.findByHazardEvent_IdAndStatusIn(any(), anyCollection()))
                .thenReturn(List.of(later, early));
        Shelter a = shelter(10L, 100, 40);
        Shelter b = shelter(11L, 100, 10);
        when(shelterRepository.findByDistrict_Id(7L)).thenReturn(List.of(a, b));
        when(citizenRepository.countByDistrict_Id(7L)).thenReturn(250L);
        when(consignmentRepository.findByStatusAndShelter_District_Id(ConsignmentStatus.DELIVERED, 7L))
                .thenReturn(List.of(consignment(EVENT_START.plusHours(2), 30, 20),
                        consignment(EVENT_START.minusDays(3), 500)));
        when(reportRepository.save(any(PostEventReport.class))).thenAnswer(i -> i.getArgument(0));

        PostEventReport report = service.generate(3L);

        assertThat(report.getGeneratedAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 5, 30));
        assertThat(report.getRelatedWarnings()).containsExactlyInAnyOrder(early, later);
        assertThat(report.getRelatedShelters()).containsExactlyInAnyOrder(a, b);
        Map<MetricType, Double> metrics = savedMetrics();
        assertThat(metrics).containsEntry(MetricType.ALERT_TIMELINE, 90.0)
                .containsEntry(MetricType.CITIZENS_REACHED, 250.0)
                .containsEntry(MetricType.SHELTER_OCCUPANCY, 25.0)
                .containsEntry(MetricType.RESOURCE_DISTRIBUTION, 50.0);
    }

    @Test
    void generate_withNoPublishedWarning_skipsTimelineAndReachesNobody() {
        when(hazardEventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(warningRepository.findByHazardEvent_IdAndStatusIn(any(), anyCollection())).thenReturn(List.of());
        when(shelterRepository.findByDistrict_Id(7L)).thenReturn(List.of());
        when(consignmentRepository.findByStatusAndShelter_District_Id(any(), any())).thenReturn(List.of());
        when(reportRepository.save(any(PostEventReport.class))).thenAnswer(i -> i.getArgument(0));

        service.generate(3L);

        Map<MetricType, Double> metrics = savedMetrics();
        assertThat(metrics).doesNotContainKey(MetricType.ALERT_TIMELINE)
                .containsEntry(MetricType.CITIZENS_REACHED, 0.0)
                .containsEntry(MetricType.SHELTER_OCCUPANCY, 0.0)
                .containsEntry(MetricType.RESOURCE_DISTRIBUTION, 0.0);
        verify(citizenRepository, never()).countByDistrict_Id(any());
    }

    @Test
    void generate_withWarningIssuedBeforeTheEvent_givesANegativeLeadTime() {
        when(hazardEventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(warningRepository.findByHazardEvent_IdAndStatusIn(any(), anyCollection()))
                .thenReturn(List.of(warning(1L, EVENT_START.minusMinutes(45))));
        when(shelterRepository.findByDistrict_Id(7L)).thenReturn(List.of());
        when(consignmentRepository.findByStatusAndShelter_District_Id(any(), any())).thenReturn(List.of());
        when(reportRepository.save(any(PostEventReport.class))).thenAnswer(i -> i.getArgument(0));

        service.generate(3L);

        assertThat(savedMetrics()).containsEntry(MetricType.ALERT_TIMELINE, -45.0);
    }

    @Test
    void listMetrics_forUnknownReport_throwsNotFound() {
        when(reportRepository.findWithRelationsById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMetrics(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    private Map<MetricType, Double> savedMetrics() {
        ArgumentCaptor<List<ReportMetric>> captor = ArgumentCaptor.forClass(List.class);
        verify(metricRepository).saveAll(captor.capture());
        Map<MetricType, Double> byType = new java.util.EnumMap<>(MetricType.class);
        captor.getValue().forEach(m -> byType.put(m.getMetricType(), m.getValue()));
        return byType;
    }

    private Warning warning(Long id, LocalDateTime issuedAt) {
        Warning w = new Warning();
        w.setId(id);
        w.setStatus(WarningStatus.ISSUED);
        w.setIssuedAt(issuedAt);
        return w;
    }

    private Shelter shelter(Long id, int capacity, int occupancy) {
        Shelter s = new Shelter();
        s.setId(id);
        s.setCapacity(capacity);
        s.setCurrentOccupancy(occupancy);
        return s;
    }

    private ReliefConsignment consignment(LocalDateTime dispatchedAt, int... quantities) {
        ReliefConsignment c = new ReliefConsignment();
        c.setDispatchedAt(dispatchedAt);
        for (int q : quantities) {
            ConsignmentItem item = new ConsignmentItem();
            item.setQuantity(q);
            c.getItems().add(item);
        }
        return c;
    }
}
