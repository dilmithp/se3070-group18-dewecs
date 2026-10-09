package com.group18.dewecs.service;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.impl.WarningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UC-01 rules added on top of draft, publish and retract: river basins, broadcast on publish, escalation, re-send. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarningServiceEscalationTest {

    @Mock
    private WarningRepository warningRepository;
    @Mock
    private HazardEventRepository hazardEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private RiverBasinRepository riverBasinRepository;
    @Mock
    private AlertBroadcastService alertBroadcastService;

    private WarningServiceImpl service;
    private District colombo;
    private HazardEvent event;
    private User officer;

    @BeforeEach
    void setUp() {
        service = new WarningServiceImpl(warningRepository, hazardEventRepository, userRepository, districtRepository,
                riverBasinRepository, alertBroadcastService);
        colombo = district(1L, "Colombo");
        event = new HazardEvent();
        event.setId(10L);
        event.setHazardType(HazardType.FLOOD);
        event.setSeverityLevel(Severity.HIGH);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(colombo);
        event.setOccurredAt(LocalDateTime.now());
        officer = new User();
        officer.setId(20L);
        officer.setFullName("Officer");
    }

    private District district(Long id, String name) {
        District d = new District();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private Warning warning(Long id, WarningStatus status, Severity severity) {
        Warning w = new Warning();
        w.setId(id);
        w.setHazardEvent(event);
        w.setSeverity(severity);
        w.setStatus(status);
        w.setMessage("Move to higher ground");
        w.setIssuedBy(officer);
        w.setIssuedAt(LocalDateTime.of(2026, 10, 9, 8, 0));
        return w;
    }

    private void stubFind(Warning w) {
        when(warningRepository.findById(w.getId())).thenReturn(Optional.of(w));
        when(warningRepository.save(any(Warning.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- river basin ----------

    @Test
    void createDraft_withABasin_coversTheEventDistrictAndEveryBasinDistrictOnce() {
        RiverBasin kelani = new RiverBasin();
        kelani.setId(3L);
        kelani.setName("Kelani Ganga");
        // A different object with the same id as the event district must not be counted twice.
        kelani.setDistricts(new HashSet<>(List.of(district(1L, "Colombo"), district(2L, "Gampaha"))));
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));
        when(riverBasinRepository.findById(3L)).thenReturn(Optional.of(kelani));
        when(warningRepository.save(any(Warning.class))).thenAnswer(i -> i.getArgument(0));

        Warning draft = service.createDraft(10L, "HIGH", "msg", null, Set.of("SMS"), 20L, 3L);

        assertThat(draft.getAffectedDistricts().stream().map(District::getName).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("Colombo", "Gampaha");
        assertThat(draft.getAffectedDistricts()).hasSize(2);
    }

    @Test
    void createDraft_withoutABasin_leavesTheDistrictSetEmptyAndTheEventDistrictApplies() {
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));
        when(warningRepository.save(any(Warning.class))).thenAnswer(i -> i.getArgument(0));

        Warning draft = service.createDraft(10L, "HIGH", "msg", null, Set.of(), 20L, null);

        assertThat(draft.getAffectedDistricts()).isEmpty();
        assertThat(draft.effectiveDistricts()).extracting(District::getName).containsExactly("Colombo");
        verify(riverBasinRepository, never()).findById(any());
    }

    @Test
    void createDraft_withAnUnknownBasin_isNotFound() {
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));
        when(riverBasinRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDraft(10L, "HIGH", "msg", null, Set.of(), 20L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDraft_keepsTheDistrictsWhenNoBasinIsGivenAndReplacesThemWhenOneIs() {
        Warning draft = warning(8L, WarningStatus.DRAFT, Severity.HIGH);
        draft.setAffectedDistricts(new HashSet<>(Set.of(colombo, district(4L, "Kalutara"))));
        stubFind(draft);

        service.updateDraft(8L, "HIGH", "new text", null, Set.of(), null);
        assertThat(draft.getAffectedDistricts()).hasSize(2);

        RiverBasin gin = new RiverBasin();
        gin.setId(6L);
        gin.setDistricts(new HashSet<>(Set.of(district(5L, "Galle"))));
        when(riverBasinRepository.findById(6L)).thenReturn(Optional.of(gin));

        service.updateDraft(8L, "HIGH", "new text", null, Set.of(), 6L);
        assertThat(draft.getAffectedDistricts()).extracting(District::getName)
                .containsExactlyInAnyOrder("Colombo", "Galle");
    }

    // ---------- publish and broadcast ----------

    @Test
    void publish_broadcastsOnItsChannels() {
        Warning draft = warning(8L, WarningStatus.DRAFT, Severity.HIGH);
        stubFind(draft);

        service.publish(8L);

        verify(alertBroadcastService).broadcast(draft, DeliveryTrigger.PUBLISH, null);
    }

    @Test
    void publish_withNoDistrictAtAll_isRefusedAndNothingIsSent() {
        event.setDistrict(null);
        Warning draft = warning(8L, WarningStatus.DRAFT, Severity.HIGH);
        stubFind(draft);

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(WarningValidationException.class).hasMessageContaining("region");
        verify(alertBroadcastService, never()).broadcast(any(), any(), any());
    }

    // ---------- escalation ----------

    @Test
    void escalate_raisesTheSeverity_marksItUpdated_keepsTheIssueTime_andSendsAgain() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.HIGH);
        LocalDateTime issuedAt = issued.getIssuedAt();
        stubFind(issued);

        Warning result = service.escalate(8L, "critical", "Water level above the danger mark", null);

        assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.getStatus()).isEqualTo(WarningStatus.UPDATED);
        assertThat(result.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(result.getMessage()).startsWith("Move to higher ground")
                .endsWith("[Escalated to CRITICAL] Water level above the danger mark");
        verify(alertBroadcastService).broadcast(eq(issued), eq(DeliveryTrigger.ESCALATION),
                eq("Escalated from HIGH to CRITICAL."));
    }

    @Test
    void escalate_withoutANote_leavesTheMessageAlone() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.MODERATE);
        stubFind(issued);

        service.escalate(8L, "HIGH", "  ", null);

        assertThat(issued.getMessage()).isEqualTo("Move to higher ground");
    }

    @Test
    void escalate_mustRaiseTheSeverity() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.HIGH);
        stubFind(issued);

        assertThatThrownBy(() -> service.escalate(8L, "HIGH", null, null))
                .isInstanceOf(WarningValidationException.class).hasMessageContaining("raise the severity");
        assertThatThrownBy(() -> service.escalate(8L, "LOW", null, null))
                .isInstanceOf(WarningValidationException.class);
        assertThat(issued.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(issued.getStatus()).isEqualTo(WarningStatus.ISSUED);
        verify(alertBroadcastService, never()).broadcast(any(), any(), any());
    }

    @Test
    void escalate_onlyWorksOnPublishedWarnings() {
        for (WarningStatus status : List.of(WarningStatus.DRAFT, WarningStatus.CANCELLED, WarningStatus.EXPIRED)) {
            Warning w = warning(8L, status, Severity.LOW);
            stubFind(w);
            assertThatThrownBy(() -> service.escalate(8L, "CRITICAL", null, null))
                    .isInstanceOf(WarningValidationException.class).hasMessageContaining("published");
        }
        verify(alertBroadcastService, never()).broadcast(any(), any(), any());
    }

    @Test
    void escalate_rejectsAnInvalidSeverityAndAPastExpiry() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.LOW);
        stubFind(issued);

        assertThatThrownBy(() -> service.escalate(8L, "EXTREME", null, null))
                .isInstanceOf(WarningValidationException.class).hasMessageContaining("Invalid severity");
        assertThatThrownBy(() -> service.escalate(8L, "HIGH", null, LocalDateTime.now().minusMinutes(1)))
                .isInstanceOf(WarningValidationException.class).hasMessageContaining("future");
    }

    @Test
    void escalate_canGoUpInSteps_andANewExpiryIsApplied() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.MODERATE);
        stubFind(issued);
        LocalDateTime later = LocalDateTime.now().plusDays(1);

        service.escalate(8L, "HIGH", null, null);
        service.escalate(8L, "CRITICAL", null, later);

        assertThat(issued.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(issued.getStatus()).isEqualTo(WarningStatus.UPDATED);
        assertThat(issued.getExpiresAt()).isEqualTo(later);
    }

    @Test
    void anEscalatedWarningCanStillBeRetractedAndExpires() {
        Warning updated = warning(8L, WarningStatus.UPDATED, Severity.CRITICAL);
        stubFind(updated);
        assertThat(service.retract(8L).getStatus()).isEqualTo(WarningStatus.CANCELLED);

        Warning overdue = warning(9L, WarningStatus.UPDATED, Severity.CRITICAL);
        overdue.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(warningRepository.findByStatus(WarningStatus.UPDATED)).thenReturn(List.of(overdue));
        when(warningRepository.findAll()).thenReturn(List.of(overdue));

        service.list(null, null);

        assertThat(overdue.getStatus()).isEqualTo(WarningStatus.EXPIRED);
    }

    // ---------- re-send and the log ----------

    @Test
    void rebroadcast_resendsOnlyForPublishedWarnings() {
        Warning issued = warning(8L, WarningStatus.ISSUED, Severity.HIGH);
        stubFind(issued);
        List<AlertDeliveryLog> rows = List.of(new AlertDeliveryLog());
        when(alertBroadcastService.retryFailed(issued)).thenReturn(rows);

        assertThat(service.rebroadcast(8L)).isSameAs(rows);

        Warning draft = warning(9L, WarningStatus.DRAFT, Severity.HIGH);
        stubFind(draft);
        assertThatThrownBy(() -> service.rebroadcast(9L)).isInstanceOf(WarningValidationException.class);
        verify(alertBroadcastService, never()).retryFailed(draft);
    }

    @Test
    void theDeliveryLogOfAnUnknownWarningIsNotFound() {
        when(warningRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listDeliveryLog(404L)).isInstanceOf(ResourceNotFoundException.class);
        verify(alertBroadcastService, never()).listLog(any());
    }

    // ---------- listing ----------

    @Test
    void listForADistrict_alsoIncludesBasinWarningsOnce() {
        Warning own = warning(1L, WarningStatus.ISSUED, Severity.HIGH);
        Warning viaBasin = warning(2L, WarningStatus.ISSUED, Severity.HIGH);
        Warning viaBasinDraft = warning(3L, WarningStatus.DRAFT, Severity.HIGH);
        when(warningRepository.findByStatusAndHazardEvent_District_Id(WarningStatus.ISSUED, 1L)).thenReturn(List.of(own));
        when(warningRepository.findByAffectedDistricts_Id(1L)).thenReturn(List.of(own, viaBasin, viaBasinDraft));

        List<Warning> result = service.list(WarningStatus.ISSUED, 1L);

        assertThat(result).containsExactly(own, viaBasin);
        verify(warningRepository, never()).findByAffectedDistricts_Id(isNull());
    }
}
