package com.group18.dewecs.repository;

import com.group18.dewecs.domain.HazardEventLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HazardEventLocationRepository extends JpaRepository<HazardEventLocation, Long> {

    Optional<HazardEventLocation> findByHazardEvent_Id(Long hazardEventId);
}
