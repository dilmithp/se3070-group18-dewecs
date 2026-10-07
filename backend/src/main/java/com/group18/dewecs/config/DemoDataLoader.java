package com.group18.dewecs.config;

import com.group18.dewecs.domain.BroadcastChannel;
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
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterOccupant;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.repository.ConsignmentItemRepository;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.GroundReportRepository;
import com.group18.dewecs.repository.HazardEventRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.RescueRequestRepository;
import com.group18.dewecs.repository.RescueTeamRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterOccupantRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.repository.UserRepository;
import com.group18.dewecs.repository.WarningRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Seeds the in-memory H2 database of the {@code local} profile so every screen has something to show.
 * Idempotent: does nothing if districts already exist. Never active on the default, dev or test profiles.
 */
@Component
@Profile("local")
public class DemoDataLoader implements CommandLineRunner {

    private final DistrictRepository districts;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final CitizenRepository citizens;
    private final HazardEventRepository hazardEvents;
    private final GroundReportRepository groundReports;
    private final WarningRepository warnings;
    private final ShelterRepository shelters;
    private final ShelterOccupantRepository occupants;
    private final RescueTeamRepository rescueTeams;
    private final RescueRequestRepository rescueRequests;
    private final ResourceRepository resources;
    private final ReliefConsignmentRepository consignments;
    private final ConsignmentItemRepository consignmentItems;

