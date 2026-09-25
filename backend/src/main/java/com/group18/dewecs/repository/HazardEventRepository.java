package com.group18.dewecs.repository;

import com.group18.dewecs.domain.HazardEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HazardEventRepository extends JpaRepository<HazardEvent, Long> {
}
