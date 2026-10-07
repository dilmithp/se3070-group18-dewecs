package com.group18.dewecs.service;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.service.GroundReportSubmissionService.SubmissionResult;
import com.group18.dewecs.service.impl.GroundReportSubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroundReportSubmissionServiceImplTest {

    /** 08:33:11.123 UTC = 14:03:11.123 in Sri Lanka. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T08:33:11.123Z"), ZoneId.of("Asia/Colombo"));
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-07T14:03:11.123");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock
    private GroundReportRepository groundReportRepository;
    @Mock
    private CitizenRepository citizenRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private PhotoStorageService photoStorage;

    private GroundReportSubmissionServiceImpl service;
    private Citizen citizen;
    private District district;

    @BeforeEach
    void setUp() {
        service = new GroundReportSubmissionServiceImpl(groundReportRepository, citizenRepository, districtRepository,
                photoStorage, CLOCK);
        citizen = new Citizen();
        district = new District();
    }

    private void parentsExist() {
        when(citizenRepository.findById(42L)).thenReturn(Optional.of(citizen));
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
    }

    private SubmissionResult submit(String category, String description, String lat, String lng,
                                    LocalDateTime capturedAt) {
        return service.submit(42L, 1L, category, description, new BigDecimal(lat), new BigDecimal(lng), capturedAt);
    }

    private GroundReport saveAndCapture() {
        ArgumentCaptor<GroundReport> captor = ArgumentCaptor.forClass(GroundReport.class);
        verify(groundReportRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void submit_valid_storesPendingReviewWithCoordinatesRoundedToSevenDecimals() {
        parentsExist();
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResult result = submit("flood", "  River is overflowing  ", "6.92712345", "79.86123454", null);

        assertThat(result.created()).isTrue();
        GroundReport saved = saveAndCapture();
        assertThat(saved.getStatus()).isEqualTo(GroundReportStatus.PENDING_REVIEW);
        assertThat(saved.getCategory()).isEqualTo(HazardType.FLOOD);
        assertThat(saved.getDescription()).isEqualTo("River is overflowing");
        assertThat(saved.getGpsLat()).isEqualByComparingTo("6.9271235");
        assertThat(saved.getGpsLng()).isEqualByComparingTo("79.8612345");
        assertThat(saved.getGpsLat().scale()).isEqualTo(7);
        assertThat(saved.getPhotoUrl()).isNull();
    }

    @Test
    void submit_withoutCapturedAt_usesNowInSriLankaTimeAndSkipsReplayLookup() {
        parentsExist();
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        submit("FLOOD", "x", "6.9", "79.8", null);

        assertThat(saveAndCapture().getSubmittedAt()).isEqualTo(NOW);
        verify(groundReportRepository, never())
                .findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(any(), any(), any());
    }

    @Test
    void submit_capturedAtIsTruncatedToMillisecondsBeforeSavingAndLookup() {
        parentsExist();
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime captured = LocalDateTime.parse("2026-10-07T14:00:00.123456789");
        LocalDateTime truncated = LocalDateTime.parse("2026-10-07T14:00:00.123");

        submit("FLOOD", "x", "6.9", "79.8", captured);

        verify(groundReportRepository)
                .findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(42L, HazardType.FLOOD, truncated);
        assertThat(saveAndCapture().getSubmittedAt()).isEqualTo(truncated);
    }

    @Test
    void submit_replay_returnsTheStoredReportUnchangedWithCreatedFalse() {
        parentsExist();
        GroundReport stored = new GroundReport();
        when(groundReportRepository.findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(
                eq(42L), eq(HazardType.FLOOD), any())).thenReturn(Optional.of(stored));

        SubmissionResult result = submit("FLOOD", "x", "6.9", "79.8", NOW.minusMinutes(1));

        assertThat(result.created()).isFalse();
        assertThat(result.report()).isSameAs(stored);
        verify(groundReportRepository, never()).save(any());
    }

    @Test
    void submit_capturedAtMoreThanFiveMinutesAhead_isRejected_butExactlyFiveIsAllowed() {
        assertThatThrownBy(() -> submit("FLOOD", "x", "6.9", "79.8", NOW.plusMinutes(5).plusNanos(1_000_000)))
                .isInstanceOf(GroundReportValidationException.class);

        parentsExist();
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(submit("FLOOD", "x", "6.9", "79.8", NOW.plusMinutes(5)).created()).isTrue();
    }

    @Test
    void submit_invalidFields_areRejected() {
        assertThatThrownBy(() -> submit("EARTHQUAKE", "x", "6.9", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit(null, "x", "6.9", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit("FLOOD", "   ", "6.9", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit("FLOOD", "x".repeat(2001), "6.9", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit("FLOOD", "x", "90.0000001", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit("FLOOD", "x", "-90.1", "79.8", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> submit("FLOOD", "x", "6.9", "180.1", null))
                .isInstanceOf(GroundReportValidationException.class);
        assertThatThrownBy(() -> service.submit(42L, 1L, "FLOOD", "x", null, new BigDecimal("1"), null))
                .isInstanceOf(GroundReportValidationException.class);
    }

    @Test
    void submit_boundaryValuesAreAccepted_includingCalifornia() {
        parentsExist();
        when(groundReportRepository.save(any(GroundReport.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(submit("FLOOD", "x".repeat(2000), "-90", "-180", null).created()).isTrue();
        assertThat(submit("FLOOD", "x", "37.4220", "-122.0841", null).created()).isTrue();
    }

    @Test
    void submit_unknownCitizenOrDistrict_isNotFound() {
        when(citizenRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> submit("FLOOD", "x", "6.9", "79.8", null))
                .isInstanceOf(ResourceNotFoundException.class);

        when(citizenRepository.findById(42L)).thenReturn(Optional.of(citizen));
        when(districtRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> submit("FLOOD", "x", "6.9", "79.8", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void attachPhoto_allowedInPendingStatuses() {
        for (GroundReportStatus status : new GroundReportStatus[] {GroundReportStatus.PENDING_SYNC,
                GroundReportStatus.PENDING_REVIEW, GroundReportStatus.NEEDS_INFO}) {
            GroundReport report = reportWith(status, null);
            when(groundReportRepository.findById(5L)).thenReturn(Optional.of(report));
            when(photoStorage.store(PNG)).thenReturn("new.png");
            when(groundReportRepository.save(report)).thenReturn(report);

            assertThat(service.attachPhoto(5L, PNG).getPhotoUrl()).isEqualTo("/api/v1/photos/new.png");
        }
    }

    @Test
    void attachPhoto_blockedAfterReview() {
        for (GroundReportStatus status : new GroundReportStatus[] {GroundReportStatus.VERIFIED,
                GroundReportStatus.REJECTED, GroundReportStatus.ACTIONED}) {
            when(groundReportRepository.findById(5L)).thenReturn(Optional.of(reportWith(status, null)));

            assertThatThrownBy(() -> service.attachPhoto(5L, PNG)).isInstanceOf(GroundReportValidationException.class);
        }
        verify(photoStorage, never()).store(any());
    }

    @Test
    void attachPhoto_unknownReport_isNotFound() {
        when(groundReportRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachPhoto(5L, PNG)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void attachPhoto_replacingDeletesTheOldApiPhotoAfterTheRowIsSaved() {
        GroundReport report = reportWith(GroundReportStatus.PENDING_REVIEW, "/api/v1/photos/old.jpg");
        when(groundReportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(photoStorage.store(PNG)).thenReturn("new.png");
        when(groundReportRepository.save(report)).thenReturn(report);

        service.attachPhoto(5L, PNG);

        verify(photoStorage).delete("old.jpg");
    }

    @Test
    void attachPhoto_legacyPhotoUrlIsNeverDeleted() {
        GroundReport report = reportWith(GroundReportStatus.PENDING_REVIEW, "https://example.com/some.jpg");
        when(groundReportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(photoStorage.store(PNG)).thenReturn("new.png");
        when(groundReportRepository.save(report)).thenReturn(report);

        service.attachPhoto(5L, PNG);

        verify(photoStorage, never()).delete(anyString());
    }

    @Test
    void attachPhoto_failedRowUpdateDeletesTheNewFileAndKeepsTheOldOne() {
        GroundReport report = reportWith(GroundReportStatus.PENDING_REVIEW, "/api/v1/photos/old.jpg");
        when(groundReportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(photoStorage.store(PNG)).thenReturn("new.png");
        when(groundReportRepository.save(report)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.attachPhoto(5L, PNG)).isInstanceOf(IllegalStateException.class);

        verify(photoStorage).delete("new.png");
        verify(photoStorage, never()).delete("old.jpg");
    }

    @Test
    void attachPhoto_rejectedContentLeavesTheReportUntouched() {
        GroundReport report = reportWith(GroundReportStatus.PENDING_REVIEW, null);
        when(groundReportRepository.findById(5L)).thenReturn(Optional.of(report));
        doThrow(new GroundReportValidationException("not an image")).when(photoStorage).store(any());

        assertThatThrownBy(() -> service.attachPhoto(5L, new byte[] {1, 2, 3}))
                .isInstanceOf(GroundReportValidationException.class);
        verify(groundReportRepository, never()).save(any());
    }

    @Test
    void listForCitizen_clampsPageAndSize() {
        when(citizenRepository.existsById(42L)).thenReturn(true);
        when(groundReportRepository.findByReportedBy_IdOrderBySubmittedAtDescIdDesc(eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertPaging(-3, 0, 1, 0);
        assertPaging(2, 1000, 50, 2);
        assertPaging(0, 20, 20, 0);
        assertPaging(1, 7, 7, 1);
    }

    private void assertPaging(int page, int size, int expectedSize, int expectedPage) {
        Page<GroundReport> ignored = service.listForCitizen(42L, page, size);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(groundReportRepository, org.mockito.Mockito.atLeastOnce())
                .findByReportedBy_IdOrderBySubmittedAtDescIdDesc(eq(42L), captor.capture());
        Pageable last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(ignored).isNotNull();
        assertThat(last.getPageSize()).isEqualTo(expectedSize);
        assertThat(last.getPageNumber()).isEqualTo(expectedPage);
    }

    @Test
    void listForCitizen_unknownCitizen_isNotFound() {
        when(citizenRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.listForCitizen(42L, 0, 20)).isInstanceOf(ResourceNotFoundException.class);
    }

    private GroundReport reportWith(GroundReportStatus status, String photoUrl) {
        GroundReport report = new GroundReport();
        report.setStatus(status);
        report.setPhotoUrl(photoUrl);
        return report;
    }
}
