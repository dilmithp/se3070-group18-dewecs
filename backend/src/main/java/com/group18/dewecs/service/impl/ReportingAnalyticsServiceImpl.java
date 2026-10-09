package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.ConsignmentDetails;
import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.NeedStatus;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.ReportDetails;
import com.group18.dewecs.domain.ReportKpi;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterNeed;
import com.group18.dewecs.exception.IncompleteDataException;
import com.group18.dewecs.exception.ReportValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.ConsignmentDetailsRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ReportDetailsRepository;
import com.group18.dewecs.repository.ReportKpiRepository;
import com.group18.dewecs.repository.ShelterNeedRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.PostEventReportService;
import com.group18.dewecs.service.ReportingAnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingAnalyticsServiceImpl implements ReportingAnalyticsService {

    private final PostEventReportService postEventReportService;
    private final HazardEventRepository hazardEventRepository;
    private final ShelterRepository shelterRepository;
    private final ShelterNeedRepository needRepository;
    private final ReliefConsignmentRepository consignmentRepository;
    private final ConsignmentDetailsRepository detailsRepository;
    private final CitizenRepository citizenRepository;
    private final OrganizationRepository organizationRepository;
    private final ReportDetailsRepository reportDetailsRepository;
    private final ReportKpiRepository kpiRepository;
    private final Clock clock;

    public ReportingAnalyticsServiceImpl(PostEventReportService postEventReportService,
                                         HazardEventRepository hazardEventRepository,
                                         ShelterRepository shelterRepository,
                                         ShelterNeedRepository needRepository,
                                         ReliefConsignmentRepository consignmentRepository,
                                         ConsignmentDetailsRepository detailsRepository,
                                         CitizenRepository citizenRepository,
                                         OrganizationRepository organizationRepository,
                                         ReportDetailsRepository reportDetailsRepository,
                                         ReportKpiRepository kpiRepository,
                                         Clock clock) {
        this.postEventReportService = postEventReportService;
        this.hazardEventRepository = hazardEventRepository;
        this.shelterRepository = shelterRepository;
        this.needRepository = needRepository;
        this.consignmentRepository = consignmentRepository;
        this.detailsRepository = detailsRepository;
        this.citizenRepository = citizenRepository;
        this.organizationRepository = organizationRepository;
        this.reportDetailsRepository = reportDetailsRepository;
        this.kpiRepository = kpiRepository;
        this.clock = clock;
    }

    @Override
    public List<String> findDataGaps(Parameters p) {
        HazardEvent event = event(p.hazardEventId());
        Window window = window(event, p);
        List<String> gaps = new ArrayList<>();
        for (Shelter shelter : shelterRepository.findByDistrict_Id(event.getDistrict().getId())) {
            if (shelter.getCurrentOccupancy() == null || shelter.getCurrentOccupancy() == 0) {
                gaps.add("Shelter \"" + shelter.getName() + "\" has no registered intake headcount.");
            }
        }
        for (ReliefConsignment c : inWindow(event, window, null)) {
            if (c.getStatus() == ConsignmentStatus.DISPATCHED) {
                gaps.add("Consignment " + c.reference() + " was dispatched but has no verified handover (closing time).");
            }
        }
        return gaps;
    }

    @Override
    @Transactional
    public PostEventReport generate(Parameters p, GapHandling handling, String justification) {
        HazardEvent event = event(p.hazardEventId());
        List<String> gaps = findDataGaps(p);
        if (!gaps.isEmpty() && handling == null) {
            throw new IncompleteDataException(gaps);
        }
        if (!gaps.isEmpty() && handling == GapHandling.OVERRIDE && (justification == null || justification.isBlank())) {
            throw new ReportValidationException("An administrative justification note is required to override the data gaps.");
        }
        Organization donor = p.donorOrganizationId() == null ? null : organizationRepository.findById(p.donorOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + p.donorOrganizationId()));
        Window window = window(event, p);

        PostEventReport report = postEventReportService.generate(event.getId());

        ReportDetails details = new ReportDetails();
        details.setReport(report);
        details.setWindowFrom(window.from());
        details.setWindowTo(window.to());
        details.setDonor(donor);
        boolean provisional = !gaps.isEmpty() && handling == GapHandling.PROVISIONAL;
        details.setProvisional(provisional);
        if (!gaps.isEmpty()) {
            details.setDataGaps(trim(String.join(" ", gaps)));
            details.setProvisionalReason(trim(provisional
                    ? "Provisional draft with flagged data gaps." + (justification == null || justification.isBlank() ? "" : " " + justification.trim())
                    : "Administrative override: " + justification.trim()));
        }
        reportDetailsRepository.save(details);

        kpiRepository.saveAll(computeKpis(report, event, window, donor));
        return report;
    }

    @Override
    @Transactional
    public ReportDetails approve(Long reportId, String officer) {
        ReportDetails details = reportDetailsRepository.findByReport_Id(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis details for report " + reportId));
        if (Boolean.TRUE.equals(details.getProvisional())) {
            throw new ReportValidationException("A provisional draft cannot be approved. Generate the report again once the data gaps are closed.");
        }
        if (details.getApprovedAt() != null) {
            throw new ReportValidationException("This report is already approved.");
        }
        details.setApprovedBy(officer == null || officer.isBlank() ? "DMC officer" : officer.trim());
        details.setApprovedAt(LocalDateTime.now(clock));
        return reportDetailsRepository.save(details);
    }

    @Override
    public Optional<ReportDetails> details(Long reportId) {
        return reportDetailsRepository.findByReport_Id(reportId);
    }

    @Override
    public List<ReportKpi> kpis(Long reportId) {
        return kpiRepository.findByReport_IdOrderBySortOrderAscIdAsc(reportId);
    }

    @Override
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll().stream().sorted(Comparator.comparing(Organization::getName)).toList();
    }

    // ---------- indicators ----------

    private List<ReportKpi> computeKpis(PostEventReport report, HazardEvent event, Window window, Organization donor) {
        Long districtId = event.getDistrict().getId();
        List<ReliefConsignment> consignments = inWindow(event, window, donor).stream()
                .filter(c -> c.getStatus() != ConsignmentStatus.CANCELLED).toList();
        Map<Long, ConsignmentDetails> details = consignments.isEmpty() ? Map.of()
                : detailsRepository.findByConsignment_IdIn(consignments.stream().map(ReliefConsignment::getId).toList())
                .stream().collect(Collectors.toMap(d -> d.getConsignment().getId(), d -> d, (a, b) -> a));

        long dispatched = 0;
        long delivered = 0;
        long damaged = 0;
        long deliveredConsignments = 0;
        Map<Long, Long> byOrg = new LinkedHashMap<>();
        Map<Long, String> orgNames = new LinkedHashMap<>();
        Map<ResourceType, Long> distributedByType = new EnumMap<>(ResourceType.class);
        for (ReliefConsignment c : consignments) {
            long qty = c.getItems().stream().mapToLong(ConsignmentItem::getQuantity).sum();
            dispatched += qty;
            byOrg.merge(c.getOrganization().getId(), qty, Long::sum);
            orgNames.put(c.getOrganization().getId(), c.getOrganization().getName());
            c.getItems().forEach(i -> distributedByType.merge(i.getResource().getType(), (long) i.getQuantity(), Long::sum));
            if (c.getStatus() == ConsignmentStatus.DELIVERED) {
                deliveredConsignments++;
                ConsignmentDetails d = details.get(c.getId());
                delivered += d != null && d.getReceivedQuantity() != null ? d.getReceivedQuantity() : qty;
                damaged += d != null && d.getDamagedQuantity() != null ? d.getDamagedQuantity() : 0;
            }
        }

        List<ReportKpi> kpis = new ArrayList<>();
        long citizens = citizenRepository.countByDistrict_Id(districtId);
        kpis.add(kpi(report, "relief_per_capita", "Relief delivered per capita", citizens == 0 ? null : round(delivered / (double) citizens),
                "units per citizen", citizens == 0 ? "No registered citizens in the district." : delivered + " units accepted / " + citizens + " registered citizens."));
        Optional<LocalDateTime> first = consignments.stream().map(ReliefConsignment::getDispatchedAt)
                .filter(t -> t != null).min(Comparator.naturalOrder());
        kpis.add(kpi(report, "response_latency", "Response latency", first.map(t ->
                        (double) Duration.between(event.getOccurredAt(), t).toMinutes()).orElse(null),
                "minutes", first.isPresent() ? "From the start of the incident to the first dispatch." : "Nothing was dispatched in this window."));
        kpis.add(kpi(report, "delivery_rate", "Consignments delivered", consignments.isEmpty() ? null
                : round(deliveredConsignments * 100.0 / consignments.size()), "%",
                deliveredConsignments + " of " + consignments.size() + " consignments handed over."));
        kpis.add(kpi(report, "units_dispatched", "Units dispatched", (double) dispatched, "units", null));
        kpis.add(kpi(report, "units_delivered", "Units accepted at handover", (double) delivered, "units", null));
        kpis.add(kpi(report, "units_damaged", "Units damaged (loss)", (double) damaged, "units", damaged > 0 ? "Logged as loss incidents in the audit log." : null));

        List<ShelterNeed> needs = needRepository.findByShelter_District_IdOrderByRequestedAtAscIdAsc(districtId).stream()
                .filter(n -> n.getStatus() != NeedStatus.CANCELLED && !n.getRequestedAt().isBefore(window.from()) && !n.getRequestedAt().isAfter(window.to()))
                .toList();
        long needed = needs.stream().mapToLong(ShelterNeed::getQuantityNeeded).sum();
        long met = needs.stream().mapToLong(n -> Math.min(n.getQuantityAllocated(), n.getQuantityNeeded())).sum();
        kpis.add(kpi(report, "needs_met", "Requested supplies allocated", needed == 0 ? null : round(met * 100.0 / needed), "%",
                needed == 0 ? "No shelter needs were recorded in this window." : met + " of " + needed + " requested units allocated."));

        int order = 10;
        for (Map.Entry<Long, Long> e : byOrg.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).toList()) {
            kpis.add(kpi(report, "share", orgNames.get(e.getKey()), dispatched == 0 ? null : round(e.getValue() * 100.0 / dispatched),
                    "%", e.getValue() + " units dispatched", order++));
        }
        Map<ResourceType, Long> needByType = new EnumMap<>(ResourceType.class);
        needs.forEach(n -> needByType.merge(n.getResourceType(), (long) n.getQuantityNeeded(), Long::sum));
        for (ResourceType type : ResourceType.values()) {
            long n = needByType.getOrDefault(type, 0L);
            long d = distributedByType.getOrDefault(type, 0L);
            if (n > 0 || d > 0) {
                String label = type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
                kpis.add(kpi(report, "need", label, (double) n, "units", "Requested by shelters", order++));
                kpis.add(kpi(report, "distributed", label, (double) d, "units", "Dispatched", order++));
            }
        }
        return kpis;
    }

    private ReportKpi kpi(PostEventReport report, String key, String label, Double value, String unit, String detail) {
        return kpi(report, key, label, value, unit, detail, kpiOrder(key));
    }

    private ReportKpi kpi(PostEventReport report, String key, String label, Double value, String unit, String detail, int order) {
        ReportKpi k = new ReportKpi();
        k.setReport(report);
        k.setKpiKey(key);
        k.setLabel(label);
        k.setValue(value);
        k.setUnit(unit);
        k.setDetail(detail);
        k.setSortOrder(order);
        return k;
    }

    private int kpiOrder(String key) {
        return switch (key) {
            case "relief_per_capita" -> 1;
            case "response_latency" -> 2;
            case "delivery_rate" -> 3;
            case "units_dispatched" -> 4;
            case "units_delivered" -> 5;
            case "units_damaged" -> 6;
            case "needs_met" -> 7;
            default -> 9;
        };
    }

    // ---------- helpers ----------

    private record Window(LocalDateTime from, LocalDateTime to) {
    }

    private Window window(HazardEvent event, Parameters p) {
        LocalDateTime from = p.from() != null ? p.from() : event.getOccurredAt();
        LocalDateTime to = p.to() != null ? p.to() : LocalDateTime.now(clock);
        if (to.isBefore(from)) {
            throw new ReportValidationException("The end of the time window is before its start.");
        }
        return new Window(from, to);
    }

    private List<ReliefConsignment> inWindow(HazardEvent event, Window window, Organization donor) {
        return consignmentRepository.findByShelter_District_IdOrderByDispatchedAtDescIdDesc(event.getDistrict().getId()).stream()
                .filter(c -> c.getDispatchedAt() != null && !c.getDispatchedAt().isBefore(window.from())
                        && !c.getDispatchedAt().isAfter(window.to()))
                .filter(c -> donor == null || c.getOrganization().getId().equals(donor.getId()))
                .toList();
    }

    private HazardEvent event(Long id) {
        if (id == null) {
            throw new ReportValidationException("Choose the hazard event to report on.");
        }
        return hazardEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event " + id + " not found"));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String trim(String s) {
        return s != null && s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
