package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ShelterOccupant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShelterOccupantRepository extends JpaRepository<ShelterOccupant, Long> {

    List<ShelterOccupant> findByShelter_IdAndCheckOutTimeIsNull(Long shelterId);
}
