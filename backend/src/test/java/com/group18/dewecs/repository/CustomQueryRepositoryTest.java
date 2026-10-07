package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The hand-written queries: the three search methods, findAvailable, findByQuantityLessThan and the counts. */
@DataJpaTest
@ActiveProfiles("test")
class CustomQueryRepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private GroundReportRepository groundReportRepository;
    @Autowired
    private RescueRequestRepository rescueRequestRepository;
    @Autowired
    private ReliefConsignmentRepository consignmentRepository;
    @Autowired
    private ShelterRepository shelterRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private WarningRepository warningRepository;

    private District galle;
    private District kandy;
    private Organization org;

    @BeforeEach
    void baseData() {
        galle = persistDistrict("Galle");
        kandy = persistDistrict("Kandy");
        org = new Organization();
        org.setName("Red Cross");
        org.setType(OrganizationType.NGO);
        em.persist(org);
    }

    @Test
    void groundReportSearchFiltersByStatusDistrictAndCategoryAndTreatsNullAsAny() {
        persistReport(galle, HazardType.FLOOD, GroundReportStatus.PENDING_REVIEW);
        persistReport(galle, HazardType.LANDSLIDE, GroundReportStatus.VERIFIED);
        persistReport(kandy, HazardType.FLOOD, GroundReportStatus.PENDING_REVIEW);
        em.flush();

        assertThat(groundReportRepository.search(null, null, null)).hasSize(3);
        assertThat(groundReportRepository.search(GroundReportStatus.PENDING_REVIEW, null, null)).hasSize(2);
        assertThat(groundReportRepository.search(null, galle.getId(), null)).hasSize(2);
        assertThat(groundReportRepository.search(null, null, HazardType.FLOOD)).hasSize(2);
        assertThat(groundReportRepository.search(GroundReportStatus.PENDING_REVIEW, galle.getId(), HazardType.FLOOD))
                .hasSize(1);
        assertThat(groundReportRepository.search(GroundReportStatus.REJECTED, null, null)).isEmpty();
    }

    @Test
    void rescueRequestSearchFiltersByStatusPriorityAndDistrict() {
        persistRequest(galle, Severity.CRITICAL, RescueRequestStatus.PENDING);
        persistRequest(galle, Severity.LOW, RescueRequestStatus.COMPLETED);
        persistRequest(kandy, Severity.CRITICAL, RescueRequestStatus.PENDING);
        em.flush();

        assertThat(rescueRequestRepository.search(null, null, null)).hasSize(3);
        assertThat(rescueRequestRepository.search(RescueRequestStatus.PENDING, null, null)).hasSize(2);
        assertThat(rescueRequestRepository.search(null, Severity.CRITICAL, kandy.getId())).hasSize(1);
        assertThat(rescueRequestRepository.search(RescueRequestStatus.COMPLETED, Severity.CRITICAL, null)).isEmpty();
    }

    @Test
    void consignmentSearchFiltersByStatusShelterAndSuppliedResource() {
        Shelter shelterA = persistShelter("A", 10, 0, ShelterStatus.OPEN);
        Shelter shelterB = persistShelter("B", 10, 0, ShelterStatus.OPEN);
        Resource rice = persistResource("Rice", 100);
        Resource water = persistResource("Water", 100);
        persistConsignment(shelterA, rice, ConsignmentStatus.DISPATCHED);
        persistConsignment(shelterA, water, ConsignmentStatus.DELIVERED);
        persistConsignment(shelterB, rice, ConsignmentStatus.DISPATCHED);
        em.flush();
        em.clear();

        assertThat(consignmentRepository.search(null, null, null)).hasSize(3);
        assertThat(consignmentRepository.search(ConsignmentStatus.DISPATCHED, null, null)).hasSize(2);
        assertThat(consignmentRepository.search(null, shelterA.getId(), null)).hasSize(2);
        assertThat(consignmentRepository.search(null, null, rice.getId())).hasSize(2);
        assertThat(consignmentRepository.search(ConsignmentStatus.DISPATCHED, shelterB.getId(), rice.getId()))
                .hasSize(1);
        assertThat(consignmentRepository.search(ConsignmentStatus.CANCELLED, null, null)).isEmpty();
    }

    @Test
    void findAvailableExcludesFullAndClosedShelters() {
        persistShelter("Open with room", 10, 3, ShelterStatus.OPEN);
        persistShelter("Full", 5, 5, ShelterStatus.FULL);
        persistShelter("Closed with room", 10, 0, ShelterStatus.CLOSED);
        em.flush();

        assertThat(shelterRepository.findAvailable()).extracting(Shelter::getName).containsExactly("Open with room");
    }

    @Test
    void lowStockQueriesUseAStrictLessThan() {
        persistResource("Empty", 0);
        persistResource("Nine", 9);
        persistResource("Ten", 10);
        persistResource("Plenty", 50);
        em.flush();

        assertThat(resourceRepository.findByQuantityLessThan(Resource.LOW_STOCK_THRESHOLD))
                .extracting(Resource::getName).containsExactlyInAnyOrder("Empty", "Nine");
        assertThat(resourceRepository.countByQuantityLessThan(Resource.LOW_STOCK_THRESHOLD)).isEqualTo(2);
    }

    @Test
    void countByStatusMethodsCountOnlyTheRequestedStatus() {
        persistReport(galle, HazardType.FLOOD, GroundReportStatus.PENDING_REVIEW);
        persistReport(galle, HazardType.FLOOD, GroundReportStatus.VERIFIED);
        persistRequest(galle, Severity.HIGH, RescueRequestStatus.PENDING);
        persistRequest(galle, Severity.HIGH, RescueRequestStatus.PENDING);
        persistRequest(galle, Severity.HIGH, RescueRequestStatus.CANCELLED);
        persistShelter("Full", 5, 5, ShelterStatus.FULL);
        persistShelter("Open", 5, 1, ShelterStatus.OPEN);
        persistWarning(WarningStatus.ISSUED);
        persistWarning(WarningStatus.DRAFT);
        persistWarning(WarningStatus.ISSUED);
        em.flush();

        assertThat(groundReportRepository.countByStatus(GroundReportStatus.PENDING_REVIEW)).isEqualTo(1);
        assertThat(rescueRequestRepository.countByStatus(RescueRequestStatus.PENDING)).isEqualTo(2);
        assertThat(shelterRepository.countByStatus(ShelterStatus.FULL)).isEqualTo(1);
        assertThat(warningRepository.countByStatus(WarningStatus.ISSUED)).isEqualTo(2);
    }

    private District persistDistrict(String name) {
        District d = new District();
        d.setName(name);
        return em.persist(d);
    }

    private void persistReport(District district, HazardType category, GroundReportStatus status) {
        Citizen c = new Citizen();
        c.setFullName("Citizen");
        c.setDistrict(district);
        c.setNic("NIC" + System.nanoTime());
        em.persist(c);
        GroundReport r = new GroundReport();
        r.setReportedBy(c);
        r.setDistrict(district);
        r.setCategory(category);
        r.setStatus(status);
        r.setGpsLat(new BigDecimal("6.0"));
        r.setGpsLng(new BigDecimal("80.0"));
        r.setSubmittedAt(LocalDateTime.now());
        em.persist(r);
    }

    private void persistRequest(District district, Severity priority, RescueRequestStatus status) {
        RescueRequest r = new RescueRequest();
        r.setDistrict(district);
        r.setRequesterName("K");
        r.setRequesterPhone("07");
        r.setDescription("help");
        r.setPriority(priority);
        r.setStatus(status);
        r.setSubmittedAt(LocalDateTime.now());
        em.persist(r);
    }

    private Shelter persistShelter(String name, int capacity, int occupancy, ShelterStatus status) {
        Shelter s = new Shelter();
        s.setName(name);
        s.setDistrict(galle);
        s.setOrganization(org);
        s.setCapacity(capacity);
        s.setCurrentOccupancy(occupancy);
        s.setStatus(status);
        return em.persist(s);
    }

    private Resource persistResource(String name, int quantity) {
        Resource r = new Resource();
        r.setName(name);
        r.setType(ResourceType.FOOD);
        r.setUnit("kg");
        r.setQuantity(quantity);
        r.setDistrict(galle);
        r.setOrganization(org);
        return em.persist(r);
    }

    private void persistConsignment(Shelter shelter, Resource resource, ConsignmentStatus status) {
        ReliefConsignment c = new ReliefConsignment();
        c.setOrganization(org);
        c.setShelter(shelter);
        c.setStatus(status);
        c.setDispatchedAt(LocalDateTime.now());
        em.persist(c);
        ConsignmentItem item = new ConsignmentItem();
        item.setConsignment(c);
        item.setResource(resource);
        item.setQuantity(1);
        em.persist(item);
    }

    private void persistWarning(WarningStatus status) {
        HazardEvent h = new HazardEvent();
        h.setHazardType(HazardType.FLOOD);
        h.setSeverityLevel(Severity.HIGH);
        h.setStatus(HazardEventStatus.ACTIVE);
        h.setDistrict(galle);
        h.setOccurredAt(LocalDateTime.now());
        em.persist(h);
        User u = new User();
        u.setFullName("Officer");
        u.setDistrict(galle);
        em.persist(u);
        Warning w = new Warning();
        w.setHazardEvent(h);
        w.setSeverity(Severity.HIGH);
        w.setStatus(status);
        w.setIssuedBy(u);
        em.persist(w);
    }
}
