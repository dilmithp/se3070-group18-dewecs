package com.group18.dewecs.repository;

import com.group18.dewecs.domain.NeedStatus;
import com.group18.dewecs.domain.ShelterNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShelterNeedRepository extends JpaRepository<ShelterNeed, Long> {

    /** Needs of the shelters of a district, urgent first, then oldest first. */
    List<ShelterNeed> findByShelter_District_IdAndStatusOrderByUrgentDescRequestedAtAscIdAsc(Long districtId, NeedStatus status);

    List<ShelterNeed> findByShelter_District_IdOrderByRequestedAtAscIdAsc(Long districtId);
}
