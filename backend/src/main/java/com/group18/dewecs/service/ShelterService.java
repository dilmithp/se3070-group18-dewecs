package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterOccupant;
import com.group18.dewecs.domain.ShelterStatus;

import java.util.List;

public interface ShelterService {

    Shelter create(Long districtId, Long organizationId, String name, Integer capacity);

    Shelter update(Long shelterId, String name, Integer capacity);

    Shelter close(Long shelterId);

    Shelter reopen(Long shelterId);

    ShelterOccupant checkIn(Long shelterId, String fullName, String nic);

    void checkOut(Long shelterId, Long occupantId);

    Shelter getById(Long shelterId);

    List<ShelterOccupant> listCurrentOccupants(Long shelterId);

    /** Either filter may be null to mean "no filter on that dimension". */
    List<Shelter> list(ShelterStatus statusFilter, Long districtId);

    List<Shelter> listAvailable();

    List<District> listDistricts();

    List<Organization> listOrganizations();
}
