package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.exception.HazardEventValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.HazardEventService.HazardReview;
import com.group18.dewecs.service.HazardEventService.HazardSummary;
import com.group18.dewecs.service.impl.HazardEventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HazardEventServiceImplTest {

    /** 2026-10-10 08:00 in Colombo. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 8, 0);

    @Mock
    private HazardEventRepository hazardEventRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private GroundReportRepository groundReportRepository;
    @Mock
    private WarningRepository warningRepository;

    private HazardEventServiceImpl service;
    private District galle;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T02:30:00Z"), ZoneId.of("Asia/Colombo"));
        service = new HazardEventServiceImpl(hazardEventRepository, districtRepository, groundReportRepository,
                warningRepository, clock);
        galle = new District();
        galle.setId(4L);
        galle.setName("Galle");
        when(districtRepository.findById(4L)).thenReturn(Optional.of(galle));
        when(hazardEventRepository.save(any(HazardEvent.class))).thenAnswer(i -> i.getArgument(0));
    }

    private HazardEvent event(Long id, HazardEventStatus status, LocalDateTime occurredAt) {
        HazardEvent e = new HazardEvent();
        e.setId(id);
        e.setHazardType(HazardType.FLOOD);
        e.setSeverityLevel(Severity.HIGH);
        e.setStatus(status);
        e.setDistrict(galle);
        e.setOccurredAt(occurredAt);
        when(hazardEventRepository.findById(id)).thenReturn(Optional.of(e));
        return e;
    }

    private GroundReport report(LocalDateTime submittedAt) {
        GroundReport r = new GroundReport();
        r.setSubmittedAt(submittedAt);
        return r;
    }

    @Test
    void create_startsAnActiveEventNowWhenNoTimeIsGiven() {
        HazardEvent created = service.create("flood", "high", 4L, null);

        assertThat(created.getStatus()).isEqualTo(HazardEventStatus.ACTIVE);
        assertThat(created.getHazardType()).isEqualTo(HazardType.FLOOD);
        assertThat(created.getSeverityLevel()).isEqualTo(Severity.HIGH);
        assertThat(created.getDistrict()).isSameAs(galle);
        assertThat(created.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void create_keepsAnEarlierStartTime_andRefusesOneInTheFuture() {
        LocalDateTime earlier = NOW.minusHours(3);
        assertThat(service.create("LANDSLIDE", "CRITICAL", 4L, earlier).getOccurredAt()).isEqualTo(earlier);

        assertThatThrownBy(() -> service.create("FLOOD", "HIGH", 4L, NOW.plusHours(1)))
                .isInstanceOf(HazardEventValidationException.class).hasMessageContaining("future");
        // A little clock difference between a phone or browser and the server is tolerated.
        assertThat(service.create("FLOOD", "HIGH", 4L, NOW.plusMinutes(3))).isNotNull();
    }

    @Test
    void create_rejectsABadTypeSeverityOrDistrict() {
        assertThatThrownBy(() -> service.create("EARTHQUAKE", "HIGH", 4L, null))
                .isInstanceOf(HazardEventValidationException.class).hasMessageContaining("hazard type");
        assertThatThrownBy(() -> service.create("FLOOD", "EXTREME", 4L, null))
                .isInstanceOf(HazardEventValidationException.class).hasMessageContaining("severity");
        assertThatThrownBy(() -> service.create(null, "HIGH", 4L, null))
                .isInstanceOf(HazardEventValidationException.class);
        when(districtRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("FLOOD", "HIGH", 99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(hazardEventRepository, never()).save(any());
    }

    @Test
    void updateStatus_onlyMovesForward() {
        HazardEvent active = event(1L, HazardEventStatus.ACTIVE, NOW);

        assertThat(service.updateStatus(1L, "contained").getStatus()).isEqualTo(HazardEventStatus.CONTAINED);
        assertThat(service.updateStatus(1L, "RESOLVED").getStatus()).isEqualTo(HazardEventStatus.RESOLVED);

        assertThatThrownBy(() -> service.updateStatus(1L, "ACTIVE"))
                .isInstanceOf(HazardEventValidationException.class).hasMessageContaining("cannot go back");
        assertThatThrownBy(() -> service.updateStatus(1L, "RESOLVED"))
                .isInstanceOf(HazardEventValidationException.class);
        assertThatThrownBy(() -> service.updateStatus(1L, "FINISHED"))
                .isInstanceOf(HazardEventValidationException.class).hasMessageContaining("Invalid status");
        assertThat(active.getStatus()).isEqualTo(HazardEventStatus.RESOLVED);
    }

    @Test
    void getById_ofAnUnknownEvent_isNotFound() {
        when(hazardEventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void review_keepsOnlyEvidenceFromTwentyFourHoursBeforeTheEvent() {
        HazardEvent e = event(1L, HazardEventStatus.ACTIVE, NOW.minusHours(2));
        GroundReport recent = report(NOW.minusHours(1));
        GroundReport justBefore = report(NOW.minusHours(2).minusHours(23));
        GroundReport tooOld = report(NOW.minusDays(5));
        when(groundReportRepository.findByDistrict_IdAndStatusInOrderBySubmittedAtDescIdDesc(anyLong(), anyCollection()))
                .thenReturn(List.of(recent, justBefore, tooOld));
        Warning w = new Warning();
        when(warningRepository.findByHazardEvent_IdAndStatusIn(anyLong(), anyCollection())).thenReturn(List.of(w));

        HazardReview review = service.review(1L);

        assertThat(review.event()).isSameAs(e);
        assertThat(review.verifiedReports()).containsExactly(recent, justBefore);
        assertThat(review.warnings()).containsExactly(w);
    }

    @Test
    void activeSummaries_countEvidenceAndLiveWarningsPerEvent() {
        HazardEvent e = event(1L, HazardEventStatus.ACTIVE, NOW.minusHours(2));
        when(hazardEventRepository.findByStatusOrderByOccurredAtDescIdDesc(HazardEventStatus.ACTIVE))
                .thenReturn(List.of(e));
        when(groundReportRepository.findByDistrict_IdAndStatusInOrderBySubmittedAtDescIdDesc(anyLong(), anyCollection()))
                .thenReturn(List.of(report(NOW.minusHours(1)), report(NOW.minusMinutes(10))));
        when(warningRepository.findByHazardEvent_IdAndStatusIn(anyLong(), anyCollection()))
                .thenReturn(List.of(new Warning()));

        List<HazardSummary> summaries = service.listActiveSummaries();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).verifiedReports()).isEqualTo(2);
        assertThat(summaries.get(0).liveWarnings()).isEqualTo(1);
    }
}
