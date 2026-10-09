package com.group18.dewecs.repository;

import com.group18.dewecs.domain.CoordinationEvent;
import com.group18.dewecs.domain.CoordinationEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoordinationEventRepository extends JpaRepository<CoordinationEvent, Long> {

    List<CoordinationEvent> findTop40ByDistrict_IdOrderByCreatedAtDescIdDesc(Long districtId);

    Optional<CoordinationEvent> findFirstBySubjectAndTypeOrderByCreatedAtDescIdDesc(String subject, CoordinationEventType type);
}
