package com.group18.dewecs.web;

import com.group18.dewecs.domain.Citizen;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.RescueTeamRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Base for the MockMvc flow tests: full Spring context on H2 (test profile), each test rolled back.
 * Tests build their own rows through the repositories; the demo loader is not active here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class FlowTestSupport {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected DistrictRepository districtRepository;
    @Autowired
    protected OrganizationRepository organizationRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CitizenRepository citizenRepository;
    @Autowired
    protected HazardEventRepository hazardEventRepository;
    @Autowired
    protected GroundReportRepository groundReportRepository;
    @Autowired
    protected ShelterRepository shelterRepository;
    @Autowired
    protected RescueTeamRepository rescueTeamRepository;
    @Autowired
    protected ResourceRepository resourceRepository;

    protected District district() {
        District d = new District();
        d.setName("Test District");
        return districtRepository.save(d);
    }

    protected Organization organization() {
        Organization o = new Organization();
        o.setName("Test Org");
        o.setType(OrganizationType.NGO);
        return organizationRepository.save(o);
    }

    protected User officer(District district) {
        User u = new User();
        u.setFullName("Test Officer");
        u.setPhone("0710000000");
        u.setDistrict(district);
        return userRepository.save(u);
    }

    protected HazardEvent hazardEvent(District district) {
        HazardEvent h = new HazardEvent();
        h.setHazardType(HazardType.FLOOD);
        h.setSeverityLevel(Severity.HIGH);
        h.setStatus(HazardEventStatus.ACTIVE);
        h.setDistrict(district);
        h.setOccurredAt(LocalDateTime.now().minusHours(1));
        return hazardEventRepository.save(h);
    }

    protected GroundReport groundReport(District district, GroundReportStatus status) {
        Citizen c = new Citizen();
        c.setFullName("Test Citizen");
        c.setPhone("0720000000");
        c.setDistrict(district);
        c.setNic("NIC" + System.nanoTime());
        citizenRepository.save(c);

        GroundReport r = new GroundReport();
        r.setReportedBy(c);
        r.setDistrict(district);
        r.setCategory(HazardType.FLOOD);
        r.setDescription("Water rising");
        r.setStatus(status);
        r.setGpsLat(new java.math.BigDecimal("6.9"));
        r.setGpsLng(new java.math.BigDecimal("79.8"));
        r.setSubmittedAt(LocalDateTime.now());
        return groundReportRepository.save(r);
    }

    protected Shelter shelter(District district, Organization org, int capacity) {
        Shelter s = new Shelter();
        s.setName("Test Shelter");
        s.setDistrict(district);
        s.setOrganization(org);
        s.setCapacity(capacity);
        s.setCurrentOccupancy(0);
        s.setStatus(ShelterStatus.OPEN);
        return shelterRepository.save(s);
    }

    protected RescueTeam team(District district, Organization org, RescueTeamStatus status) {
        RescueTeam t = new RescueTeam();
        t.setName("Test Team");
        t.setDistrict(district);
        t.setOrganization(org);
        t.setStatus(status);
        return rescueTeamRepository.save(t);
    }

    protected Resource supply(District district, Organization org, int quantity) {
        Resource r = new Resource();
        r.setName("Test Rice");
        r.setType(ResourceType.FOOD);
        r.setUnit("kg");
        r.setQuantity(quantity);
        r.setDistrict(district);
        r.setOrganization(org);
        return resourceRepository.save(r);
    }
}
