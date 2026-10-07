package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.ConsignmentItemRepository;
import com.group18.dewecs.repository.ReliefConsignmentRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.ReliefDistributionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReliefDistributionServiceImpl implements ReliefDistributionService {

    private final ReliefConsignmentRepository consignmentRepository;
    private final ConsignmentItemRepository consignmentItemRepository;
    private final ResourceRepository resourceRepository;
    private final ShelterRepository shelterRepository;

    public ReliefDistributionServiceImpl(ReliefConsignmentRepository consignmentRepository,
                                          ConsignmentItemRepository consignmentItemRepository,
                                          ResourceRepository resourceRepository,
                                          ShelterRepository shelterRepository) {
        this.consignmentRepository = consignmentRepository;
        this.consignmentItemRepository = consignmentItemRepository;
        this.resourceRepository = resourceRepository;
        this.shelterRepository = shelterRepository;
    }

    @Override
    @Transactional
    public ReliefConsignment create(Long resourceId, Long shelterId, Integer quantity) {
        Resource resource = findResource(resourceId);
        Shelter shelter = findShelter(shelterId);

        if (quantity == null || quantity <= 0) {
            throw new ReliefValidationException("Quantity must be a positive number.");
        }
        if (quantity > resource.getQuantity()) {
            throw new ReliefValidationException("Requested quantity exceeds available stock ("
                    + resource.getQuantity() + " " + resource.getUnit() + " available).");
        }

        resource.setQuantity(resource.getQuantity() - quantity);
        resourceRepository.save(resource);

        ReliefConsignment consignment = new ReliefConsignment();
        consignment.setOrganization(resource.getOrganization());
        consignment.setShelter(shelter);
        consignment.setStatus(ConsignmentStatus.DISPATCHED);
        consignment.setDispatchedAt(LocalDateTime.now());
        consignment = consignmentRepository.save(consignment);

        ConsignmentItem item = new ConsignmentItem();
        item.setConsignment(consignment);
        item.setResource(resource);
        item.setQuantity(quantity);
        item = consignmentItemRepository.save(item);
        consignment.getItems().add(item);

        return consignment;
    }

    @Override
    @Transactional
    public ReliefConsignment deliver(Long distributionId) {
        ReliefConsignment consignment = findConsignment(distributionId);
        if (consignment.getStatus() != ConsignmentStatus.DISPATCHED) {
            throw new ReliefValidationException("Only dispatched distributions can be marked delivered.");
        }
        consignment.setStatus(ConsignmentStatus.DELIVERED);
        consignment.setDeliveredAt(LocalDateTime.now());
        return consignmentRepository.save(consignment);
    }

    @Override
    @Transactional
    public ReliefConsignment cancel(Long distributionId) {
        ReliefConsignment consignment = findConsignment(distributionId);
        if (consignment.getStatus() != ConsignmentStatus.DISPATCHED) {
            throw new ReliefValidationException("Only dispatched (not yet delivered) distributions can be cancelled.");
        }

        for (ConsignmentItem item : consignment.getItems()) {
            Resource resource = item.getResource();
            resource.setQuantity(resource.getQuantity() + item.getQuantity());
            resourceRepository.save(resource);
        }

        consignment.setStatus(ConsignmentStatus.CANCELLED);
        return consignmentRepository.save(consignment);
    }

    @Override
    public ReliefConsignment getById(Long distributionId) {
        return findConsignment(distributionId);
    }

    @Override
    public List<ReliefConsignment> list(ConsignmentStatus statusFilter, Long shelterId, Long resourceId) {
        return consignmentRepository.search(statusFilter, shelterId, resourceId);
    }

    @Override
    public List<Resource> listSuppliesForSelection() {
        return resourceRepository.findAll();
    }

    @Override
    public List<Shelter> listSheltersForSelection() {
        return shelterRepository.findAll();
    }

    private ReliefConsignment findConsignment(Long id) {
        return consignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relief distribution not found: " + id));
    }

    private Resource findResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relief supply not found: " + id));
    }

    private Shelter findShelter(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }
}
