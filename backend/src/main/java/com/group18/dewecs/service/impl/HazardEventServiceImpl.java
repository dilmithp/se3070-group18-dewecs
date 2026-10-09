package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.HazardEventValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.HazardEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class HazardEventServiceImpl implements HazardEventService {

    /** Field evidence from this long before the event began is still relevant to it. */
    private static final Duration EVIDENCE_LEAD = Duration.ofHours(24);
    private static final Duration CLOCK_SLACK = Duration.ofMinutes(5);
    private static final Set<GroundReportStatus> EVIDENCE =
            EnumSet.of(GroundReportStatus.VERIFIED, GroundReportStatus.ACTIONED);
    private static final Set<WarningStatus> LIVE = EnumSet.of(WarningStatus.ISSUED, WarningStatus.UPDATED);

    private final HazardEventRepository hazardEventRepository;
    private final DistrictRepository districtRepository;
    private final GroundReportRepository groundReportRepository;
    private final WarningRepository warningRepository;
    private final Clock clock;

    public HazardEventServiceImpl(HazardEventRepository hazardEventRepository,
                                  DistrictRepository districtRepository,
                                  GroundReportRepository groundReportRepository,
                                  WarningRepository warningRepository,
                                  Clock clock) {
        this.hazardEventRepository = hazardEventRepository;
        this.districtRepository = districtRepository;
        this.groundReportRepository = groundReportRepository;
        this.warningRepository = warningRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HazardEvent create(String hazardType, String severity, Long districtId, LocalDateTime occurredAt) {
        HazardType type = parse(HazardType.class, hazardType, "hazard type");
        Severity level = parse(Severity.class, severity, "severity");
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId));
        LocalDateTime now = LocalDateTime.now(clock);
        if (occurredAt != null && occurredAt.isAfter(now.plus(CLOCK_SLACK))) {
            throw new HazardEventValidationException("The start time of the event is in the future.");
        }

        HazardEvent event = new HazardEvent();
        event.setHazardType(type);
        event.setSeverityLevel(level);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(district);
        event.setOccurredAt(occurredAt != null ? occurredAt : now);
        return hazardEventRepository.save(event);
    }

    @Override
    @Transactional
    public HazardEvent updateStatus(Long eventId, String status) {
        HazardEvent event = getById(eventId);
        HazardEventStatus target = parse(HazardEventStatus.class, status, "status");
        if (target.ordinal() <= event.getStatus().ordinal()) {
            throw new HazardEventValidationException(
                    "An event cannot go back from " + event.getStatus().name() + " to " + target.name() + ".");
        }
        event.setStatus(target);
        return hazardEventRepository.save(event);
    }

    @Override
    public HazardEvent getById(Long eventId) {
        return hazardEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event not found: " + eventId));
    }

    @Override
    public List<HazardEvent> list() {
        return hazardEventRepository.findAllByOrderByOccurredAtDescIdDesc();
    }

    @Override
    public List<HazardSummary> listActiveSummaries() {
        return hazardEventRepository.findByStatusOrderByOccurredAtDescIdDesc(HazardEventStatus.ACTIVE).stream()
                .map(event -> new HazardSummary(event, evidenceFor(event).size(), liveWarningsOf(event)))
                .toList();
    }

    @Override
    public HazardReview review(Long eventId) {
        HazardEvent event = getById(eventId);
        List<Warning> warnings = warningRepository.findByHazardEvent_IdAndStatusIn(eventId,
                EnumSet.allOf(WarningStatus.class));
        return new HazardReview(event, evidenceFor(event), warnings);
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    private List<GroundReport> evidenceFor(HazardEvent event) {
        LocalDateTime from = event.getOccurredAt().minus(EVIDENCE_LEAD);
        return groundReportRepository
                .findByDistrict_IdAndStatusInOrderBySubmittedAtDescIdDesc(event.getDistrict().getId(), EVIDENCE).stream()
                .filter(r -> r.getSubmittedAt() == null || !r.getSubmittedAt().isBefore(from))
                .toList();
    }

    private int liveWarningsOf(HazardEvent event) {
        return warningRepository.findByHazardEvent_IdAndStatusIn(event.getId(), LIVE).size();
    }

    private <E extends Enum<E>> E parse(Class<E> type, String raw, String what) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new HazardEventValidationException("Invalid " + what + ": " + raw);
        }
    }
}
