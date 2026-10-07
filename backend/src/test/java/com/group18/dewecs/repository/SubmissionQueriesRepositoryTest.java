package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The queries added for the mobile API: NIC lookup, replay detection and a citizen's paged report list. */
@DataJpaTest
@ActiveProfiles("test")
class SubmissionQueriesRepositoryTest {

    private static final LocalDateTime T0 = LocalDateTime.parse("2026-10-07T10:00:00.123");

    @Autowired
    private TestEntityManager em;
    @Autowired
    private CitizenRepository citizenRepository;
    @Autowired
    private GroundReportRepository groundReportRepository;

    private District district;
    private Citizen nimal;
    private Citizen kamal;

    @BeforeEach
    void data() {
        district = new District();
        district.setName("Colombo");
        em.persist(district);
        nimal = citizen("901234567V");
        kamal = citizen("199012345678");
    }

    @Test
    void findByNicIgnoreCaseMatchesAnyCaseAndNothingElse() {
        assertThat(citizenRepository.findByNicIgnoreCase("901234567v")).get().isEqualTo(nimal);
        assertThat(citizenRepository.findByNicIgnoreCase("901234567V")).get().isEqualTo(nimal);
        assertThat(citizenRepository.findByNicIgnoreCase("000000000V")).isEmpty();
    }

    @Test
    void replayLookupMatchesCitizenCategoryAndExactTimeAndReturnsTheOldestDuplicate() {
        GroundReport first = report(nimal, HazardType.FLOOD, T0);
        report(nimal, HazardType.FLOOD, T0);
        report(nimal, HazardType.LANDSLIDE, T0);
        report(nimal, HazardType.FLOOD, T0.plusSeconds(1));
        report(kamal, HazardType.FLOOD, T0);
        em.flush();

        Optional<GroundReport> found = groundReportRepository
                .findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(nimal.getId(), HazardType.FLOOD, T0);

        assertThat(found).get().isEqualTo(first);
        assertThat(groundReportRepository.findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(
                nimal.getId(), HazardType.CYCLONE, T0)).isEmpty();
    }

    @Test
    void citizenReportsArePagedNewestFirstWithIdAsTieBreaker() {
        GroundReport oldest = report(nimal, HazardType.FLOOD, T0);
        GroundReport tieA = report(nimal, HazardType.FLOOD, T0.plusHours(1));
        GroundReport tieB = report(nimal, HazardType.DROUGHT, T0.plusHours(1));
        GroundReport newest = report(nimal, HazardType.CYCLONE, T0.plusHours(2));
        report(kamal, HazardType.FLOOD, T0.plusHours(5));
        em.flush();
        em.clear();

        Page<GroundReport> first = groundReportRepository
                .findByReportedBy_IdOrderBySubmittedAtDescIdDesc(nimal.getId(), PageRequest.of(0, 3));
        Page<GroundReport> second = groundReportRepository
                .findByReportedBy_IdOrderBySubmittedAtDescIdDesc(nimal.getId(), PageRequest.of(1, 3));

        assertThat(first.getContent()).extracting(GroundReport::getId)
                .containsExactly(newest.getId(), tieB.getId(), tieA.getId());
        assertThat(second.getContent()).extracting(GroundReport::getId).containsExactly(oldest.getId());
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    private Citizen citizen(String nic) {
        Citizen c = new Citizen();
        c.setFullName("Citizen " + nic);
        c.setNic(nic);
        c.setDistrict(district);
        return em.persist(c);
    }

    private GroundReport report(Citizen by, HazardType category, LocalDateTime at) {
        GroundReport r = new GroundReport();
        r.setReportedBy(by);
        r.setDistrict(district);
        r.setCategory(category);
        r.setStatus(GroundReportStatus.PENDING_REVIEW);
        r.setGpsLat(new BigDecimal("6.9271234"));
        r.setGpsLng(new BigDecimal("79.8612345"));
        r.setSubmittedAt(at);
        return em.persist(r);
    }
}
