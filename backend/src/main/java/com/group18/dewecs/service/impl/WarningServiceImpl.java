package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.exception.WarningValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.WarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WarningServiceImpl implements WarningService {

    private final WarningRepository warningRepository;
    private final HazardEventRepository hazardEventRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;

    public WarningServiceImpl(WarningRepository warningRepository,
                               HazardEventRepository hazardEventRepository,
                               UserRepository userRepository,
                               DistrictRepository districtRepository) {
        this.warningRepository = warningRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
    }

    @Override
    @Transactional
    public Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels, Long issuedByUserId) {
        HazardEvent hazardEvent = findHazardEvent(hazardEventId);
        User issuedBy = findUser(issuedByUserId);

        Warning warning = new Warning();
        warning.setHazardEvent(hazardEvent);
        warning.setSeverity(parseSeverity(severity));
        warning.setMessage(message);
        warning.setExpiresAt(expiresAt);
        warning.setBroadcastChannels(parseChannels(broadcastChannels));
        warning.setIssuedBy(issuedBy);
        warning.setStatus(WarningStatus.DRAFT);
        warning.setIssuedAt(null);

        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels) {
        Warning warning = findWarning(warningId);
        requireDraft(warning);

        warning.setSeverity(parseSeverity(severity));
        warning.setMessage(message);
        warning.setExpiresAt(expiresAt);
        warning.setBroadcastChannels(parseChannels(broadcastChannels));

        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning publish(Long warningId) {
        Warning warning = findWarning(warningId);
        requireDraft(warning);

        if (warning.getHazardEvent().getDistrict() == null) {
            throw new WarningValidationException("A warning cannot be published without an affected region.");
        }
        if (warning.getMessage() == null || warning.getMessage().isBlank()) {
            throw new WarningValidationException("A warning cannot be published without a message.");
        }

        warning.setStatus(WarningStatus.ISSUED);
        warning.setIssuedAt(LocalDateTime.now());
        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning retract(Long warningId) {
        Warning warning = findWarning(warningId);
        if (warning.getStatus() != WarningStatus.DRAFT && warning.getStatus() != WarningStatus.ISSUED) {
            throw new WarningValidationException("Only draft or published warnings can be retracted.");
        }
        warning.setStatus(WarningStatus.CANCELLED);
        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning getById(Long warningId) {
        expireOverdue();
        return findWarning(warningId);
    }

    @Override
    @Transactional
    public List<Warning> list(WarningStatus statusFilter, Long districtId) {
        expireOverdue();

        if (statusFilter != null && districtId != null) {
            return warningRepository.findByStatusAndHazardEvent_District_Id(statusFilter, districtId);
        }
        if (statusFilter != null) {
            return warningRepository.findByStatus(statusFilter);
        }
        if (districtId != null) {
            return warningRepository.findByHazardEvent_District_Id(districtId);
        }
        return warningRepository.findAll();
    }

    @Override
    public List<HazardEvent> listHazardEventsForSelection() {
        return hazardEventRepository.findAll();
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<User> listUsersForSelection() {
        return userRepository.findAll();
    }

    private void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Warning> overdue = warningRepository.findByStatus(WarningStatus.ISSUED).stream()
                .filter(w -> w.getExpiresAt() != null && !w.getExpiresAt().isAfter(now))
                .toList();
        if (!overdue.isEmpty()) {
            overdue.forEach(w -> w.setStatus(WarningStatus.EXPIRED));
            warningRepository.saveAll(overdue);
        }
    }

    private void requireDraft(Warning warning) {
        if (warning.getStatus() != WarningStatus.DRAFT) {
            throw new WarningValidationException("Only draft warnings can be edited or published.");
        }
    }

    private Warning findWarning(Long id) {
        return warningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warning not found: " + id));
    }

    private HazardEvent findHazardEvent(Long id) {
        return hazardEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event not found: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new WarningValidationException("Invalid severity: " + raw);
        }
    }

    private Set<BroadcastChannel> parseChannels(Set<String> raw) {
        if (raw == null) {
            return new HashSet<>();
        }
        return raw.stream()
                .map(value -> {
                    try {
                        return BroadcastChannel.valueOf(value.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        throw new WarningValidationException("Invalid broadcast channel: " + value);
                    }
                })
                .collect(Collectors.toSet());
    }
}
