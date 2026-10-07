package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.exception.RescueRequestValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.RescueTeamRepository;
import com.group18.dewecs.service.RescueRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RescueRequestServiceImpl implements RescueRequestService {

    private final RescueRequestRepository rescueRequestRepository;
    private final RescueTeamRepository rescueTeamRepository;
    private final DistrictRepository districtRepository;

    public RescueRequestServiceImpl(RescueRequestRepository rescueRequestRepository,
                                     RescueTeamRepository rescueTeamRepository,
                                     DistrictRepository districtRepository) {
        this.rescueRequestRepository = rescueRequestRepository;
        this.rescueTeamRepository = rescueTeamRepository;
        this.districtRepository = districtRepository;
    }

    @Override
    @Transactional
    public RescueRequest submit(Long districtId, String requesterName, String requesterPhone, BigDecimal gpsLat,
                                 BigDecimal gpsLng, String description, String priority) {
        District district = findDistrict(districtId);

        RescueRequest request = new RescueRequest();
        request.setDistrict(district);
        request.setRequesterName(requesterName);
        request.setRequesterPhone(requesterPhone);
        request.setGpsLat(gpsLat);
        request.setGpsLng(gpsLng);
        request.setDescription(description);
        request.setPriority(parsePriority(priority));
        request.setStatus(RescueRequestStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());

        return rescueRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RescueRequest assign(Long requestId, Long teamId) {
        RescueRequest request = findRequest(requestId);
        if (request.getStatus() != RescueRequestStatus.PENDING) {
            throw new RescueRequestValidationException("Only pending requests can be assigned.");
        }

        RescueTeam team = rescueTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue team not found: " + teamId));
        if (team.getStatus() != RescueTeamStatus.AVAILABLE) {
            throw new RescueRequestValidationException("Team is not available for assignment.");
        }

        team.setStatus(RescueTeamStatus.DISPATCHED);
        rescueTeamRepository.save(team);

        request.setAssignedTeam(team);
        request.setStatus(RescueRequestStatus.ASSIGNED);
        request.setAssignedAt(LocalDateTime.now());
        return rescueRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RescueRequest complete(Long requestId) {
        RescueRequest request = findRequest(requestId);
        if (request.getStatus() != RescueRequestStatus.ASSIGNED) {
            throw new RescueRequestValidationException("Only assigned requests can be completed.");
        }

        freeUpAssignedTeam(request);

        request.setStatus(RescueRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        return rescueRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RescueRequest cancel(Long requestId) {
        RescueRequest request = findRequest(requestId);
        if (request.getStatus() != RescueRequestStatus.PENDING && request.getStatus() != RescueRequestStatus.ASSIGNED) {
            throw new RescueRequestValidationException("Only pending or assigned requests can be cancelled.");
        }

        if (request.getStatus() == RescueRequestStatus.ASSIGNED) {
            freeUpAssignedTeam(request);
        }

        request.setStatus(RescueRequestStatus.CANCELLED);
        return rescueRequestRepository.save(request);
    }

    @Override
    public RescueRequest getById(Long requestId) {
        return findRequest(requestId);
    }

    @Override
    public List<RescueRequest> list(RescueRequestStatus statusFilter, Severity priorityFilter, Long districtId) {
        return rescueRequestRepository.search(statusFilter, priorityFilter, districtId);
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<RescueTeam> listTeamsForSelection() {
        return rescueTeamRepository.findByStatus(RescueTeamStatus.AVAILABLE);
    }

    private void freeUpAssignedTeam(RescueRequest request) {
        RescueTeam team = request.getAssignedTeam();
        if (team != null) {
            team.setStatus(RescueTeamStatus.AVAILABLE);
            rescueTeamRepository.save(team);
        }
    }

    private RescueRequest findRequest(Long id) {
        return rescueRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue request not found: " + id));
    }

    private District findDistrict(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));
    }

    private Severity parsePriority(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new RescueRequestValidationException("Invalid priority: " + raw);
        }
    }
}