    public DemoDataLoader(DistrictRepository districts, OrganizationRepository organizations,
                          UserRepository users, CitizenRepository citizens,
                          HazardEventRepository hazardEvents, GroundReportRepository groundReports,
                          WarningRepository warnings, ShelterRepository shelters,
                          ShelterOccupantRepository occupants, RescueTeamRepository rescueTeams,
                          RescueRequestRepository rescueRequests, ResourceRepository resources,
                          ReliefConsignmentRepository consignments, ConsignmentItemRepository consignmentItems) {
        this.districts = districts;
        this.organizations = organizations;
        this.users = users;
        this.citizens = citizens;
        this.hazardEvents = hazardEvents;
        this.groundReports = groundReports;
        this.warnings = warnings;
        this.shelters = shelters;
        this.occupants = occupants;
        this.rescueTeams = rescueTeams;
        this.rescueRequests = rescueRequests;
        this.resources = resources;
        this.consignments = consignments;
        this.consignmentItems = consignmentItems;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (districts.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        District colombo = district("Colombo");
        District galle = district("Galle");
        District kandy = district("Kandy");

        Organization dmc = organization("Disaster Management Centre", OrganizationType.GOVERNMENT);
        Organization army = organization("Sri Lanka Army", OrganizationType.ARMED_FORCES);
        Organization redCross = organization("Sri Lanka Red Cross", OrganizationType.NGO);
        Organization donor = organization("Lanka Relief Foundation", OrganizationType.PRIVATE_DONOR);

        User officer = user("Nadeesha Fernando", "0711000001", colombo);
        user("Ruwan Jayasinghe", "0711000002", galle);
        Citizen kasun = citizen("Kasun Perera", "0772000001", galle, "199012345671");
        Citizen amaya = citizen("Amaya Silva", "0772000002", kandy, "199523456782");
        Citizen tharindu = citizen("Tharindu Bandara", "0772000003", colombo, "198834567893");

        HazardEvent flood = hazard(HazardType.FLOOD, Severity.HIGH, galle, now.minusHours(10));
        HazardEvent landslide = hazard(HazardType.LANDSLIDE, Severity.CRITICAL, kandy, now.minusHours(4));
        hazard(HazardType.CYCLONE, Severity.MODERATE, colombo, now.minusDays(1));

        report(kasun, galle, HazardType.FLOOD, "River level rising fast near Wakwella bridge",
                GroundReportStatus.PENDING_REVIEW, null, now.minusHours(3));
        report(amaya, kandy, HazardType.LANDSLIDE, "Cracks on the hillside above the Peradeniya road",
                GroundReportStatus.PENDING_REVIEW, null, now.minusHours(2));
        report(tharindu, colombo, HazardType.FLOOD, "Road under 1 m of water in Kolonnawa",
                GroundReportStatus.VERIFIED, officer, now.minusHours(6));
        report(kasun, galle, HazardType.FLOOD, "Offline report waiting to sync from the mobile app",
                GroundReportStatus.PENDING_SYNC, null, now.minusHours(1));

        warning(flood, Severity.HIGH, WarningStatus.DRAFT, "Flood risk along the Gin river: prepare to evacuate.",
                now.plusHours(12), null, officer, Set.of(BroadcastChannel.SMS, BroadcastChannel.RADIO));
        warning(landslide, Severity.CRITICAL, WarningStatus.ISSUED,
                "Landslide warning for Kandy hill slopes: evacuate now.",
                now.plusHours(24), now.minusHours(1), officer, Set.of(BroadcastChannel.SMS, BroadcastChannel.SIREN));
        warning(flood, Severity.MODERATE, WarningStatus.ISSUED,
                "Earlier flood advisory for Galle (already past its expiry time).",
                now.minusMinutes(30), now.minusHours(8), officer, Set.of(BroadcastChannel.EMAIL));

        Shelter nearlyFull = shelter("Galle Central College", galle, redCross, 10, 9, ShelterStatus.OPEN);
        Shelter full = shelter("Kandy Town Hall", kandy, army, 5, 5, ShelterStatus.FULL);
        Shelter spacious = shelter("Colombo Sports Complex", colombo, dmc, 150, 0, ShelterStatus.OPEN);
        occupantsFor(nearlyFull, 9, "2001", now);
        occupantsFor(full, 5, "2002", now);

        RescueTeam alpha = team("Alpha Rescue Unit", galle, army, RescueTeamStatus.AVAILABLE);
        team("Bravo Boat Team", colombo, redCross, RescueTeamStatus.AVAILABLE);
        RescueTeam charlie = team("Charlie Medical Team", kandy, redCross, RescueTeamStatus.DISPATCHED);

        request(galle, "Sumith Gunawardena", "0763000001", "Family of five stranded on the roof",
                Severity.CRITICAL, RescueRequestStatus.PENDING, null, now.minusMinutes(50));
        request(colombo, "Dilani Rathnayake", "0763000002", "Elderly resident needs boat evacuation",
                Severity.HIGH, RescueRequestStatus.PENDING, null, now.minusMinutes(35));
        request(kandy, "Mahesh Wijesinghe", "0763000003", "Injured hiker trapped near the landslide",
                Severity.HIGH, RescueRequestStatus.ASSIGNED, charlie, now.minusHours(2));
        request(galle, "Priya Kumari", "0763000004", "Livestock and family cut off by flood water",
                Severity.MODERATE, RescueRequestStatus.COMPLETED, alpha, now.minusHours(9));

        // Quantities are already net of the DISPATCHED and DELIVERED distributions seeded below.
        Resource rice = supply("Rice (50 kg bags)", ResourceType.FOOD, "bags", 120, galle, dmc);
        Resource water = supply("Bottled water", ResourceType.WATER, "cases", 300, colombo, donor);
        Resource medkits = supply("First-aid kits", ResourceType.MEDICAL_SUPPLIES, "kits", 6, kandy, redCross);
        supply("Tarpaulins", ResourceType.SHELTER_MATERIALS, "sheets", 45, kandy, army);

        distribution(rice, nearlyFull, 20, ConsignmentStatus.DISPATCHED, now.minusHours(2));
        distribution(water, spacious, 50, ConsignmentStatus.DELIVERED, now.minusHours(7));
        distribution(medkits, full, 10, ConsignmentStatus.CANCELLED, now.minusHours(5));
    }

    private District district(String name) {
        District d = new District();
        d.setName(name);
        return districts.save(d);
    }

    private Organization organization(String name, OrganizationType type) {
        Organization o = new Organization();
        o.setName(name);
        o.setType(type);
        return organizations.save(o);
    }

    private User user(String fullName, String phone, District district) {
        User u = new User();
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setDistrict(district);
        return users.save(u);
    }

    private Citizen citizen(String fullName, String phone, District district, String nic) {
        Citizen c = new Citizen();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setDistrict(district);
        c.setNic(nic);
        return citizens.save(c);
    }

    private HazardEvent hazard(HazardType type, Severity severity, District district, LocalDateTime occurredAt) {
        HazardEvent h = new HazardEvent();
        h.setHazardType(type);
        h.setSeverityLevel(severity);
        h.setStatus(HazardEventStatus.ACTIVE);
        h.setDistrict(district);
        h.setOccurredAt(occurredAt);
        return hazardEvents.save(h);
    }

    private void report(Citizen by, District district, HazardType category, String description,
                        GroundReportStatus status, User verifiedBy, LocalDateTime at) {
        GroundReport r = new GroundReport();
        r.setReportedBy(by);
        r.setDistrict(district);
        r.setCategory(category);
        r.setDescription(description);
        r.setStatus(status);
        r.setVerifiedBy(verifiedBy);
        r.setGpsLat(new BigDecimal("6.0329"));
        r.setGpsLng(new BigDecimal("80.2168"));
        r.setSubmittedAt(at);
        groundReports.save(r);
    }

    private void warning(HazardEvent event, Severity severity, WarningStatus status, String message,
                         LocalDateTime expiresAt, LocalDateTime issuedAt, User by, Set<BroadcastChannel> channels) {
        Warning w = new Warning();
        w.setHazardEvent(event);
        w.setSeverity(severity);
        w.setStatus(status);
        w.setMessage(message);
        w.setExpiresAt(expiresAt);
        w.setIssuedAt(issuedAt);
        w.setIssuedBy(by);
        w.setBroadcastChannels(new HashSet<>(channels));
        warnings.save(w);
    }

    private Shelter shelter(String name, District district, Organization org, int capacity, int occupancy,
                            ShelterStatus status) {
        Shelter s = new Shelter();
        s.setName(name);
        s.setDistrict(district);
        s.setOrganization(org);
        s.setCapacity(capacity);
        s.setCurrentOccupancy(occupancy);
        s.setStatus(status);
        return shelters.save(s);
    }

    private void occupantsFor(Shelter shelter, int count, String nicPrefix, LocalDateTime now) {
        for (int i = 1; i <= count; i++) {
            ShelterOccupant o = new ShelterOccupant();
            o.setShelter(shelter);
            o.setFullName(shelter.getName() + " evacuee " + i);
            o.setNic(String.format("%s%08d", nicPrefix, i));
            o.setCheckInTime(now.minusHours(3));
            occupants.save(o);
        }
    }

    private RescueTeam team(String name, District district, Organization org, RescueTeamStatus status) {
        RescueTeam t = new RescueTeam();
        t.setName(name);
        t.setDistrict(district);
        t.setOrganization(org);
        t.setStatus(status);
        return rescueTeams.save(t);
    }

    private void request(District district, String name, String phone, String description, Severity priority,
                         RescueRequestStatus status, RescueTeam team, LocalDateTime submittedAt) {
        RescueRequest r = new RescueRequest();
        r.setDistrict(district);
        r.setRequesterName(name);
        r.setRequesterPhone(phone);
        r.setDescription(description);
        r.setPriority(priority);
        r.setStatus(status);
        r.setAssignedTeam(team);
        r.setSubmittedAt(submittedAt);
        if (team != null) {
            r.setAssignedAt(submittedAt.plusMinutes(15));
        }
        if (status == RescueRequestStatus.COMPLETED) {
            r.setCompletedAt(submittedAt.plusHours(2));
        }
        rescueRequests.save(r);
    }

    private Resource supply(String name, ResourceType type, String unit, int quantity, District district,
                            Organization org) {
        Resource r = new Resource();
        r.setName(name);
        r.setType(type);
        r.setUnit(unit);
        r.setQuantity(quantity);
        r.setDistrict(district);
        r.setOrganization(org);
        return resources.save(r);
    }

    private void distribution(Resource resource, Shelter shelter, int quantity, ConsignmentStatus status,
                              LocalDateTime dispatchedAt) {
        ReliefConsignment c = new ReliefConsignment();
        c.setOrganization(resource.getOrganization());
        c.setShelter(shelter);
        c.setStatus(status);
        c.setDispatchedAt(dispatchedAt);
        if (status == ConsignmentStatus.DELIVERED) {
            c.setDeliveredAt(dispatchedAt.plusHours(2));
        }
        c = consignments.save(c);
        ConsignmentItem item = new ConsignmentItem();
        item.setConsignment(c);
        item.setResource(resource);
        item.setQuantity(quantity);
        consignmentItems.save(item);
    }
}
