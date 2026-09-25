package com.group18.dewecs.repository;

import com.group18.dewecs.domain.District;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DistrictRepositoryTest {

    @Autowired
    private DistrictRepository districtRepository;

    @Test
    void savesAndReloadsADistrictByGeneratedId() {
        District district = new District();
        district.setName("Colombo");

        District saved = districtRepository.save(district);

        assertThat(saved.getId()).isNotNull();
        assertThat(districtRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(District::getName)
                .isEqualTo("Colombo");
    }
}
