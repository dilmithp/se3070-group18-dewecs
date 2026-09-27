package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterOccupant;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.exception.ShelterValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ShelterOccupantRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.impl.ShelterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelterServiceImplTest {

    @Mock
    private ShelterRepository shelterRepository;
    @Mock
    private ShelterOccupantRepository shelterOccupantRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private ShelterServiceImpl shelterService;
    private District district;
    private Organization organization;

    @BeforeEach
    void setUp() {
        shelterService = new ShelterServiceImpl(shelterRepository, shelterOccupantRepository, districtRepository,
                organizationRepository);

        district = new District();
        district.setId(1L);
        district.setName("Colombo");

        organization = new Organization();
        organization.setId(2L);
        organization.setName("Red Cross");
        organization.setType(OrganizationType.NGO);
    }

    @Test
    void create_withInvalidCapacity_throwsValidationException() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(organization));

        assertThatThrownBy(() -> shelterService.create(1L, 2L, "Town Hall", 0))
                .isInstanceOf(ShelterValidationException.class);
    }

    @Test
    void checkIn_lastAvailableSlot_flipsStatusToFull() {
        Shelter shelter = shelterOf(2, 1);
        when(shelterRepository.findById(50L)).thenReturn(Optional.of(shelter));
        when(shelterOccupantRepository.save(any(ShelterOccupant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shelterRepository.save(any(Shelter.class))).thenAnswer(inv -> inv.getArgument(0));

        shelterService.checkIn(50L, "Jane Doe", "123456789V");

        assertThat(shelter.getCurrentOccupancy()).isEqualTo(2);
        assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.FULL);
    }

    @Test
    void checkIn_whenAlreadyAtCapacity_throwsValidationException() {
        Shelter shelter = shelterOf(2, 2);
        shelter.setStatus(ShelterStatus.FULL);
        when(shelterRepository.findById(50L)).thenReturn(Optional.of(shelter));

        assertThatThrownBy(() -> shelterService.checkIn(50L, "Jane Doe", "123456789V"))
                .isInstanceOf(ShelterValidationException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void checkIn_whenClosed_throwsValidationException() {
        Shelter shelter = shelterOf(10, 0);
        shelter.setStatus(ShelterStatus.CLOSED);
        when(shelterRepository.findById(50L)).thenReturn(Optional.of(shelter));

        assertThatThrownBy(() -> shelterService.checkIn(50L, "Jane Doe", "123456789V"))
                .isInstanceOf(ShelterValidationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void checkOut_whenFull_flipsStatusBackToOpen() {
        Shelter shelter = shelterOf(2, 2);
        shelter.setStatus(ShelterStatus.FULL);

        ShelterOccupant occupant = new ShelterOccupant();
        occupant.setId(7L);
        occupant.setShelter(shelter);
        occupant.setFullName("Jane Doe");
        occupant.setNic("123456789V");

        when(shelterRepository.findById(50L)).thenReturn(Optional.of(shelter));
        when(shelterOccupantRepository.findById(7L)).thenReturn(Optional.of(occupant));
        when(shelterOccupantRepository.save(any(ShelterOccupant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shelterRepository.save(any(Shelter.class))).thenAnswer(inv -> inv.getArgument(0));

        shelterService.checkOut(50L, 7L);

        assertThat(shelter.getCurrentOccupancy()).isEqualTo(1);
        assertThat(shelter.getStatus()).isEqualTo(ShelterStatus.OPEN);
        assertThat(occupant.getCheckOutTime()).isNotNull();
    }

    @Test
    void reopen_onNonClosedShelter_throwsValidationException() {
        Shelter shelter = shelterOf(10, 3);
        shelter.setStatus(ShelterStatus.OPEN);
        when(shelterRepository.findById(50L)).thenReturn(Optional.of(shelter));

        assertThatThrownBy(() -> shelterService.reopen(50L))
                .isInstanceOf(ShelterValidationException.class);
    }

    private Shelter shelterOf(int capacity, int currentOccupancy) {
        Shelter shelter = new Shelter();
        shelter.setId(50L);
        shelter.setName("Town Hall");
        shelter.setDistrict(district);
        shelter.setOrganization(organization);
        shelter.setCapacity(capacity);
        shelter.setCurrentOccupancy(currentOccupancy);
        shelter.setStatus(ShelterStatus.OPEN);
        return shelter;
    }
}
