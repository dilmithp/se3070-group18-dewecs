package com.group18.dewecs.service.impl;

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
import com.group18.dewecs.service.GroundReportSubmissionService;
import com.group18.dewecs.service.PhotoStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Replay detection is check-then-insert (citizen + category + capturedAt), not a database constraint, because no
 * schema change is allowed. Two overlapping retries could therefore insert two rows; the lookup returns the oldest
 * match so later retries still succeed.
 *
 * attachPhoto is deliberately not one transaction: the new file is written first, the row update commits on its own,
 * and only then is the old file deleted, so a failed update never loses the existing photo.
 */
@Service
public class GroundReportSubmissionServiceImpl implements GroundReportSubmissionService {

    static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int COORDINATE_SCALE = 7;
    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");
    private static final Set<GroundReportStatus> PHOTO_ALLOWED = Set.of(
            GroundReportStatus.PENDING_SYNC, GroundReportStatus.PENDING_REVIEW, GroundReportStatus.NEEDS_INFO);

    private final GroundReportRepository groundReportRepository;
    private final CitizenRepository citizenRepository;
    private final DistrictRepository districtRepository;
    private final PhotoStorageService photoStorage;
    private final Clock clock;

    public GroundReportSubmissionServiceImpl(GroundReportRepository groundReportRepository,
                                             CitizenRepository citizenRepository,
                                             DistrictRepository districtRepository,
                                             PhotoStorageService photoStorage,
                                             Clock clock) {
        this.groundReportRepository = groundReportRepository;
        this.citizenRepository = citizenRepository;
        this.districtRepository = districtRepository;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmissionResult submit(Long citizenId, Long districtId, String category, String description,
                                   BigDecimal gpsLat, BigDecimal gpsLng, LocalDateTime capturedAt) {
        if (citizenId == null || districtId == null) {
            throw new GroundReportValidationException("Citizen and district are required.");
        }
        HazardType type = parseCategory(category);
        String text = cleanDescription(description);
        BigDecimal lat = roundCoordinate(gpsLat, MAX_LAT, "Latitude");
        BigDecimal lng = roundCoordinate(gpsLng, MAX_LNG, "Longitude");

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime submittedAt = capturedAt != null ? capturedAt.truncatedTo(ChronoUnit.MILLIS) : now;
        if (capturedAt != null && submittedAt.isAfter(now.plusMinutes(5))) {
            throw new GroundReportValidationException("The capture time is in the future.");
        }

        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + citizenId));
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId));

        if (capturedAt != null) {
            Optional<GroundReport> replay = groundReportRepository
                    .findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(citizenId, type, submittedAt);
            if (replay.isPresent()) {
                return new SubmissionResult(replay.get(), false);
            }
        }

        GroundReport report = new GroundReport();
        report.setReportedBy(citizen);
        report.setDistrict(district);
        report.setCategory(type);
        report.setDescription(text);
        report.setGpsLat(lat);
        report.setGpsLng(lng);
        report.setStatus(GroundReportStatus.PENDING_REVIEW);
        report.setSubmittedAt(submittedAt);
        return new SubmissionResult(groundReportRepository.save(report), true);
    }

    @Override
    public GroundReport attachPhoto(Long reportId, byte[] content) {
        GroundReport report = groundReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Ground report not found: " + reportId));
        if (!PHOTO_ALLOWED.contains(report.getStatus())) {
            throw new GroundReportValidationException(
                    "A photo can only be added while the report is waiting for review or needs information.");
        }

        String newName = photoStorage.store(content);
        String oldUrl = report.getPhotoUrl();
        GroundReport saved;
        try {
            report.setPhotoUrl(PhotoStorageService.URL_PREFIX + newName);
            saved = groundReportRepository.save(report);
        } catch (RuntimeException e) {
            photoStorage.delete(newName);
            throw e;
        }
        if (oldUrl != null && oldUrl.startsWith(PhotoStorageService.URL_PREFIX)) {
            photoStorage.delete(oldUrl.substring(PhotoStorageService.URL_PREFIX.length()));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroundReport> listForCitizen(Long citizenId, int page, int size) {
        if (!citizenRepository.existsById(citizenId)) {
            throw new ResourceNotFoundException("Citizen not found: " + citizenId);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return groundReportRepository.findByReportedBy_IdOrderBySubmittedAtDescIdDesc(
                citizenId, PageRequest.of(safePage, safeSize));
    }

    private HazardType parseCategory(String raw) {
        try {
            return HazardType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new GroundReportValidationException("Invalid category: " + raw);
        }
    }

    private String cleanDescription(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || text.length() > MAX_DESCRIPTION_LENGTH) {
            throw new GroundReportValidationException(
                    "Description must be 1 to " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        return text;
    }

    private BigDecimal roundCoordinate(BigDecimal value, BigDecimal limit, String label) {
        if (value == null || value.abs().compareTo(limit) > 0) {
            throw new GroundReportValidationException(label + " must be between -" + limit + " and " + limit + ".");
        }
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }
}
