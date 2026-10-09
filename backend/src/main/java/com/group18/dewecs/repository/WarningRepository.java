package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    List<Warning> findByStatus(WarningStatus status);

    List<Warning> findByHazardEvent_District_Id(Long districtId);

    List<Warning> findByStatusAndHazardEvent_District_Id(WarningStatus status, Long districtId);

    long countByStatus(WarningStatus status);

    List<Warning> findByHazardEvent_IdAndStatusIn(Long hazardEventId, Collection<WarningStatus> statuses);
}
