package com.group18.dewecs.repository;

import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.HazardEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HazardEventRepository extends JpaRepository<HazardEvent, Long> {

    List<HazardEvent> findAllByOrderByOccurredAtDescIdDesc();

    List<HazardEvent> findByStatusOrderByOccurredAtDescIdDesc(HazardEventStatus status);
}
