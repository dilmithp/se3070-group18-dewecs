package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.MetricType;
import com.group18.dewecs.domain.PostEventReport;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.ReportMetric;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.PostEventReportRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ReportMetricRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.WarningRepository;
import com.group18.dewecs.service.PostEventReportService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PostEventReportServiceImpl implements PostEventReportService {

    /** Warnings that were actually published (not drafts, not retracted). */
    private static final Set<WarningStatus> PUBLISHED = EnumSet.of(WarningStatus.ISSUED, WarningStatus.UPDATED,
            WarningStatus.EXPIRED);

    private final PostEventReportRepository reportRepository;
    private final ReportMetricRepository metricRepository;
    private final HazardEventRepository hazardEventRepository;
    private final WarningRepository warningRepository;
    private final ShelterRepository shelterRepository;
    private final CitizenRepository citizenRepository;
    private final ReliefConsignmentRepository consignmentRepository;
    private final Clock clock;

    public PostEventReportServiceImpl(PostEventReportRepository reportRepository,
                                      ReportMetricRepository metricRepository,
                                      HazardEventRepository hazardEventRepository,
                                      WarningRepository warningRepository,
                                      ShelterRepository shelterRepository,
                                      CitizenRepository citizenRepository,
                                      ReliefConsignmentRepository consignmentRepository,
                                      Clock clock) {
        this.reportRepository = reportRepository;
        this.metricRepository = metricRepository;
        this.hazardEventRepository = hazardEventRepository;
        this.warningRepository = warningRepository;
        this.shelterRepository = shelterRepository;
        this.citizenRepository = citizenRepository;
        this.consignmentRepository = consignmentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PostEventReport generate(Long hazardEventId) {
        HazardEvent event = hazardEventRepository.findById(hazardEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Hazard event " + hazardEventId + " not found"));
        Long districtId = event.getDistrict().getId();

        List<Warning> warnings = warningRepository.findByHazardEvent_IdAndStatusIn(hazardEventId, PUBLISHED);
        List<Shelter> shelters = shelterRepository.findByDistrict_Id(districtId);

        PostEventReport report = new PostEventReport();
        report.setDistrict(event.getDistrict());
        report.setHazardEvent(event);
        report.setGeneratedAt(LocalDateTime.now(clock));
        report.setRelatedWarnings(new HashSet<>(warnings));
        report.setRelatedShelters(new HashSet<>(shelters));
        PostEventReport saved = reportRepository.save(report);

        List<ReportMetric> metrics = new ArrayList<>();
        alertLeadMinutes(event, warnings).ifPresent(v -> metrics.add(metric(saved, MetricType.ALERT_TIMELINE, v)));
        metrics.add(metric(saved, MetricType.CITIZENS_REACHED,
                warnings.isEmpty() ? 0 : citizenRepository.countByDistrict_Id(districtId)));
        metrics.add(metric(saved, MetricType.SHELTER_OCCUPANCY, occupancyPercent(shelters)));
        metrics.add(metric(saved, MetricType.RESOURCE_DISTRIBUTION, deliveredUnits(event, districtId)));
        metricRepository.saveAll(metrics);
        return saved;
    }

    @Override
    public PostEventReport getById(Long reportId) {
        return reportRepository.findWithRelationsById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Post-event report " + reportId + " not found"));
    }

    @Override
    public List<PostEventReport> list() {
        return reportRepository.findAllByOrderByGeneratedAtDescIdDesc();
    }

    @Override
    public List<ReportMetric> listMetrics(Long reportId) {
        getById(reportId);
        return metricRepository.findByReport_IdOrderByMetricType(reportId);
    }

    @Override
    public List<HazardEvent> listHazardEvents() {
        return hazardEventRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"));
    }

    /** Minutes from the event's start to the first published warning; empty when none was published. */
    private Optional<Double> alertLeadMinutes(HazardEvent event, List<Warning> warnings) {
        return warnings.stream()
                .map(Warning::getIssuedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(first -> (double) Duration.between(event.getOccurredAt(), first).toMinutes());
    }

    private double occupancyPercent(List<Shelter> shelters) {
        int capacity = shelters.stream().mapToInt(Shelter::getCapacity).sum();
        if (capacity == 0) {
            return 0;
        }
        int occupied = shelters.stream().mapToInt(Shelter::getCurrentOccupancy).sum();
        return Math.round(occupied * 1000.0 / capacity) / 10.0;
    }

    /** Units delivered to shelters of the district since the event started. */
    private double deliveredUnits(HazardEvent event, Long districtId) {
        return consignmentRepository.findByStatusAndShelter_District_Id(ConsignmentStatus.DELIVERED, districtId)
                .stream()
                .filter(c -> c.getDispatchedAt() == null || !c.getDispatchedAt().isBefore(event.getOccurredAt()))
                .map(ReliefConsignment::getItems)
                .flatMap(List::stream)
                .mapToInt(ConsignmentItem::getQuantity)
                .sum();
    }

    private ReportMetric metric(PostEventReport report, MetricType type, double value) {
        ReportMetric m = new ReportMetric();
        m.setReport(report);
        m.setMetricType(type);
        m.setValue(value);
        return m;
    }
}
