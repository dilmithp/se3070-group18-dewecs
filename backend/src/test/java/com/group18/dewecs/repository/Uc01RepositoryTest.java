package com.group18.dewecs.repository;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.domain.Citizen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The mappings and queries added for UC-01: river basins, warning districts, delivery log, hazard events, evidence. */
@DataJpaTest
@ActiveProfiles("test")
class Uc01RepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private RiverBasinRepository riverBasinRepository;
    @Autowired
    private AlertDeliveryLogRepository logRepository;
    @Autowired
    private HazardEventRepository hazardEventRepository;
    @Autowired
    private GroundReportRepository groundReportRepository;

    private District galle;
    private District matara;
    private District kandy;
    private User officer;

    @BeforeEach
    void setUp() {
        galle = persistDistrict("Galle");
        matara = persistDistrict("Matara");
        kandy = persistDistrict("Kandy");
        officer = new User();
        officer.setFullName("Officer");
        officer.setPhone("0710000001");
        officer.setDistrict(galle);
        em.persist(officer);
    }

    private District persistDistrict(String name) {
        District d = new District();
        d.setName(name);
        return em.persist(d);
    }

    private HazardEvent event(District district, HazardEventStatus status, LocalDateTime at) {
        HazardEvent e = new HazardEvent();
        e.setHazardType(HazardType.FLOOD);
        e.setSeverityLevel(Severity.HIGH);
        e.setStatus(status);
        e.setDistrict(district);
        e.setOccurredAt(at);
        return em.persist(e);
    }

    private Warning warning(HazardEvent event, Set<District> affected) {
        Warning w = new Warning();
        w.setHazardEvent(event);
        w.setSeverity(Severity.HIGH);
        w.setStatus(WarningStatus.ISSUED);
        w.setMessage("Move to higher ground");
        w.setIssuedBy(officer);
        w.setIssuedAt(LocalDateTime.now());
        w.setAffectedDistricts(affected);
        return em.persist(w);
    }

    @Test
    void aRiverBasinKeepsItsDistricts() {
        RiverBasin nilwala = new RiverBasin();
        nilwala.setName("Nilwala Ganga");
        nilwala.setDistricts(new HashSet<>(Set.of(galle, matara)));
        em.persistAndFlush(nilwala);
        em.clear();

        RiverBasin loaded = riverBasinRepository.findById(nilwala.getId()).orElseThrow();

        assertThat(loaded.getDistricts()).extracting(District::getName).containsExactlyInAnyOrder("Galle", "Matara");
    }

    @Test
    void basinsAreListedByName() {
        for (String name : List.of("Walawe", "Kelani", "Gin")) {
            RiverBasin b = new RiverBasin();
            b.setName(name);
            em.persist(b);
        }
        em.flush();

        assertThat(riverBasinRepository.findAllByOrderByNameAsc()).extracting(RiverBasin::getName)
                .containsExactly("Gin", "Kelani", "Walawe");
    }

    @Test
    void warningsAreFoundByAnyDistrictTheyCover() {
        HazardEvent inGalle = event(galle, HazardEventStatus.ACTIVE, LocalDateTime.now());
        Warning basinWarning = warning(inGalle, new HashSet<>(Set.of(galle, matara)));
        Warning plain = warning(event(kandy, HazardEventStatus.ACTIVE, LocalDateTime.now()), new HashSet<>());
        em.flush();
        em.clear();

        assertThat(warningRepository.findByAffectedDistricts_Id(matara.getId()))
                .extracting(Warning::getId).containsExactly(basinWarning.getId());
        assertThat(warningRepository.findByAffectedDistricts_Id(kandy.getId())).isEmpty();
        assertThat(warningRepository.findById(plain.getId()).orElseThrow().effectiveDistricts())
                .extracting(District::getName).containsExactly("Kandy");
        assertThat(warningRepository.findById(basinWarning.getId()).orElseThrow().getAffectedDistricts()).hasSize(2);
    }

    @Test
    void theDeliveryLogComesBackOldestFirstWithEveryField() {
        Warning w = warning(event(galle, HazardEventStatus.ACTIVE, LocalDateTime.now()), new HashSet<>());
        AlertDeliveryLog later = logRow(w, BroadcastChannel.RADIO, 1, DeliveryOutcome.SENT, LocalDateTime.of(2026, 10, 9, 9, 5));
        later.setFallbackFor(BroadcastChannel.SMS);
        later.setRecipients(40);
        later.setDetail("ok");
        AlertDeliveryLog earlier = logRow(w, BroadcastChannel.SMS, 2, DeliveryOutcome.FAILED, LocalDateTime.of(2026, 10, 9, 9, 0));
        em.persist(later);
        em.persist(earlier);
        em.flush();
        em.clear();

        List<AlertDeliveryLog> rows = logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId());

        assertThat(rows).extracting(AlertDeliveryLog::getChannel)
                .containsExactly(BroadcastChannel.SMS, BroadcastChannel.RADIO);
        AlertDeliveryLog second = rows.get(1);
        assertThat(second.getFallbackFor()).isEqualTo(BroadcastChannel.SMS);
        assertThat(second.getRecipients()).isEqualTo(40);
        assertThat(second.getTrigger()).isEqualTo(DeliveryTrigger.PUBLISH);
        assertThat(logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(w.getId() + 1000)).isEmpty();
    }

    private AlertDeliveryLog logRow(Warning w, BroadcastChannel channel, int attempt, DeliveryOutcome outcome,
                                    LocalDateTime at) {
        AlertDeliveryLog row = new AlertDeliveryLog();
        row.setWarning(w);
        row.setChannel(channel);
        row.setAttempt(attempt);
        row.setOutcome(outcome);
        row.setTrigger(DeliveryTrigger.PUBLISH);
        row.setCreatedAt(at);
        return row;
    }

    @Test
    void hazardEventsAreListedNewestFirstAndCanBeFilteredByStatus() {
        HazardEvent old = event(galle, HazardEventStatus.RESOLVED, LocalDateTime.now().minusDays(5));
        HazardEvent mid = event(kandy, HazardEventStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        HazardEvent recent = event(matara, HazardEventStatus.ACTIVE, LocalDateTime.now().minusHours(1));
        em.flush();

        assertThat(hazardEventRepository.findAllByOrderByOccurredAtDescIdDesc())
                .containsExactly(recent, mid, old);
        assertThat(hazardEventRepository.findByStatusOrderByOccurredAtDescIdDesc(HazardEventStatus.ACTIVE))
                .containsExactly(recent, mid);
    }

    @Test
    void evidenceQueryReturnsOnlyVerifiedOrActionedReportsOfTheDistrictNewestFirst() {
        Citizen citizen = new Citizen();
        citizen.setFullName("Citizen");
        citizen.setPhone("0720000001");
        citizen.setDistrict(galle);
        citizen.setNic("200000000101");
        em.persist(citizen);
        GroundReport verifiedOld = report(citizen, galle, GroundReportStatus.VERIFIED, LocalDateTime.now().minusHours(5));
        GroundReport actionedNew = report(citizen, galle, GroundReportStatus.ACTIONED, LocalDateTime.now().minusHours(1));
        report(citizen, galle, GroundReportStatus.PENDING_REVIEW, LocalDateTime.now());
        report(citizen, galle, GroundReportStatus.REJECTED, LocalDateTime.now());
        report(citizen, kandy, GroundReportStatus.VERIFIED, LocalDateTime.now());
        em.flush();

        List<GroundReport> evidence = groundReportRepository.findByDistrict_IdAndStatusInOrderBySubmittedAtDescIdDesc(
                galle.getId(), EnumSet.of(GroundReportStatus.VERIFIED, GroundReportStatus.ACTIONED));

        assertThat(evidence).containsExactly(actionedNew, verifiedOld);
    }

    private GroundReport report(Citizen citizen, District district, GroundReportStatus status, LocalDateTime at) {
        GroundReport r = new GroundReport();
        r.setReportedBy(citizen);
        r.setDistrict(district);
        r.setCategory(HazardType.FLOOD);
        r.setDescription("Water rising " + at.getNano() + status);
        r.setStatus(status);
        r.setGpsLat(new BigDecimal("6.05"));
        r.setGpsLng(new BigDecimal("80.22"));
        r.setSubmittedAt(at);
        return em.persist(r);
    }
}
