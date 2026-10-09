package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.AgencyNotification;
import com.group18.dewecs.domain.CoordinationEvent;
import com.group18.dewecs.domain.CoordinationEventType;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.TeamCapability;
import com.group18.dewecs.domain.TeamProfile;
import com.group18.dewecs.domain.TeamStage;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.dto.CoordinationViews.CandidateTeam;
import com.group18.dewecs.dto.CoordinationViews.DispatchOptions;
import com.group18.dewecs.dto.CoordinationViews.EventLine;
import com.group18.dewecs.dto.CoordinationViews.IncidentRow;
import com.group18.dewecs.dto.CoordinationViews.OccupancyResult;
import com.group18.dewecs.dto.CoordinationViews.Picture;
import com.group18.dewecs.dto.CoordinationViews.ShelterRow;
import com.group18.dewecs.dto.CoordinationViews.SyncResult;
import com.group18.dewecs.dto.CoordinationViews.TeamRow;
import com.group18.dewecs.dto.CoordinationViews.TeamUpdateResult;
import com.group18.dewecs.exception.CoordinationValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.AgencyNotificationRepository;
import com.group18.dewecs.repository.CoordinationEventRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.RescueTeamRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.TeamProfileRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.DistrictCoordinationService;
import com.group18.dewecs.service.ReliefDistributionService;
import com.group18.dewecs.service.RescueRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DistrictCoordinationServiceImpl implements DistrictCoordinationService {

    /** A shelter at or above this share of its capacity is flagged. */
    static final int NEAR_PERCENT = 90;
    /** A dispatched team silent for longer than this is shown with its last known status. */
    static final long STALE_MINUTES = 30;

    private final ShelterRepository shelterRepository;
    private final RescueTeamRepository teamRepository;
    private final RescueRequestRepository requestRepository;
    private final RescueRequestService rescueRequestService;
    private final TeamProfileRepository profileRepository;
    private final CoordinationEventRepository eventRepository;
    private final AgencyNotificationRepository notificationRepository;
    private final DistrictRepository districtRepository;
    private final OrganizationRepository organizationRepository;
    private final WarningRepository warningRepository;
    private final ReliefDistributionService distributionService;
    private final Clock clock;

    public DistrictCoordinationServiceImpl(ShelterRepository shelterRepository, RescueTeamRepository teamRepository,
                                           RescueRequestRepository requestRepository,
                                           RescueRequestService rescueRequestService,
                                           TeamProfileRepository profileRepository,
                                           CoordinationEventRepository eventRepository,
                                           AgencyNotificationRepository notificationRepository,
                                           DistrictRepository districtRepository,
                                           OrganizationRepository organizationRepository,
                                           WarningRepository warningRepository,
                                           ReliefDistributionService distributionService, Clock clock) {
        this.shelterRepository = shelterRepository;
        this.teamRepository = teamRepository;
        this.requestRepository = requestRepository;
        this.rescueRequestService = rescueRequestService;
        this.profileRepository = profileRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.districtRepository = districtRepository;
        this.organizationRepository = organizationRepository;
        this.warningRepository = warningRepository;
        this.distributionService = distributionService;
        this.clock = clock;
    }

    // ---------- the combined picture ----------

    @Override
    public Picture picture(Long districtId) {
        District district = district(districtId);
        List<Shelter> shelters = shelterRepository.findByDistrict_Id(districtId);
        List<RescueTeam> teams = teamRepository.findByDistrict_Id(districtId);
        Map<Long, TeamProfile> profiles = profiles(teams);
        List<RescueRequest> open = requestRepository.findByDistrict_IdAndStatusIn(districtId,
                List.of(RescueRequestStatus.PENDING, RescueRequestStatus.ASSIGNED));

        int warnings = warningRepository.findByStatusAndHazardEvent_District_Id(WarningStatus.ISSUED, districtId).size();
        List<ShelterRow> shelterRows = shelters.stream().map(s -> shelterRow(s, shelters)).toList();
        List<TeamRow> teamRows = teams.stream().map(t -> teamRow(t, profiles.get(t.getId()), open)).toList();
        List<IncidentRow> incidentRows = open.stream()
                .sorted(Comparator.comparing((RescueRequest r) -> r.getPriority()).reversed().thenComparing(RescueRequest::getSubmittedAt))
                .map(r -> new IncidentRow(r.getId(), incidentLabel(r), r.getPriority().name(), r.getStatus().name(),
                        r.getAssignedTeam() == null ? null : r.getAssignedTeam().getName(), r.getSubmittedAt())).toList();
        List<EventLine> events = eventRepository.findTop40ByDistrict_IdOrderByCreatedAtDescIdDesc(districtId).stream()
                .map(e -> new EventLine(e.getCreatedAt(), e.getType().name(), e.getSubject(), e.getDetail(),
                        e.getRecordedBy(), e.isNeedsReview())).toList();
        return new Picture(districtId, district.getName(), warnings, shelterRows, teamRows, incidentRows, events);
    }

    // ---------- shelter occupancy ----------

    @Override
    @Transactional
    public OccupancyResult updateOccupancy(Long shelterId, Integer occupancy, Integer expectedOccupancy, String officer,
                                           Long organizationId, LocalDateTime clientTime) {
        Shelter shelter = shelter(shelterId);
        validateOccupancy(shelter, occupancy);
        int before = shelter.getCurrentOccupancy();
        boolean conflict = expectedOccupancy != null && expectedOccupancy != before;
        if (conflict) {
            record(shelter.getDistrict(), CoordinationEventType.SYNC_CONFLICT, shelter.getName(),
                    "Conflicting update: the officer expected " + expectedOccupancy + " but another update set " + before
                            + ". Both attempts are logged; the latest valid value " + occupancy + " is applied.",
                    officer, organizationId, clientTime, true);
        }
        apply(shelter, occupancy, officer, organizationId, clientTime);
        List<Shelter> sameDistrict = shelterRepository.findByDistrict_Id(shelter.getDistrict().getId());
        ShelterRow row = shelterRow(shelter, sameDistrict);
        return new OccupancyResult(shelter.getName(), row.occupancy, row.capacity, row.level, row.alternates, conflict);
    }

    private void validateOccupancy(Shelter shelter, Integer occupancy) {
        if (occupancy == null || occupancy < 0) {
            throw new CoordinationValidationException("Occupancy must be a whole number, zero or more.");
        }
        if (shelter.getStatus() == ShelterStatus.CLOSED) {
            throw new CoordinationValidationException("\"" + shelter.getName() + "\" is closed; reopen it before updating the occupancy.");
        }
        if (occupancy > shelter.getCapacity()) {
            List<String> alternates = alternates(shelter, shelterRepository.findByDistrict_Id(shelter.getDistrict().getId()));
            throw new CoordinationValidationException("\"" + shelter.getName() + "\" holds at most " + shelter.getCapacity()
                    + " people. " + (alternates.isEmpty() ? "No other shelter in the district has room."
                    : "Nearest shelters with room: " + String.join("; ", alternates) + "."));
        }
    }

    private void apply(Shelter shelter, int occupancy, String officer, Long organizationId, LocalDateTime clientTime) {
        int before = shelter.getCurrentOccupancy();
        shelter.setCurrentOccupancy(occupancy);
        if (occupancy >= shelter.getCapacity()) {
            shelter.setStatus(ShelterStatus.FULL);
        } else if (shelter.getStatus() == ShelterStatus.FULL) {
            shelter.setStatus(ShelterStatus.OPEN);
        }
        shelterRepository.save(shelter);
        record(shelter.getDistrict(), CoordinationEventType.OCCUPANCY_UPDATED, shelter.getName(),
                "Occupancy " + before + " to " + occupancy + " of " + shelter.getCapacity(), officer, organizationId,
                clientTime, false);
        int percent = percent(occupancy, shelter.getCapacity());
        if (percent >= NEAR_PERCENT) {
            record(shelter.getDistrict(), CoordinationEventType.SHELTER_FLAGGED, shelter.getName(),
                    percent >= 100 ? "Shelter is full: check-in is blocked." : "Shelter is near capacity (" + percent + "%).",
                    officer, organizationId, clientTime, false);
        }
    }

    // ---------- dispatch ----------

    @Override
    public DispatchOptions dispatchOptions(Long requestId, TeamCapability capability, Long actingOrganizationId) {
        RescueRequest request = request(requestId);
        List<RescueTeam> available = teamRepository.findByStatus(RescueTeamStatus.AVAILABLE);
        Map<Long, TeamProfile> profiles = profiles(available);
        List<CandidateTeam> inDistrict = candidates(request, available, profiles, capability, actingOrganizationId, true);
        List<CandidateTeam> result = inDistrict;
        if (inDistrict.isEmpty()) {
            result = candidates(request, available, profiles, capability, actingOrganizationId, false);
        }
        String location = request.getGpsLat() == null || request.getGpsLng() == null ? "No GPS position"
                : request.getGpsLat() + ", " + request.getGpsLng();
        return new DispatchOptions(request.getId(), request.getDistrict().getId(), request.getDescription(),
                request.getPriority().name(), request.getStatus().name(), location, result, result.isEmpty());
    }

    private List<CandidateTeam> candidates(RescueRequest request, List<RescueTeam> available,
                                           Map<Long, TeamProfile> profiles, TeamCapability capability,
                                           Long actingOrganizationId, boolean sameDistrictOnly) {
        Long districtId = request.getDistrict().getId();
        return available.stream()
                .filter(t -> sameDistrictOnly == t.getDistrict().getId().equals(districtId))
                .filter(t -> capability == null || capabilityOf(profiles.get(t.getId())) == capability)
                .map(t -> new Object[] {t, distanceKm(request, profiles.get(t.getId()))})
                .sorted(Comparator.comparing((Object[] o) -> o[1] == null ? Double.MAX_VALUE : (Double) o[1]))
                .map(o -> {
                    RescueTeam t = (RescueTeam) o[0];
                    Double km = (Double) o[1];
                    return new CandidateTeam(t.getId(), t.getName(), t.getOrganization().getName(),
                            capabilityOf(profiles.get(t.getId())).name(),
                            km == null ? "unknown" : String.format(Locale.ROOT, "%.1f km", km),
                            actingOrganizationId != null && !t.getOrganization().getId().equals(actingOrganizationId),
                            !t.getDistrict().getId().equals(districtId));
                }).toList();
    }

    @Override
    @Transactional
    public RescueRequest dispatch(Long requestId, Long teamId, String notes, Long actingOrganizationId,
                                  boolean confirmCrossOrganization, String officer) {
        RescueRequest request = request(requestId);
        RescueTeam team = team(teamId);
        boolean cross = actingOrganizationId != null && !team.getOrganization().getId().equals(actingOrganizationId);
        if (cross && !confirmCrossOrganization) {
            throw new CoordinationValidationException("\"" + team.getName() + "\" belongs to "
                    + team.getOrganization().getName() + ". Confirm the cross-organization handshake to dispatch it.");
        }
        RescueRequest assigned = rescueRequestService.assign(requestId, teamId);
        TeamProfile profile = profileFor(team);
        profile.setStage(null);
        profile.setSupportNote(null);
        profile.setStatusUpdatedAt(now());
        profileRepository.save(profile);

        String subject = team.getName();
        record(request.getDistrict(), CoordinationEventType.TEAM_DISPATCHED, subject,
                "Dispatched to " + incidentLabel(request) + (notes == null || notes.isBlank() ? "" : ". Notes: " + notes.trim()),
                officer, actingOrganizationId, null, false);
        if (cross) {
            record(request.getDistrict(), CoordinationEventType.CROSS_ORG_CONFIRMED, subject,
                    "Cross-organization handshake confirmed. Responding organization: " + team.getOrganization().getName(),
                    officer, actingOrganizationId, null, false);
        }
        notifyOrganization(team.getOrganization(), "Dispatch order for team " + team.getName() + ": "
                + incidentLabel(request) + (notes == null || notes.isBlank() ? "" : ". " + notes.trim()));
        return assigned;
    }

    @Override
    @Transactional
    public void escalateUnassigned(Long requestId, String officer) {
        RescueRequest request = request(requestId);
        record(request.getDistrict(), CoordinationEventType.UNASSIGNED_ESCALATED, incidentLabel(request),
                "No rescue team available: unassigned, escalated to the DMC.", officer, null, null, false);
    }

    // ---------- team status ----------

    @Override
    @Transactional
    public TeamUpdateResult updateTeamStage(Long teamId, String stage, String note, String officer) {
        RescueTeam team = team(teamId);
        if (team.getStatus() != RescueTeamStatus.DISPATCHED && team.getStatus() != RescueTeamStatus.NEEDS_SUPPORT) {
            throw new CoordinationValidationException("\"" + team.getName() + "\" has no active dispatch to report on.");
        }
        String key = stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
        TeamProfile profile = profileFor(team);
        List<String> backups = List.of();
        switch (key) {
            case "EN_ROUTE", "ON_SITE" -> {
                team.setStatus(RescueTeamStatus.DISPATCHED);
                profile.setStage(TeamStage.valueOf(key));
                profile.setSupportNote(null);
            }
            case "TASK_COMPLETE" -> {
                requestRepository.findFirstByAssignedTeam_IdAndStatusOrderByIdDesc(teamId, RescueRequestStatus.ASSIGNED)
                        .ifPresent(r -> rescueRequestService.complete(r.getId()));
                team.setStatus(RescueTeamStatus.AVAILABLE);
                profile.setStage(TeamStage.TASK_COMPLETE);
                profile.setSupportNote(null);
            }
            case "NEEDS_SUPPORT" -> {
                team.setStatus(RescueTeamStatus.NEEDS_SUPPORT);
                profile.setSupportNote(note == null || note.isBlank() ? "Support requested" : note.trim());
                backups = teamRepository.findByStatus(RescueTeamStatus.AVAILABLE).stream()
                        .filter(t -> t.getDistrict().getId().equals(team.getDistrict().getId()))
                        .map(t -> t.getName() + " (" + t.getOrganization().getName() + ")").toList();
            }
            default -> throw new CoordinationValidationException(
                    "Choose En route, On site, Task complete or Needs support.");
        }
        profile.setStatusUpdatedAt(now());
        teamRepository.save(team);
        profileRepository.save(profile);
        boolean support = key.equals("NEEDS_SUPPORT");
        record(team.getDistrict(), support ? CoordinationEventType.SUPPORT_REQUESTED : CoordinationEventType.TEAM_STATUS,
                team.getName(), label(key) + (note == null || note.isBlank() ? "" : ": " + note.trim())
                        + (support ? ". District officer alerted; backup teams: "
                        + (backups.isEmpty() ? "none available" : String.join(", ", backups)) : ""),
                officer, team.getOrganization().getId(), null, false);
        return new TeamUpdateResult(team.getName(), key, backups);
    }

    @Override
    @Transactional
    public void saveProfile(Long teamId, TeamCapability capability, BigDecimal lat, BigDecimal lng) {
        RescueTeam team = team(teamId);
        if ((lat == null) != (lng == null)) {
            throw new CoordinationValidationException("Enter both latitude and longitude, or neither.");
        }
        if (lat != null && (lat.abs().doubleValue() > 90 || lng.abs().doubleValue() > 180)) {
            throw new CoordinationValidationException("The coordinates are out of range.");
        }
        TeamProfile profile = profileFor(team);
        if (capability != null) {
            profile.setCapability(capability);
        }
        profile.setBaseLat(lat);
        profile.setBaseLng(lng);
        profileRepository.save(profile);
    }

    // ---------- supplies ----------

    @Override
    @Transactional
    public void logSupply(Long shelterId, Long resourceId, Integer quantity, String officer, Long organizationId) {
        Shelter shelter = shelter(shelterId);
        if (quantity == null || quantity < 1) {
            throw new CoordinationValidationException("The supply quantity must be a whole number, at least 1.");
        }
        var consignment = distributionService.create(resourceId, shelterId, quantity);
        record(shelter.getDistrict(), CoordinationEventType.SUPPLY_LOGGED, shelter.getName(),
                quantity + " " + consignment.getItems().get(0).getResource().getName() + " logged against "
                        + consignment.getOrganization().getName() + " stock (" + consignment.reference() + ")",
                officer, organizationId, null, false);
    }

    // ---------- offline sync ----------

    @Override
    @Transactional
    public SyncResult sync(List<QueuedOccupancy> actions, String officer) {
        int applied = 0;
        int conflicts = 0;
        int rejected = 0;
        List<QueuedOccupancy> ordered = actions.stream()
                .sorted(Comparator.comparing(a -> a.clientTime() == null ? LocalDateTime.MIN : a.clientTime())).toList();
        for (QueuedOccupancy a : ordered) {
            Shelter shelter = shelterRepository.findById(a.shelterId()).orElse(null);
            if (shelter == null) {
                rejected++;
                continue;
            }
            try {
                validateOccupancy(shelter, a.occupancy());
            } catch (CoordinationValidationException ex) {
                rejected++;
                record(shelter.getDistrict(), CoordinationEventType.SYNC_CONFLICT, shelter.getName(),
                        "Queued update rejected: " + ex.getMessage(), officer, null, a.clientTime(), true);
                continue;
            }
            var latest = eventRepository.findFirstBySubjectAndTypeOrderByCreatedAtDescIdDesc(shelter.getName(),
                    CoordinationEventType.OCCUPANCY_UPDATED);
            if (a.clientTime() != null && latest.isPresent() && latest.get().getCreatedAt().isAfter(a.clientTime())) {
                conflicts++;
                record(shelter.getDistrict(), CoordinationEventType.SYNC_CONFLICT, shelter.getName(),
                        "Queued update (" + a.occupancy() + ", made " + a.clientTime() + ") is older than the latest update; "
                                + "the later value is kept. Flagged for manual review.", officer, null, a.clientTime(), true);
                continue;
            }
            apply(shelter, a.occupancy(), officer, null, a.clientTime());
            applied++;
        }
        return new SyncResult(applied, conflicts, rejected);
    }

    // ---------- reference data ----------

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll().stream().sorted(Comparator.comparing(District::getName)).toList();
    }

    @Override
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll().stream().sorted(Comparator.comparing(Organization::getName)).toList();
    }

    @Override
    public List<Resource> listSupplies() {
        return distributionService.listSuppliesForSelection();
    }

    // ---------- helpers ----------

    private ShelterRow shelterRow(Shelter s, List<Shelter> sameDistrict) {
        int occupancy = s.getCurrentOccupancy() == null ? 0 : s.getCurrentOccupancy();
        int percent = percent(occupancy, s.getCapacity());
        String level = s.getStatus() == ShelterStatus.CLOSED ? "CLOSED" : percent >= 100 ? "FULL" : percent >= NEAR_PERCENT ? "NEAR" : "OK";
        List<String> alternates = level.equals("NEAR") || level.equals("FULL") ? alternates(s, sameDistrict) : List.of();
        return new ShelterRow(s.getId(), s.getName(), s.getOrganization() == null ? null : s.getOrganization().getName(),
                occupancy, s.getCapacity(), Math.min(percent, 100), level, alternates);
    }

    /** The shelters of the district with room, the roomiest first (shelters have no coordinates to rank by distance). */
    private List<String> alternates(Shelter shelter, List<Shelter> sameDistrict) {
        return sameDistrict.stream()
                .filter(o -> !o.getId().equals(shelter.getId()) && o.getStatus() != ShelterStatus.CLOSED
                        && o.getCurrentOccupancy() < o.getCapacity())
                .sorted(Comparator.comparingInt((Shelter o) -> o.getCapacity() - o.getCurrentOccupancy()).reversed())
                .limit(3)
                .map(o -> o.getName() + " (" + (o.getCapacity() - o.getCurrentOccupancy()) + " free)").toList();
    }

    private TeamRow teamRow(RescueTeam t, TeamProfile p, List<RescueRequest> open) {
        String incident = open.stream().filter(r -> r.getAssignedTeam() != null && r.getAssignedTeam().getId().equals(t.getId()))
                .findFirst().map(this::incidentLabel).orElse(null);
        LocalDateTime updated = p == null ? null : p.getStatusUpdatedAt();
        boolean active = t.getStatus() == RescueTeamStatus.DISPATCHED || t.getStatus() == RescueTeamStatus.NEEDS_SUPPORT;
        boolean stale = active && updated != null && Duration.between(updated, now()).toMinutes() > STALE_MINUTES;
        return new TeamRow(t.getId(), t.getName(), t.getOrganization().getName(), t.getStatus().name(),
                p == null || p.getStage() == null ? null : p.getStage().name(), capabilityOf(p).name(), updated, stale,
                p == null ? null : p.getSupportNote(), incident,
                p == null || p.getBaseLat() == null ? "" : p.getBaseLat().toPlainString(),
                p == null || p.getBaseLng() == null ? "" : p.getBaseLng().toPlainString());
    }

    private String incidentLabel(RescueRequest r) {
        String d = r.getDescription() == null ? "" : r.getDescription();
        return "Request #" + r.getId() + (d.isBlank() ? "" : ": " + (d.length() > 70 ? d.substring(0, 70) + "..." : d));
    }

    private TeamCapability capabilityOf(TeamProfile p) {
        return p == null || p.getCapability() == null ? TeamCapability.GENERAL : p.getCapability();
    }

    private Double distanceKm(RescueRequest r, TeamProfile p) {
        if (p == null || p.getBaseLat() == null || p.getBaseLng() == null || r.getGpsLat() == null || r.getGpsLng() == null) {
            return null;
        }
        double lat1 = Math.toRadians(r.getGpsLat().doubleValue());
        double lat2 = Math.toRadians(p.getBaseLat().doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(p.getBaseLng().doubleValue() - r.getGpsLng().doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.asin(Math.sqrt(h));
    }

    private Map<Long, TeamProfile> profiles(List<RescueTeam> teams) {
        if (teams.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByTeam_IdIn(teams.stream().map(RescueTeam::getId).toList()).stream()
                .collect(Collectors.toMap(p -> p.getTeam().getId(), Function.identity(), (a, b) -> a));
    }

    private TeamProfile profileFor(RescueTeam team) {
        return profileRepository.findByTeam_Id(team.getId()).orElseGet(() -> {
            TeamProfile p = new TeamProfile();
            p.setTeam(team);
            p.setCapability(TeamCapability.GENERAL);
            return p;
        });
    }

    private void notifyOrganization(Organization org, String message) {
        AgencyNotification n = new AgencyNotification();
        n.setOrganization(org);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        n.setCreatedAt(now());
        notificationRepository.save(n);
    }

    private void record(District district, CoordinationEventType type, String subject, String detail, String officer,
                        Long organizationId, LocalDateTime clientTime, boolean needsReview) {
        CoordinationEvent e = new CoordinationEvent();
        e.setDistrict(district);
        e.setType(type);
        e.setSubject(subject);
        e.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        e.setRecordedBy(officer == null || officer.isBlank() ? "District officer" : officer.trim());
        e.setOrganization(organizationId == null ? null : organizationRepository.findById(organizationId).orElse(null));
        e.setClientTime(clientTime);
        e.setNeedsReview(needsReview);
        e.setCreatedAt(now());
        eventRepository.save(e);
    }

    private String label(String key) {
        return switch (key) {
            case "EN_ROUTE" -> "En route";
            case "ON_SITE" -> "On site";
            case "TASK_COMPLETE" -> "Task complete";
            default -> "Needs support";
        };
    }

    private int percent(int value, Integer capacity) {
        return capacity == null || capacity <= 0 ? 0 : (int) Math.round(value * 100.0 / capacity);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private District district(Long id) {
        return districtRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));
    }

    private Shelter shelter(Long id) {
        return shelterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }

    private RescueTeam team(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rescue team not found: " + id));
    }

    private RescueRequest request(Long id) {
        return requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rescue request not found: " + id));
    }
}
