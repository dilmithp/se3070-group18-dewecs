package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterOccupant;
import com.group18.dewecs.domain.ShelterStatus;
import com.group18.dewecs.exception.ResourceNotFoundException;
import com.group18.dewecs.exception.ShelterValidationException;
import com.group18.dewecs.repository.DistrictRepository;
import com.group18.dewecs.repository.OrganizationRepository;
import com.group18.dewecs.repository.ShelterOccupantRepository;
import com.group18.dewecs.repository.ShelterRepository;
import com.group18.dewecs.service.ShelterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShelterServiceImpl implements ShelterService {

    private final ShelterRepository shelterRepository;
    private final ShelterOccupantRepository shelterOccupantRepository;
    private final DistrictRepository districtRepository;
    private final OrganizationRepository organizationRepository;

    public ShelterServiceImpl(ShelterRepository shelterRepository,
                               ShelterOccupantRepository shelterOccupantRepository,
                               DistrictRepository districtRepository,
                               OrganizationRepository organizationRepository) {
        this.shelterRepository = shelterRepository;
        this.shelterOccupantRepository = shelterOccupantRepository;
        this.districtRepository = districtRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Shelter create(Long districtId, Long organizationId, String name, Integer capacity) {
        District district = findDistrict(districtId);
        Organization organization = findOrganization(organizationId);

        if (capacity == null || capacity <= 0) {
            throw new ShelterValidationException("Capacity must be a positive number.");
        }

        Shelter shelter = new Shelter();
        shelter.setDistrict(district);
        shelter.setOrganization(organization);
        shelter.setName(name);
        shelter.setCapacity(capacity);
        shelter.setCurrentOccupancy(0);
        shelter.setStatus(ShelterStatus.OPEN);

        return shelterRepository.save(shelter);
    }

    @Override
    public Shelter update(Long shelterId, String name, Integer capacity) {
        Shelter shelter = findShelter(shelterId);

        if (capacity == null || capacity <= 0) {
            throw new ShelterValidationException("Capacity must be a positive number.");
        }
        if (capacity < shelter.getCurrentOccupancy()) {
            throw new ShelterValidationException(
                    "Capacity cannot be lower than the current occupancy (" + shelter.getCurrentOccupancy() + ").");
        }

        shelter.setName(name);
        shelter.setCapacity(capacity);
        if (shelter.getStatus() != ShelterStatus.CLOSED) {
            shelter.setStatus(shelter.getCurrentOccupancy() >= capacity ? ShelterStatus.FULL : ShelterStatus.OPEN);
        }

        return shelterRepository.save(shelter);
    }

    @Override
    public Shelter close(Long shelterId) {
        Shelter shelter = findShelter(shelterId);
        shelter.setStatus(ShelterStatus.CLOSED);
        return shelterRepository.save(shelter);
    }

    @Override
    public Shelter reopen(Long shelterId) {
        Shelter shelter = findShelter(shelterId);
        if (shelter.getStatus() != ShelterStatus.CLOSED) {
            throw new ShelterValidationException("Only a closed shelter can be reopened.");
        }
        shelter.setStatus(shelter.getCurrentOccupancy() >= shelter.getCapacity() ? ShelterStatus.FULL : ShelterStatus.OPEN);
        return shelterRepository.save(shelter);
    }

    @Override
    public ShelterOccupant checkIn(Long shelterId, String fullName, String nic) {
        Shelter shelter = findShelter(shelterId);

        if (shelter.getStatus() == ShelterStatus.CLOSED) {
            throw new ShelterValidationException("Cannot check in: shelter is closed.");
        }
        if (shelter.getCurrentOccupancy() >= shelter.getCapacity()) {
            throw new ShelterValidationException("Cannot check in: shelter is at full capacity.");
        }

        ShelterOccupant occupant = new ShelterOccupant();
        occupant.setShelter(shelter);
        occupant.setFullName(fullName);
        occupant.setNic(nic);
        occupant.setCheckInTime(LocalDateTime.now());
        occupant = shelterOccupantRepository.save(occupant);

        shelter.setCurrentOccupancy(shelter.getCurrentOccupancy() + 1);
        if (shelter.getCurrentOccupancy() >= shelter.getCapacity()) {
            shelter.setStatus(ShelterStatus.FULL);
        }
        shelterRepository.save(shelter);

        return occupant;
    }

    @Override
    public void checkOut(Long shelterId, Long occupantId) {
        Shelter shelter = findShelter(shelterId);
        ShelterOccupant occupant = shelterOccupantRepository.findById(occupantId)
                .orElseThrow(() -> new ResourceNotFoundException("Occupant not found: " + occupantId));

        if (!occupant.getShelter().getId().equals(shelterId)) {
            throw new ShelterValidationException("Occupant does not belong to this shelter.");
        }
        if (occupant.getCheckOutTime() != null) {
            throw new ShelterValidationException("Occupant has already checked out.");
        }

        occupant.setCheckOutTime(LocalDateTime.now());
        shelterOccupantRepository.save(occupant);

        shelter.setCurrentOccupancy(Math.max(0, shelter.getCurrentOccupancy() - 1));
        if (shelter.getStatus() == ShelterStatus.FULL && shelter.getCurrentOccupancy() < shelter.getCapacity()) {
            shelter.setStatus(ShelterStatus.OPEN);
        }
        shelterRepository.save(shelter);
    }

    @Override
    public Shelter getById(Long shelterId) {
        return findShelter(shelterId);
    }

    @Override
    public List<ShelterOccupant> listCurrentOccupants(Long shelterId) {
        return shelterOccupantRepository.findByShelter_IdAndCheckOutTimeIsNull(shelterId);
    }

    @Override
    public List<Shelter> list(ShelterStatus statusFilter, Long districtId) {
        if (statusFilter != null && districtId != null) {
            return shelterRepository.findByStatusAndDistrict_Id(statusFilter, districtId);
        }
        if (statusFilter != null) {
            return shelterRepository.findByStatus(statusFilter);
        }
        if (districtId != null) {
            return shelterRepository.findByDistrict_Id(districtId);
        }
        return shelterRepository.findAll();
    }

    @Override
    public List<Shelter> listAvailable() {
        return shelterRepository.findAvailable();
    }

    @Override
    public List<District> listDistricts() {
        return districtRepository.findAll();
    }

    @Override
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    private Shelter findShelter(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found: " + id));
    }

    private District findDistrict(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));
    }

    private Organization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}
