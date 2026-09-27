package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.service.impl.ReliefSupplyServiceImpl;
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
class ReliefSupplyServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private ReliefSupplyServiceImpl reliefSupplyService;
    private District district;
    private Organization organization;

    @BeforeEach
    void setUp() {
        reliefSupplyService = new ReliefSupplyServiceImpl(resourceRepository, districtRepository,
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
    void create_withNegativeQuantity_throwsValidationException() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(organization));

        assertThatThrownBy(() -> reliefSupplyService.create(1L, 2L, "Rice", "FOOD", "kg", -5))
                .isInstanceOf(ReliefValidationException.class);
    }

    @Test
    void create_withInvalidCategory_throwsValidationException() {
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(organization));

        assertThatThrownBy(() -> reliefSupplyService.create(1L, 2L, "Rice", "GRAIN", "kg", 100))
                .isInstanceOf(ReliefValidationException.class);
    }

    @Test
    void restock_addsToExistingQuantity() {
        Resource resource = resourceOf(50);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));

        Resource restocked = reliefSupplyService.restock(10L, 25);

        assertThat(restocked.getQuantity()).isEqualTo(75);
    }

    @Test
    void restock_withNonPositiveQuantity_throwsValidationException() {
        Resource resource = resourceOf(50);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> reliefSupplyService.restock(10L, 0))
                .isInstanceOf(ReliefValidationException.class);
    }

    private Resource resourceOf(int quantity) {
        Resource resource = new Resource();
        resource.setId(10L);
        resource.setName("Rice");
        resource.setType(ResourceType.FOOD);
        resource.setUnit("kg");
        resource.setQuantity(quantity);
        resource.setDistrict(district);
        resource.setOrganization(organization);
        return resource;
    }
}
