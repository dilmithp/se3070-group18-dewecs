package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.RiverBasinRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.AlertBroadcastService;
import com.group18.dewecs.service.impl.WarningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarningServiceImplTest {

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

    private WarningServiceImpl warningService;
    private HazardEvent hazardEvent;
    private User officer;

    @BeforeEach
    void setUp() {
        warningService = new WarningServiceImpl(warningRepository, hazardEventRepository, userRepository,
                districtRepository, riverBasinRepository, alertBroadcastService);

        District district = new District();
        district.setId(1L);
        district.setName("Colombo");

        hazardEvent = new HazardEvent();
        hazardEvent.setId(10L);
        hazardEvent.setHazardType(HazardType.FLOOD);
        hazardEvent.setSeverityLevel(Severity.HIGH);
        hazardEvent.setStatus(HazardEventStatus.ACTIVE);
        hazardEvent.setDistrict(district);
        hazardEvent.setOccurredAt(LocalDateTime.now());

        officer = new User();
        officer.setId(20L);
        officer.setFullName("Disaster Officer");
        officer.setDistrict(district);
    }

    @Test
    void createDraft_thenPublish_stampsIssuedAtAndTransitionsToIssued() {
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(hazardEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));
        when(warningRepository.save(any(Warning.class))).thenAnswer(inv -> {
            Warning w = inv.getArgument(0);
            if (w.getId() == null) {
                w.setId(99L);
            }
            return w;
        });

        Warning draft = warningService.createDraft(10L, "high", "Evacuate low-lying areas", null,
                Set.of("SMS", "SIREN"), 20L);

        assertThat(draft.getStatus()).isEqualTo(WarningStatus.DRAFT);
        assertThat(draft.getIssuedAt()).isNull();
        assertThat(draft.getSeverity()).isEqualTo(Severity.HIGH);

        when(warningRepository.findById(99L)).thenReturn(Optional.of(draft));

        Warning published = warningService.publish(99L);

        assertThat(published.getStatus()).isEqualTo(WarningStatus.ISSUED);
        assertThat(published.getIssuedAt()).isNotNull();
    }

    @Test
    void publish_withoutMessage_throwsValidationException() {
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(hazardEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));
        when(warningRepository.save(any(Warning.class))).thenAnswer(inv -> {
            Warning w = inv.getArgument(0);
            w.setId(101L);
            return w;
        });

        Warning draft = warningService.createDraft(10L, "MODERATE", "   ", null, Set.of(), 20L);
        when(warningRepository.findById(101L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> warningService.publish(101L))
                .isInstanceOf(WarningValidationException.class)
                .hasMessageContaining("message");
    }

    @Test
    void createDraft_withInvalidSeverity_throwsValidationException() {
        when(hazardEventRepository.findById(10L)).thenReturn(Optional.of(hazardEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officer));

        assertThatThrownBy(() -> warningService.createDraft(10L, "CATASTROPHIC", "msg", null, Set.of(), 20L))
                .isInstanceOf(WarningValidationException.class);
    }

    @Test
    void publish_onAlreadyPublishedWarning_throwsValidationException() {
        Warning issued = new Warning();
        issued.setId(5L);
        issued.setHazardEvent(hazardEvent);
        issued.setSeverity(Severity.HIGH);
        issued.setMessage("Already published");
        issued.setStatus(WarningStatus.ISSUED);
        issued.setIssuedAt(LocalDateTime.now().minusHours(1));
        issued.setIssuedBy(officer);

        when(warningRepository.findById(5L)).thenReturn(Optional.of(issued));

        assertThatThrownBy(() -> warningService.publish(5L))
                .isInstanceOf(WarningValidationException.class);
    }

    @Test
    void updateDraft_onPublishedWarning_throwsValidationException() {
        Warning issued = new Warning();
        issued.setId(9L);
        issued.setHazardEvent(hazardEvent);
        issued.setSeverity(Severity.HIGH);
        issued.setMessage("Already live");
        issued.setStatus(WarningStatus.ISSUED);
        issued.setIssuedAt(LocalDateTime.now());
        issued.setIssuedBy(officer);

        when(warningRepository.findById(9L)).thenReturn(Optional.of(issued));

        assertThatThrownBy(() -> warningService.updateDraft(9L, "LOW", "Updated message", null, Set.of()))
                .isInstanceOf(WarningValidationException.class);
    }

    @Test
    void retract_fromIssued_setsCancelled() {
        Warning issued = new Warning();
        issued.setId(8L);
        issued.setHazardEvent(hazardEvent);
        issued.setSeverity(Severity.HIGH);
        issued.setMessage("Flood warning");
        issued.setStatus(WarningStatus.ISSUED);
        issued.setIssuedAt(LocalDateTime.now());
        issued.setIssuedBy(officer);

        when(warningRepository.findById(8L)).thenReturn(Optional.of(issued));
        when(warningRepository.save(any(Warning.class))).thenAnswer(inv -> inv.getArgument(0));

        Warning retracted = warningService.retract(8L);

        assertThat(retracted.getStatus()).isEqualTo(WarningStatus.CANCELLED);
    }

    @Test
    void listWithNoFilters_transitionsPastDueIssuedWarningsToExpired() {
        Warning overdue = new Warning();
        overdue.setId(7L);
        overdue.setHazardEvent(hazardEvent);
        overdue.setSeverity(Severity.CRITICAL);
        overdue.setMessage("Storm surge expected");
        overdue.setStatus(WarningStatus.ISSUED);
        overdue.setIssuedAt(LocalDateTime.now().minusDays(2));
        overdue.setExpiresAt(LocalDateTime.now().minusHours(1));
        overdue.setIssuedBy(officer);

        when(warningRepository.findByStatus(WarningStatus.ISSUED)).thenReturn(List.of(overdue));
        when(warningRepository.findAll()).thenReturn(List.of(overdue));

        warningService.list(null, null);

        assertThat(overdue.getStatus()).isEqualTo(WarningStatus.EXPIRED);
        verify(warningRepository).saveAll(List.of(overdue));
    }
}
