package com.group18.dewecs.repository;

import com.group18.dewecs.domain.AgencyNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgencyNotificationRepository extends JpaRepository<AgencyNotification, Long> {

    List<AgencyNotification> findTop100ByOrderByCreatedAtDescIdDesc();
}
