package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.service.GroundReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GroundReportServiceImpl implements GroundReportService {

    private final GroundReportRepository groundReportRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;

    public GroundReportServiceImpl(GroundReportRepository groundReportRepository,
                                    DistrictRepository districtRepository,
                                    UserRepository userRepository) {
        this.groundReportRepository = groundReportRepository;
        this.districtRepository = districtRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public GroundReport markReviewed(Long reportId, Long reviewingUserId) {
        GroundReport report = findReport(reportId);
        if (report.getStatus() != GroundReportStatus.PENDING_REVIEW) {
            throw new GroundReportValidationException("Only unreviewed reports can be marked reviewed.");
        }
        report.setStatus(GroundReportStatus.VERIFIED);
        report.setVerifiedBy(findUser(reviewingUserId));
        return groundReportRepository.save(report);
    }

    @Override
    @Transactional
    public GroundReport action(Long reportId, Long reviewingUserId, String note) {
        GroundReport report = findReport(reportId);
        if (report.getStatus() != GroundReportStatus.VERIFIED) {
            throw new GroundReportValidationException("Only reviewed reports can be actioned.");
        }
        if (note == null || note.isBlank()) {
            throw new GroundReportValidationException("An action note is required.");
        }
        report.setStatus(GroundReportStatus.ACTIONED);
        report.setActionNote(note);
        report.setVerifiedBy(findUser(reviewingUserId));
        return groundReportRepository.save(report);
    }

    @Override
    @Transactional
    public GroundReport dismiss(Long reportId, Long reviewingUserId) {
        GroundReport report = findReport(reportId);
        if (report.getStatus() == GroundReportStatus.ACTIONED || report.getStatus() == GroundReportStatus.REJECTED) {
            throw new GroundReportValidationException("This report has already been actioned or dismissed.");
        }
        report.setStatus(GroundReportStatus.REJECTED);
        report.setVerifiedBy(findUser(reviewingUserId));
        return groundReportRepository.save(report);
    }

    @Override
    public GroundReport getById(Long reportId) {
        return findReport(reportId);
    }

    @Override
    public List<GroundReport> list(GroundReportStatus statusFilter, Long districtId, HazardType categoryFilter) {
        return groundReportRepository.search(statusFilter, districtId, categoryFilter);
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<User> listUsersForSelection() {
        return userRepository.findAll();
    }

    private GroundReport findReport(Long id) {
        return groundReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ground report not found: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
