package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarningRepository extends JpaRepository<Warning, Long> {
}
