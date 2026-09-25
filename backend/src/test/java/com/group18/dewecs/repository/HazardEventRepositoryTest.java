package com.group18.dewecs.repository;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HazardEventRepositoryTest {

    @Autowired
    private HazardEventRepository hazardEventRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Test
    void persistsHazardEventWithDistrictAndEnumFields() {
        District district = new District();
        district.setName("Galle");
        district = districtRepository.save(district);

        HazardEvent event = new HazardEvent();
        event.setHazardType(HazardType.FLOOD);
        event.setSeverityLevel(Severity.HIGH);
        event.setStatus(HazardEventStatus.ACTIVE);
        event.setDistrict(district);
        event.setOccurredAt(LocalDateTime.of(2026, 5, 1, 8, 30));

        HazardEvent saved = hazardEventRepository.save(event);

        HazardEvent reloaded = hazardEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getHazardType()).isEqualTo(HazardType.FLOOD);
        assertThat(reloaded.getSeverityLevel()).isEqualTo(Severity.HIGH);
        assertThat(reloaded.getStatus()).isEqualTo(HazardEventStatus.ACTIVE);
        assertThat(reloaded.getDistrict().getName()).isEqualTo("Galle");
    }
}
