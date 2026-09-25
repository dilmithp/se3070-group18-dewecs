package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitizenRepository extends JpaRepository<Citizen, Long> {
}
