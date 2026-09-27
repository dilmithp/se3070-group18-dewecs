package com.group18.dewecs.service;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.service.impl.GroundReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroundReportServiceImplTest {

    @Mock
    private GroundReportRepository groundReportRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private UserRepository userRepository;

    private GroundReportServiceImpl groundReportService;
    private District district;
    private User officer;

    @BeforeEach
    void setUp() {
        groundReportService = new GroundReportServiceImpl(groundReportRepository, districtRepository,
                userRepository);

        district = new District();
        district.setId(1L);
        district.setName("Colombo");

        officer = new User();
        officer.setId(5L);
        officer.setFullName("Field Officer");
        officer.setDistrict(district);
    }

    @Test
    void markReviewed_fromPendingReview_setsVerifiedAndReviewer() {
        GroundReport report = reportOf(GroundReportStatus.PENDING_REVIEW);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(userRepository.findById(5L)).thenReturn(Optional.of(officer));
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        GroundReport reviewed = groundReportService.markReviewed(100L, 5L);

        assertThat(reviewed.getStatus()).isEqualTo(GroundReportStatus.VERIFIED);
        assertThat(reviewed.getVerifiedBy()).isEqualTo(officer);
    }

    @Test
    void markReviewed_onAlreadyVerifiedReport_throwsValidationException() {
        GroundReport report = reportOf(GroundReportStatus.VERIFIED);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> groundReportService.markReviewed(100L, 5L))
                .isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void action_onVerifiedReportWithNote_setsActionedAndNote() {
        GroundReport report = reportOf(GroundReportStatus.VERIFIED);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(userRepository.findById(5L)).thenReturn(Optional.of(officer));
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        GroundReport actioned = groundReportService.action(100L, 5L, "Dispatched rescue team to location.");

        assertThat(actioned.getStatus()).isEqualTo(GroundReportStatus.ACTIONED);
        assertThat(actioned.getActionNote()).isEqualTo("Dispatched rescue team to location.");
    }

    @Test
    void action_withBlankNote_throwsValidationException() {
        GroundReport report = reportOf(GroundReportStatus.VERIFIED);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> groundReportService.action(100L, 5L, "   "))
                .isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void action_onUnreviewedReport_throwsValidationException() {
        GroundReport report = reportOf(GroundReportStatus.PENDING_REVIEW);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> groundReportService.action(100L, 5L, "Some note"))
                .isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void dismiss_fromPendingReview_setsRejected() {
        GroundReport report = reportOf(GroundReportStatus.PENDING_REVIEW);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(userRepository.findById(5L)).thenReturn(Optional.of(officer));
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        GroundReport dismissed = groundReportService.dismiss(100L, 5L);

        assertThat(dismissed.getStatus()).isEqualTo(GroundReportStatus.REJECTED);
    }

    @Test
    void dismiss_onAlreadyActionedReport_throwsValidationException() {
        GroundReport report = reportOf(GroundReportStatus.ACTIONED);
        when(groundReportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> groundReportService.dismiss(100L, 5L))
                .isInstanceOf(GroundReportValidationException.class);
    }

    private GroundReport reportOf(GroundReportStatus status) {
        Citizen citizen = new Citizen();
        citizen.setId(50L);
        citizen.setFullName("Jane Citizen");
        citizen.setDistrict(district);
        citizen.setNic("199012345678");

        GroundReport report = new GroundReport();
        report.setId(100L);
        report.setReportedBy(citizen);
        report.setDistrict(district);
        report.setCategory(HazardType.FLOOD);
        report.setGpsLat(new BigDecimal("6.9271"));
        report.setGpsLng(new BigDecimal("79.8612"));
        report.setDescription("Water rising near the main road.");
        report.setStatus(status);
        report.setSubmittedAt(LocalDateTime.now().minusHours(2));
        return report;
    }
}
