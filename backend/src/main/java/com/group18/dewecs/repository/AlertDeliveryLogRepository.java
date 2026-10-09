package com.group18.dewecs.repository;

import com.group18.dewecs.domain.AlertDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertDeliveryLogRepository extends JpaRepository<AlertDeliveryLog, Long> {

    List<AlertDeliveryLog> findByWarning_IdOrderByCreatedAtAscIdAsc(Long warningId);
}
