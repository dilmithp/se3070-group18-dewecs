package com.group18.dewecs.service;

import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.OrganizationType;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.repository.ConsignmentItemRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.impl.ReliefDistributionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReliefDistributionServiceImplTest {

    @Mock
    private ReliefConsignmentRepository consignmentRepository;
    @Mock
    private ConsignmentItemRepository consignmentItemRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ShelterRepository shelterRepository;

    private ReliefDistributionServiceImpl reliefDistributionService;
    private District district;
    private Organization organization;
    private Shelter shelter;

    @BeforeEach
    void setUp() {
        reliefDistributionService = new ReliefDistributionServiceImpl(consignmentRepository,
                consignmentItemRepository, resourceRepository, shelterRepository);

        district = new District();
        district.setId(1L);
        district.setName("Colombo");

        organization = new Organization();
        organization.setId(2L);
        organization.setName("Red Cross");
        organization.setType(OrganizationType.NGO);

        shelter = new Shelter();
        shelter.setId(3L);
        shelter.setName("Town Hall");
        shelter.setDistrict(district);
        shelter.setOrganization(organization);
        shelter.setCapacity(100);
        shelter.setCurrentOccupancy(0);
        shelter.setStatus(ShelterStatus.OPEN);
    }

    @Test
    void create_withQuantityOverStock_throwsValidationException() {
        Resource resource = resourceOf(10);
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(resource));
        when(shelterRepository.findById(3L)).thenReturn(Optional.of(shelter));

        assertThatThrownBy(() -> reliefDistributionService.create(20L, 3L, 11))
                .isInstanceOf(ReliefValidationException.class)
                .hasMessageContaining("stock");
    }

    @Test
    void create_withExactStock_succeedsAndZeroesStock() {
        Resource resource = resourceOf(10);
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(resource));
        when(shelterRepository.findById(3L)).thenReturn(Optional.of(shelter));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consignmentRepository.save(any(ReliefConsignment.class))).thenAnswer(inv -> {
            ReliefConsignment c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(99L);
            }
            return c;
        });
        when(consignmentItemRepository.save(any(ConsignmentItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ReliefConsignment consignment = reliefDistributionService.create(20L, 3L, 10);

        assertThat(resource.getQuantity()).isZero();
        assertThat(consignment.getStatus()).isEqualTo(ConsignmentStatus.DISPATCHED);
        assertThat(consignment.getItems()).hasSize(1);
        assertThat(consignment.getItems().get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    void deliver_onDispatchedDistribution_setsDeliveredAt() {
        ReliefConsignment consignment = consignmentOf(ConsignmentStatus.DISPATCHED, 5);
        when(consignmentRepository.findById(99L)).thenReturn(Optional.of(consignment));
        when(consignmentRepository.save(any(ReliefConsignment.class))).thenAnswer(inv -> inv.getArgument(0));

        ReliefConsignment delivered = reliefDistributionService.deliver(99L);

        assertThat(delivered.getStatus()).isEqualTo(ConsignmentStatus.DELIVERED);
        assertThat(delivered.getDeliveredAt()).isNotNull();
    }

    @Test
    void deliver_onAlreadyDeliveredDistribution_throwsValidationException() {
        ReliefConsignment consignment = consignmentOf(ConsignmentStatus.DELIVERED, 5);
        when(consignmentRepository.findById(99L)).thenReturn(Optional.of(consignment));

        assertThatThrownBy(() -> reliefDistributionService.deliver(99L))
                .isInstanceOf(ReliefValidationException.class);
    }

    @Test
    void cancel_onDispatchedDistribution_restoresStock() {
        Resource resource = resourceOf(5);
        ReliefConsignment consignment = consignmentOf(ConsignmentStatus.DISPATCHED, 5);
        consignment.getItems().get(0).setResource(resource);

        when(consignmentRepository.findById(99L)).thenReturn(Optional.of(consignment));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consignmentRepository.save(any(ReliefConsignment.class))).thenAnswer(inv -> inv.getArgument(0));

        ReliefConsignment cancelled = reliefDistributionService.cancel(99L);

        assertThat(cancelled.getStatus()).isEqualTo(ConsignmentStatus.CANCELLED);
        assertThat(resource.getQuantity()).isEqualTo(10);
    }

    @Test
    void cancel_onDeliveredDistribution_throwsValidationExceptionAndDoesNotRestoreStock() {
        Resource resource = resourceOf(5);
        ReliefConsignment consignment = consignmentOf(ConsignmentStatus.DELIVERED, 5);
        consignment.getItems().get(0).setResource(resource);

        when(consignmentRepository.findById(99L)).thenReturn(Optional.of(consignment));

        assertThatThrownBy(() -> reliefDistributionService.cancel(99L))
                .isInstanceOf(ReliefValidationException.class);
        assertThat(resource.getQuantity()).isEqualTo(5);
    }

    private Resource resourceOf(int quantity) {
        Resource resource = new Resource();
        resource.setId(20L);
        resource.setName("Rice");
        resource.setType(ResourceType.FOOD);
        resource.setUnit("kg");
        resource.setQuantity(quantity);
        resource.setDistrict(district);
        resource.setOrganization(organization);
        return resource;
    }

    private ReliefConsignment consignmentOf(ConsignmentStatus status, int itemQuantity) {
        ReliefConsignment consignment = new ReliefConsignment();
        consignment.setId(99L);
        consignment.setOrganization(organization);
        consignment.setShelter(shelter);
        consignment.setStatus(status);
        consignment.setDispatchedAt(LocalDateTime.now().minusHours(1));

        ConsignmentItem item = new ConsignmentItem();
        item.setId(500L);
        item.setConsignment(consignment);
        item.setResource(resourceOf(0));
        item.setQuantity(itemQuantity);
        consignment.getItems().add(item);

        return consignment;
    }
}
