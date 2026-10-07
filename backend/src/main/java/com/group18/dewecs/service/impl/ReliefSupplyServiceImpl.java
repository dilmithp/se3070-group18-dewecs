package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.ResourceType;
import com.group18.dewecs.exception.ReliefValidationException;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ResourceRepository;
import com.group18.dewecs.service.ReliefSupplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReliefSupplyServiceImpl implements ReliefSupplyService {

    private final ResourceRepository resourceRepository;
    private final DistrictRepository districtRepository;
    private final OrganizationRepository organizationRepository;

    public ReliefSupplyServiceImpl(ResourceRepository resourceRepository,
                                    DistrictRepository districtRepository,
                                    OrganizationRepository organizationRepository) {
        this.resourceRepository = resourceRepository;
        this.districtRepository = districtRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public Resource create(Long districtId, Long organizationId, String name, String type, String unit,
                            Integer initialQuantity) {
        District district = findDistrict(districtId);
        Organization organization = findOrganization(organizationId);

        if (initialQuantity == null || initialQuantity < 0) {
            throw new ReliefValidationException("Initial quantity cannot be negative.");
        }

        Resource resource = new Resource();
        resource.setDistrict(district);
        resource.setOrganization(organization);
        resource.setName(name);
        resource.setType(parseType(type));
        resource.setUnit(unit);
        resource.setQuantity(initialQuantity);

        return resourceRepository.save(resource);
    }

    @Override
    @Transactional
    public Resource update(Long resourceId, String name, String type, String unit) {
        Resource resource = findResource(resourceId);
        resource.setName(name);
        resource.setType(parseType(type));
        resource.setUnit(unit);
        return resourceRepository.save(resource);
    }

    @Override
    @Transactional
    public Resource restock(Long resourceId, Integer additionalQuantity) {
        Resource resource = findResource(resourceId);
        if (additionalQuantity == null || additionalQuantity <= 0) {
            throw new ReliefValidationException("Restock quantity must be a positive number.");
        }
        resource.setQuantity(resource.getQuantity() + additionalQuantity);
        return resourceRepository.save(resource);
    }

    @Override
    public Resource getById(Long resourceId) {
        return findResource(resourceId);
    }

    @Override
    public List<Resource> list(ResourceType typeFilter, Long districtId) {
        if (typeFilter != null && districtId != null) {
            return resourceRepository.findByTypeAndDistrict_Id(typeFilter, districtId);
        }
        if (typeFilter != null) {
            return resourceRepository.findByType(typeFilter);
        }
        if (districtId != null) {
            return resourceRepository.findByDistrict_Id(districtId);
        }
        return resourceRepository.findAll();
    }

    @Override
    public List<Resource> listLowStock() {
        return resourceRepository.findByQuantityLessThan(Resource.LOW_STOCK_THRESHOLD);
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    private Resource findResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relief supply not found: " + id));
    }

    private District findDistrict(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }

    private ResourceType parseType(String raw) {
        try {
            return ResourceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ReliefValidationException("Invalid supply category: " + raw);
        }
    }
}
