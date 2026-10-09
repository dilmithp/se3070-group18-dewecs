package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
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
import com.group18.dewecs.service.AlertBroadcastService;
import com.group18.dewecs.service.WarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WarningServiceImpl implements WarningService {

    private final WarningRepository warningRepository;
    private final HazardEventRepository hazardEventRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final RiverBasinRepository riverBasinRepository;
    private final AlertBroadcastService alertBroadcastService;

    public WarningServiceImpl(WarningRepository warningRepository,
                               HazardEventRepository hazardEventRepository,
                               UserRepository userRepository,
                               DistrictRepository districtRepository,
                               RiverBasinRepository riverBasinRepository,
                               AlertBroadcastService alertBroadcastService) {
        this.warningRepository = warningRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.riverBasinRepository = riverBasinRepository;
        this.alertBroadcastService = alertBroadcastService;
    }

    @Override
    @Transactional
    public Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels, Long issuedByUserId) {
        return createDraft(hazardEventId, severity, message, expiresAt, broadcastChannels, issuedByUserId, null);
    }

    @Override
    @Transactional
    public Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels, Long issuedByUserId, Long riverBasinId) {
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
        if (riverBasinId != null) {
            warning.setAffectedDistricts(districtsOfBasin(hazardEvent, findRiverBasin(riverBasinId)));
        }

        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels) {
        return updateDraft(warningId, severity, message, expiresAt, broadcastChannels, null);
    }

    @Override
    @Transactional
    public Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                                Set<String> broadcastChannels, Long riverBasinId) {
        Warning warning = findWarning(warningId);
        requireDraft(warning);

        warning.setSeverity(parseSeverity(severity));
        warning.setMessage(message);
        warning.setExpiresAt(expiresAt);
        warning.setBroadcastChannels(parseChannels(broadcastChannels));
        if (riverBasinId != null) {
            warning.setAffectedDistricts(districtsOfBasin(warning.getHazardEvent(), findRiverBasin(riverBasinId)));
        }

        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning publish(Long warningId) {
        Warning warning = findWarning(warningId);
        requireDraft(warning);

        if (warning.effectiveDistricts().isEmpty()) {
            throw new WarningValidationException("A warning cannot be published without an affected region.");
        }
        if (warning.getMessage() == null || warning.getMessage().isBlank()) {
            throw new WarningValidationException("A warning cannot be published without a message.");
        }

        warning.setStatus(WarningStatus.ISSUED);
        warning.setIssuedAt(LocalDateTime.now());
        Warning saved = warningRepository.save(warning);
        alertBroadcastService.broadcast(saved, DeliveryTrigger.PUBLISH, null);
        return saved;
    }

    @Override
    @Transactional
    public Warning retract(Long warningId) {
        Warning warning = findWarning(warningId);
        if (warning.getStatus() != WarningStatus.DRAFT && !isPublished(warning)) {
            throw new WarningValidationException("Only draft or published warnings can be retracted.");
        }
        warning.setStatus(WarningStatus.CANCELLED);
        return warningRepository.save(warning);
    }

    @Override
    @Transactional
    public Warning escalate(Long warningId, String newSeverity, String note, LocalDateTime newExpiresAt) {
        expireOverdue();
        Warning warning = findWarning(warningId);
        if (!isPublished(warning)) {
            throw new WarningValidationException("Only published warnings can be escalated.");
        }
        Severity target = parseSeverity(newSeverity);
        Severity current = warning.getSeverity();
        if (target.ordinal() <= current.ordinal()) {
            throw new WarningValidationException(
                    "Escalation must raise the severity (it is " + current.name() + " now).");
        }
        if (newExpiresAt != null && !newExpiresAt.isAfter(LocalDateTime.now())) {
            throw new WarningValidationException("The new expiry must be in the future.");
        }

        String change = "Escalated from " + current.name() + " to " + target.name() + ".";
        warning.setSeverity(target);
        warning.setStatus(WarningStatus.UPDATED);
        if (note != null && !note.isBlank()) {
            String base = warning.getMessage() == null ? "" : warning.getMessage() + "\n\n";
            warning.setMessage(base + "[Escalated to " + target.name() + "] " + note.trim());
        }
        if (newExpiresAt != null) {
            warning.setExpiresAt(newExpiresAt);
        }
        Warning saved = warningRepository.save(warning);
        alertBroadcastService.broadcast(saved, DeliveryTrigger.ESCALATION, change);
        return saved;
    }

    @Override
    @Transactional
    public List<AlertDeliveryLog> rebroadcast(Long warningId) {
        Warning warning = findWarning(warningId);
        if (!isPublished(warning)) {
            throw new WarningValidationException("Only published warnings can be sent again.");
        }
        return alertBroadcastService.retryFailed(warning);
    }

    @Override
    public List<AlertDeliveryLog> listDeliveryLog(Long warningId) {
        findWarning(warningId);
        return alertBroadcastService.listLog(warningId);
    }

    @Override
    public boolean allChannelsDelivered(Long warningId) {
        return alertBroadcastService.allChannelsCovered(findWarning(warningId));
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

        List<Warning> base;
        if (statusFilter != null && districtId != null) {
            base = warningRepository.findByStatusAndHazardEvent_District_Id(statusFilter, districtId);
        } else if (statusFilter != null) {
            base = warningRepository.findByStatus(statusFilter);
        } else if (districtId != null) {
            base = warningRepository.findByHazardEvent_District_Id(districtId);
        } else {
            base = warningRepository.findAll();
        }
        if (districtId == null) {
            return base;
        }
        // A river-basin warning also covers districts other than the one of its hazard event.
        Map<Long, Warning> merged = new LinkedHashMap<>();
        base.forEach(w -> merged.put(w.getId(), w));
        for (Warning w : warningRepository.findByAffectedDistricts_Id(districtId)) {
            if (statusFilter == null || w.getStatus() == statusFilter) {
                merged.putIfAbsent(w.getId(), w);
            }
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    @Transactional
    public List<Warning> listActive() {
        expireOverdue();
        List<Warning> live = new ArrayList<>(warningRepository.findByStatus(WarningStatus.ISSUED));
        live.addAll(warningRepository.findByStatus(WarningStatus.UPDATED));
        live.sort(java.util.Comparator.comparing(Warning::getIssuedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return live;
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

    @Override
    public List<RiverBasin> listRiverBasins() {
        return riverBasinRepository.findAllByOrderByNameAsc();
    }

    private void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Warning> open = new ArrayList<>(warningRepository.findByStatus(WarningStatus.ISSUED));
        open.addAll(warningRepository.findByStatus(WarningStatus.UPDATED));
        List<Warning> overdue = open.stream()
                .filter(w -> w.getExpiresAt() != null && !w.getExpiresAt().isAfter(now))
                .toList();
        if (!overdue.isEmpty()) {
            overdue.forEach(w -> w.setStatus(WarningStatus.EXPIRED));
            warningRepository.saveAll(overdue);
        }
    }

    /** ISSUED, or UPDATED after an escalation: the warning is live. */
    private boolean isPublished(Warning warning) {
        return warning.getStatus() == WarningStatus.ISSUED || warning.getStatus() == WarningStatus.UPDATED;
    }

    private void requireDraft(Warning warning) {
        if (warning.getStatus() != WarningStatus.DRAFT) {
            throw new WarningValidationException("Only draft warnings can be edited or published.");
        }
    }

    /** The district of the hazard event plus every district of the basin, each once. */
    private Set<District> districtsOfBasin(HazardEvent event, RiverBasin basin) {
        Map<Long, District> byId = new LinkedHashMap<>();
        if (event.getDistrict() != null) {
            byId.put(event.getDistrict().getId(), event.getDistrict());
        }
        basin.getDistricts().forEach(d -> byId.putIfAbsent(d.getId(), d));
        return new HashSet<>(byId.values());
    }

    private Warning findWarning(Long id) {
        return warningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warning not found: " + id));
    }

    private HazardEvent findHazardEvent(Long id) {
        return hazardEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event not found: " + id));
    }

    private RiverBasin findRiverBasin(Long id) {
        return riverBasinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("River basin not found: " + id));
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
