package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ConsignmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsignmentEventRepository extends JpaRepository<ConsignmentEvent, Long> {

    List<ConsignmentEvent> findByConsignment_IdOrderByCreatedAtAscIdAsc(Long consignmentId);
}
