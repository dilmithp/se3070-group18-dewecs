package com.group18.dewecs.repository;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class WarningRepositoryTest {

    @Autowired
    private WarningRepository warningRepository;

    @Autowired
    private HazardEventRepository hazardEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Test
    void persistsWarningWithBroadcastChannelsAndIssuingUser() {
        District district = districtRepository.save(districtOf("Kandy"));

        HazardEvent hazardEvent = new HazardEvent();
        hazardEvent.setHazardType(HazardType.LANDSLIDE);
        hazardEvent.setSeverityLevel(Severity.CRITICAL);
        hazardEvent.setStatus(HazardEventStatus.ACTIVE);
        hazardEvent.setDistrict(district);
        hazardEvent.setOccurredAt(LocalDateTime.now());
        hazardEvent = hazardEventRepository.save(hazardEvent);

        User officer = new User();
        officer.setFullName("Disaster Officer");
        officer.setDistrict(district);
        officer = userRepository.save(officer);

        Warning warning = new Warning();
        warning.setHazardEvent(hazardEvent);
        warning.setSeverity(Severity.CRITICAL);
        warning.setStatus(WarningStatus.ISSUED);
        warning.setBroadcastChannels(Set.of(BroadcastChannel.SMS, BroadcastChannel.SIREN));
        warning.setIssuedAt(LocalDateTime.now());
        warning.setIssuedBy(officer);

        Warning saved = warningRepository.save(warning);

        Warning reloaded = warningRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBroadcastChannels())
                .containsExactlyInAnyOrder(BroadcastChannel.SMS, BroadcastChannel.SIREN);
        assertThat(reloaded.getIssuedBy().getFullName()).isEqualTo("Disaster Officer");
        assertThat(reloaded.getHazardEvent().getHazardType()).isEqualTo(HazardType.LANDSLIDE);
    }

    private District districtOf(String name) {
        District district = new District();
        district.setName(name);
        return district;
    }
}
