package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.AgencyNotification;
import com.group18.dewecs.domain.ConsignmentDetails;
import com.group18.dewecs.domain.ConsignmentEvent;
import com.group18.dewecs.domain.ConsignmentEventType;
import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.NeedStatus;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterNeed;
import com.group18.dewecs.dto.LogisticsViews.CommandCenterView;
import com.group18.dewecs.dto.LogisticsViews.ConsignmentLine;
import com.group18.dewecs.dto.LogisticsViews.DetailsView;
import com.group18.dewecs.dto.LogisticsViews.EventLine;
import com.group18.dewecs.dto.LogisticsViews.NeedLine;
import com.group18.dewecs.dto.LogisticsViews.NotificationLine;
import com.group18.dewecs.dto.LogisticsViews.SplitLine;
import com.group18.dewecs.dto.LogisticsViews.StockLine;
import com.group18.dewecs.dto.ShelterResponse;
import com.group18.dewecs.exception.GroundReportValidationException;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.mapper.ShelterMapper;
import com.group18.dewecs.repository.AgencyNotificationRepository;
import com.group18.dewecs.repository.ConsignmentDetailsRepository;
import com.group18.dewecs.repository.ConsignmentEventRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterNeedRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.PhotoStorageService;
import com.group18.dewecs.service.ReliefDistributionService;
import com.group18.dewecs.service.ReliefLogisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReliefLogisticsServiceImpl implements ReliefLogisticsService {

    /** A batch above this share of one agency's remaining stock is split across partner agencies. */
    static final double SPLIT_THRESHOLD = 0.85;
    static final String CONFLICT_MESSAGE =
            "Selected stock is no longer available due to a concurrent dispatch allocation.";

    private final ReliefDistributionService distributionService;
    private final ReliefConsignmentRepository consignmentRepository;
    private final ResourceRepository resourceRepository;
    private final ShelterRepository shelterRepository;
    private final ShelterNeedRepository needRepository;
    private final ConsignmentDetailsRepository detailsRepository;
    private final ConsignmentEventRepository eventRepository;
    private final AgencyNotificationRepository notificationRepository;
    private final DistrictRepository districtRepository;
    private final HazardEventRepository hazardEventRepository;
    private final PhotoStorageService photoStorage;
    private final ShelterMapper shelterMapper;
    private final Clock clock;

    public ReliefLogisticsServiceImpl(ReliefDistributionService distributionService,
                                      ReliefConsignmentRepository consignmentRepository,
                                      ResourceRepository resourceRepository,
                                      ShelterRepository shelterRepository,
                                      ShelterNeedRepository needRepository,
                                      ConsignmentDetailsRepository detailsRepository,
                                      ConsignmentEventRepository eventRepository,
                                      AgencyNotificationRepository notificationRepository,
                                      DistrictRepository districtRepository,
                                      HazardEventRepository hazardEventRepository,
                                      PhotoStorageService photoStorage,
                                      ShelterMapper shelterMapper,
                                      Clock clock) {
        this.distributionService = distributionService;
        this.consignmentRepository = consignmentRepository;
        this.resourceRepository = resourceRepository;
        this.shelterRepository = shelterRepository;
        this.needRepository = needRepository;
        this.detailsRepository = detailsRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.districtRepository = districtRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.photoStorage = photoStorage;
        this.shelterMapper = shelterMapper;
        this.clock = clock;
    }

    // ---------- command center ----------

    @Override
    public CommandCenterView commandCenter(Long hazardEventId, Long districtId) {
        HazardEvent event = hazardEventId == null ? null : hazardEventRepository.findById(hazardEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event not found: " + hazardEventId));
        District district = districtId != null ? districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId))
                : event != null ? event.getDistrict() : null;
        String incident = event == null ? null : label(event);
        if (district == null) {
            return new CommandCenterView(hazardEventId, null, null, incident, List.of(), List.of(), List.of(),
                    List.of(), -1, false);
        }
        Long id = district.getId();
        List<StockLine> inventory = resourceRepository.findByDistrict_Id(id).stream()
                .sorted(Comparator.comparing((Resource r) -> r.getOrganization().getName()).thenComparing(Resource::getName))
                .map(r -> new StockLine(r.getId(), r.getOrganization().getName(), r.getName(), r.getType().name(),
                        r.getQuantity(), r.getUnit(), r.getQuantity() < Resource.LOW_STOCK_THRESHOLD))
                .toList();
        List<NeedLine> needs = needRepository
                .findByShelter_District_IdAndStatusOrderByUrgentDescRequestedAtAscIdAsc(id, NeedStatus.OPEN).stream()
                .map(this::toNeedLine).toList();
        List<ShelterResponse> shelters = shelterMapper.toResponseList(shelterRepository.findByDistrict_Id(id));
        List<ReliefConsignment> consignments = consignmentRepository.findByShelter_District_IdOrderByDispatchedAtDescIdDesc(id);
        Map<Long, ConsignmentDetails> details = detailsFor(consignments);
        List<ConsignmentLine> recent = consignments.stream().limit(12).map(c -> toLine(c, details.get(c.getId()))).toList();
        int percent = fulfilmentPercent(id);
        return new CommandCenterView(event == null ? null : event.getId(), id, district.getName(), incident, inventory,
                needs, shelters, recent, percent, percent >= 0);
    }

    // ---------- shelter needs ----------

    @Override
    @Transactional
    public ShelterNeed recordNeed(Long shelterId, String resourceType, Integer quantity, boolean urgent, String note) {
        if (quantity == null || quantity <= 0) {
            throw new ReliefValidationException("The quantity needed must be a positive number.");
        }
        ResourceType type;
        try {
            type = ResourceType.valueOf(resourceType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ReliefValidationException("Invalid supply type: " + resourceType);
        }
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + shelterId));
        return needRepository.save(newNeed(shelter, type, quantity, urgent, note));
    }

    @Override
    @Transactional
    public ShelterNeed cancelNeed(Long needId) {
        ShelterNeed need = needRepository.findById(needId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter need not found: " + needId));
        if (need.getStatus() != NeedStatus.OPEN) {
            throw new ReliefValidationException("Only an open need can be withdrawn.");
        }
        need.setStatus(NeedStatus.CANCELLED);
        return needRepository.save(need);
    }

    // ---------- dispatch ----------

    @Override
    @Transactional
    public DispatchOutcome dispatch(DispatchRequest request, boolean acceptSplit, String officer) {
        Resource primary = resourceRepository.findByIdForUpdate(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Relief supply not found: " + request.resourceId()));
        int quantity = request.quantity() == null ? 0 : request.quantity();
        if (request.expectedStock() != null && request.expectedStock() >= quantity && primary.getQuantity() < quantity) {
            throw new ReliefValidationException(CONFLICT_MESSAGE);
        }
        boolean overThreshold = quantity > 0 && quantity > Math.floor(primary.getQuantity() * SPLIT_THRESHOLD);
        if (!overThreshold) {
            return new DispatchOutcome(List.of(createOne(request, primary.getId(), quantity, null, officer)), List.of());
        }
        List<ShareLine> shares = proposeSplit(primary, quantity);
        if (!acceptSplit) {
            return new DispatchOutcome(List.of(), shares.stream().map(s -> toSplitLine(s, quantity)).toList());
        }
        List<ReliefConsignment> created = new ArrayList<>();
        ReliefConsignment parent = null;
        for (ShareLine share : shares) {
            resourceRepository.findByIdForUpdate(share.resource().getId());
            ReliefConsignment c = createOne(request, share.resource().getId(), share.quantity(), parent, officer);
            if (parent == null) {
                parent = c;
            }
            created.add(c);
        }
        return new DispatchOutcome(created, List.of());
    }

    private ReliefConsignment createOne(DispatchRequest request, Long resourceId, int quantity,
                                        ReliefConsignment parent, String officer) {
        ReliefConsignment consignment = distributionService.create(resourceId, request.shelterId(), quantity);
        LocalDateTime now = LocalDateTime.now(clock);

        ConsignmentDetails details = new ConsignmentDetails();
        details.setConsignment(consignment);
        details.setParentConsignment(parent);
        if (request.hazardEventId() != null) {
            details.setHazardEvent(hazardEventRepository.findById(request.hazardEventId()).orElse(null));
        }
        details.setHandlingNotes(blankToNull(request.handlingNotes()));
        details.setVehicle(blankToNull(request.vehicle()));
        details.setDriverName(blankToNull(request.driverName()));
        details.setDriverPhone(blankToNull(request.driverPhone()));
        details.setExpectedArrival(request.expectedArrival());
        if (request.needId() != null) {
            ShelterNeed need = needRepository.findById(request.needId()).orElse(null);
            if (need != null && need.getStatus() == NeedStatus.OPEN) {
                details.setShelterNeed(need);
                need.setQuantityAllocated(need.getQuantityAllocated() + quantity);
                if (need.getQuantityAllocated() >= need.getQuantityNeeded()) {
                    need.setStatus(NeedStatus.MET);
                }
                needRepository.save(need);
            }
        }
        detailsRepository.save(details);

        ConsignmentItem item = consignment.getItems().get(0);
        Resource resource = item.getResource();
        record(consignment, ConsignmentEventType.DISPATCHED, consignment.reference() + ": " + quantity + " "
                + resource.getUnit() + " of " + resource.getName() + " from " + consignment.getOrganization().getName()
                + " to " + consignment.getShelter().getName()
                + (parent == null ? "." : " (sub-batch of " + parent.reference() + ")."), officer);
        record(consignment, ConsignmentEventType.FIELD_UNIT_NOTIFIED, "Task notification sent to the convoy unit of "
                + consignment.getOrganization().getName()
                + (details.getVehicle() != null ? " (vehicle " + details.getVehicle() + ")" : "") + ".", "system");
        notifyAgencies(consignment, consignment.reference() + " dispatched to " + consignment.getShelter().getName());
        return consignment;
    }

    /** One agency's part of a split allocation. */
    private record ShareLine(Resource resource, int quantity) {
    }

    /**
     * Each agency gives at most 85% of its remaining stock: first the chosen one, then partner agencies holding the
     * same type of supply in the district, richest first. Fails when all of them together cannot cover the quantity.
     */
    private List<ShareLine> proposeSplit(Resource primary, int quantity) {
        List<Resource> candidates = new ArrayList<>();
        candidates.add(primary);
        resourceRepository.findByTypeAndDistrict_Id(primary.getType(), primary.getDistrict().getId()).stream()
                .filter(r -> !r.getId().equals(primary.getId()) && r.getQuantity() > 0)
                .sorted(Comparator.comparing(Resource::getQuantity).reversed())
                .forEach(candidates::add);
        List<ShareLine> lines = new ArrayList<>();
        int remaining = quantity;
        for (Resource r : candidates) {
            int take = Math.min(remaining, (int) Math.floor(r.getQuantity() * SPLIT_THRESHOLD));
            if (take >= 1) {
                lines.add(new ShareLine(r, take));
                remaining -= take;
            }
            if (remaining == 0) {
                break;
            }
        }
        if (remaining > 0) {
            throw new ReliefValidationException("Not enough stock across the partner agencies: " + remaining
                    + " " + primary.getUnit() + " short. Reduce the quantity or restock first.");
        }
        return lines;
    }

    private SplitLine toSplitLine(ShareLine s, int total) {
        return new SplitLine(s.resource().getId(), s.resource().getOrganization().getName(), s.resource().getName(),
                s.quantity(), s.resource().getQuantity(), Math.round(s.quantity() * 100f / total));
    }

    // ---------- handover ----------

    @Override
    @Transactional
    public ReliefConsignment handover(Long consignmentId, HandoverRequest request, String officer) {
        ReliefConsignment consignment = findConsignment(consignmentId);
        if (consignment.getStatus() != ConsignmentStatus.DISPATCHED) {
            throw new ReliefValidationException("Only dispatched distributions can be handed over.");
        }
        if (request.receivedBy() == null || request.receivedBy().isBlank()) {
            throw new ReliefValidationException("The receiving supervisor's name (the signature) is required.");
        }
        ConsignmentItem item = consignment.getItems().get(0);
        int total = item.getQuantity();
        int damaged = request.damagedQuantity() == null ? 0 : request.damagedQuantity();
        int received = request.receivedQuantity() == null ? total - damaged : request.receivedQuantity();
        if (damaged < 0 || received < 0) {
            throw new ReliefValidationException("Quantities cannot be negative.");
        }
        if (received + damaged > total) {
            throw new ReliefValidationException("Accepted plus damaged (" + (received + damaged)
                    + ") is more than the " + total + " dispatched.");
        }
        if (damaged > 0 && (request.note() == null || request.note().isBlank())) {
            throw new ReliefValidationException("Damage notes are required for a partial handover.");
        }
        String photoUrl = null;
        if (request.photo() != null && request.photo().length > 0) {
            try {
                photoUrl = PhotoStorageService.URL_PREFIX + photoStorage.store(request.photo());
            } catch (GroundReportValidationException e) {
                throw new ReliefValidationException(e.getMessage());
            }
        }

        distributionService.deliver(consignmentId);

        ConsignmentDetails details = detailsRepository.findByConsignment_Id(consignmentId).orElseGet(() -> {
            ConsignmentDetails d = new ConsignmentDetails();
            d.setConsignment(consignment);
            return d;
        });
        details.setReceivedBy(request.receivedBy().trim());
        details.setReceivedQuantity(received);
        details.setDamagedQuantity(damaged);
        details.setHandoverNote(blankToNull(request.note()));
        details.setHandoverPhotoUrl(photoUrl);
        details.setHandedOverAt(LocalDateTime.now(clock));
        detailsRepository.save(details);

        Resource resource = item.getResource();
        String unit = resource.getUnit();
        record(consignment, ConsignmentEventType.HANDOVER, "Signed for by " + request.receivedBy().trim() + ": accepted "
                + received + " of " + total + " " + unit + (damaged > 0 ? ", " + damaged + " damaged" : "") + ".", officer);
        int unfulfilled = total - received;
        if (unfulfilled > 0) {
            ShelterNeed requeued = needRepository.save(newNeed(consignment.getShelter(), resource.getType(), unfulfilled, true,
                    "Re-queued after the handover of " + consignment.reference()
                            + (damaged > 0 ? " (" + damaged + " damaged)" : "") + "."));
            record(consignment, ConsignmentEventType.DAMAGE_REPORTED, "Loss incident: " + damaged + " " + unit
                    + " damaged, " + unfulfilled + " " + unit + " not accepted and returned to the urgent needs queue (need #"
                    + requeued.getId() + "). " + (request.note() == null ? "" : request.note().trim()), officer);
        }
        notifyAgencies(consignment, consignment.reference() + " handed over at " + consignment.getShelter().getName()
                + (unfulfilled > 0 ? " with exceptions" : ""));
        return consignment;
    }

    // ---------- re-route and cancel ----------

    @Override
    @Transactional
    public ReliefConsignment reroute(Long consignmentId, Long newShelterId, String reason, String officer) {
        ReliefConsignment consignment = findConsignment(consignmentId);
        if (consignment.getStatus() != ConsignmentStatus.DISPATCHED) {
            throw new ReliefValidationException("Only a dispatched consignment can be re-routed.");
        }
        Shelter target = shelterRepository.findById(newShelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + newShelterId));
        Shelter current = consignment.getShelter();
        if (target.getId().equals(current.getId())) {
            throw new ReliefValidationException("Choose a different shelter than the current destination.");
        }
        if (!target.getDistrict().getId().equals(current.getDistrict().getId())) {
            throw new ReliefValidationException("Choose a shelter in the same district.");
        }
        consignment.setShelter(target);
        consignmentRepository.save(consignment);
        record(consignment, ConsignmentEventType.REROUTED, "Destination changed from " + current.getName() + " to "
                + target.getName() + (reason == null || reason.isBlank() ? "." : ". Reason: " + reason.trim()), officer);
        ConsignmentDetails details = detailsRepository.findByConsignment_Id(consignmentId).orElse(null);
        String driver = details == null || details.getDriverName() == null ? null
                : details.getDriverName() + (details.getDriverPhone() == null ? "" : " (" + details.getDriverPhone() + ")");
        record(consignment, ConsignmentEventType.DRIVER_NOTIFIED, driver != null
                ? "Route alteration notice sent to the convoy driver " + driver + "."
                : "No convoy driver recorded: route alteration notice sent to the field unit of "
                + consignment.getOrganization().getName() + ".", "system");
        return consignment;
    }

    @Override
    @Transactional
    public ReliefConsignment cancel(Long consignmentId, String officer) {
        ReliefConsignment consignment = distributionService.cancel(consignmentId);
        detailsRepository.findByConsignment_Id(consignmentId).ifPresent(d -> {
            ShelterNeed need = d.getShelterNeed();
            if (need != null && need.getStatus() != NeedStatus.CANCELLED) {
                int quantity = consignment.getItems().get(0).getQuantity();
                need.setQuantityAllocated(Math.max(0, need.getQuantityAllocated() - quantity));
                if (need.getQuantityAllocated() < need.getQuantityNeeded()) {
                    need.setStatus(NeedStatus.OPEN);
                }
                needRepository.save(need);
            }
        });
        record(consignment, ConsignmentEventType.CANCELLED, consignment.reference()
                + " cancelled before delivery; the stock was returned.", officer);
        return consignment;
    }

    // ---------- reads ----------

    @Override
    public Optional<DetailsView> details(Long consignmentId) {
        return detailsRepository.findByConsignment_Id(consignmentId).map(d -> {
            List<String> linked = d.getParentConsignment() == null ? linkedChildren(consignmentId)
                    : linkedSiblings(d.getParentConsignment().getId(), consignmentId);
            return new DetailsView(
                    d.getHazardEvent() == null ? null : label(d.getHazardEvent()),
                    d.getHandlingNotes(), d.getVehicle(), d.getDriverName(), d.getDriverPhone(), d.getExpectedArrival(),
                    d.getParentConsignment() == null ? null : d.getParentConsignment().reference(),
                    d.getShelterNeed() == null ? null : "Need #" + d.getShelterNeed().getId() + " ("
                            + d.getShelterNeed().getResourceType().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ")",
                    d.getReceivedBy(), d.getReceivedQuantity(), d.getDamagedQuantity(), d.getHandoverNote(),
                    d.getHandoverPhotoUrl(), d.getHandedOverAt(), linked);
        });
    }

    @Override
    public List<EventLine> events(Long consignmentId) {
        findConsignment(consignmentId);
        return eventRepository.findByConsignment_IdOrderByCreatedAtAscIdAsc(consignmentId).stream()
                .map(e -> new EventLine(e.getCreatedAt(), e.getType().name(), e.getDetail(), e.getRecordedBy())).toList();
    }

    @Override
    public List<NotificationLine> notifications() {
        return notificationRepository.findTop100ByOrderByCreatedAtDescIdDesc().stream()
                .map(n -> new NotificationLine(n.getCreatedAt(), n.getOrganization().getName(),
                        n.getConsignment() == null ? null : n.getConsignment().reference(), n.getMessage())).toList();
    }

    @Override
    public List<ShelterResponse> rerouteOptions(Long consignmentId) {
        Shelter current = findConsignment(consignmentId).getShelter();
        return shelterMapper.toResponseList(shelterRepository.findByDistrict_Id(current.getDistrict().getId()).stream()
                .filter(s -> !s.getId().equals(current.getId())).toList());
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<HazardEvent> listIncidents() {
        return hazardEventRepository.findAllByOrderByOccurredAtDescIdDesc();
    }

    // ---------- helpers ----------

    /** Share of the requested supplies of the district that are allocated; -1 when nothing was requested. */
    private int fulfilmentPercent(Long districtId) {
        List<ShelterNeed> needs = needRepository.findByShelter_District_IdOrderByRequestedAtAscIdAsc(districtId).stream()
                .filter(n -> n.getStatus() != NeedStatus.CANCELLED).toList();
        long needed = needs.stream().mapToLong(ShelterNeed::getQuantityNeeded).sum();
        if (needed == 0) {
            return -1;
        }
        long allocated = needs.stream().mapToLong(n -> Math.min(n.getQuantityAllocated(), n.getQuantityNeeded())).sum();
        return (int) Math.round(allocated * 100.0 / needed);
    }

    private void notifyAgencies(ReliefConsignment consignment, String headline) {
        District district = consignment.getShelter().getDistrict();
        int percent = fulfilmentPercent(district.getId());
        String tail = percent < 0 ? "" : " " + district.getName() + " fulfilment: " + percent
                + "% of the requested supplies are allocated.";
        Set<Organization> agencies = new LinkedHashSet<>();
        agencies.add(consignment.getOrganization());
        resourceRepository.findByDistrict_Id(district.getId()).forEach(r -> agencies.add(r.getOrganization()));
        Set<Long> seen = new LinkedHashSet<>();
        for (Organization org : agencies) {
            if (seen.add(org.getId())) {
                AgencyNotification n = new AgencyNotification();
                n.setOrganization(org);
                n.setConsignment(consignment);
                n.setMessage(headline + "." + tail);
                n.setCreatedAt(LocalDateTime.now(clock));
                notificationRepository.save(n);
            }
        }
    }

    private void record(ReliefConsignment consignment, ConsignmentEventType type, String detail, String by) {
        ConsignmentEvent e = new ConsignmentEvent();
        e.setConsignment(consignment);
        e.setType(type);
        e.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        e.setRecordedBy(by == null || by.isBlank() ? "system" : by);
        e.setCreatedAt(LocalDateTime.now(clock));
        eventRepository.save(e);
    }

    private ShelterNeed newNeed(Shelter shelter, ResourceType type, int quantity, boolean urgent, String note) {
        ShelterNeed need = new ShelterNeed();
        need.setShelter(shelter);
        need.setResourceType(type);
        need.setQuantityNeeded(quantity);
        need.setQuantityAllocated(0);
        need.setUrgent(urgent);
        need.setStatus(NeedStatus.OPEN);
        need.setNote(blankToNull(note));
        need.setRequestedAt(LocalDateTime.now(clock));
        return need;
    }

    private NeedLine toNeedLine(ShelterNeed n) {
        int percent = n.getQuantityNeeded() == 0 ? 0
                : Math.min(100, Math.round(n.getQuantityAllocated() * 100f / n.getQuantityNeeded()));
        return new NeedLine(n.getId(), n.getShelter().getId(), n.getShelter().getName(), n.getResourceType().name(),
                n.getQuantityNeeded(), n.getQuantityAllocated(), n.remaining(), percent, Boolean.TRUE.equals(n.getUrgent()),
                n.getStatus().name(), n.getNote(), n.getRequestedAt());
    }

    private ConsignmentLine toLine(ReliefConsignment c, ConsignmentDetails d) {
        ConsignmentItem item = c.getItems().isEmpty() ? null : c.getItems().get(0);
        return new ConsignmentLine(c.getId(), c.reference(), c.getOrganization().getName(), c.getShelter().getName(),
                item == null ? "-" : item.getResource().getName(), item == null ? 0 : item.getQuantity(),
                item == null ? "" : item.getResource().getUnit(), c.getStatus().name(), d != null && d.isDamaged(),
                d == null || d.getParentConsignment() == null ? null : d.getParentConsignment().reference(),
                c.getDispatchedAt());
    }

    private Map<Long, ConsignmentDetails> detailsFor(List<ReliefConsignment> consignments) {
        if (consignments.isEmpty()) {
            return Map.of();
        }
        return detailsRepository.findByConsignment_IdIn(consignments.stream().map(ReliefConsignment::getId).toList())
                .stream().collect(Collectors.toMap(d -> d.getConsignment().getId(), d -> d, (a, b) -> a, LinkedHashMap::new));
    }

    private List<String> linkedChildren(Long parentId) {
        return detailsRepository.findByParentConsignment_Id(parentId).stream()
                .map(d -> d.getConsignment().reference()).toList();
    }

    private List<String> linkedSiblings(Long parentId, Long selfId) {
        List<String> refs = new ArrayList<>();
        refs.add(consignmentRepository.findById(parentId).map(ReliefConsignment::reference).orElse("#DIS-" + parentId));
        detailsRepository.findByParentConsignment_Id(parentId).stream()
                .filter(d -> !d.getConsignment().getId().equals(selfId)).forEach(d -> refs.add(d.getConsignment().reference()));
        return refs;
    }

    private ReliefConsignment findConsignment(Long id) {
        return consignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relief distribution not found: " + id));
    }

    private String label(HazardEvent e) {
        String type = e.getHazardType().name();
        return type.charAt(0) + type.substring(1).toLowerCase(Locale.ROOT) + " in " + e.getDistrict().getName();
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
