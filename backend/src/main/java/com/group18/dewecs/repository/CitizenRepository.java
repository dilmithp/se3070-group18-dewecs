package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CitizenRepository extends JpaRepository<Citizen, Long> {

    Optional<Citizen> findByNicIgnoreCase(String nic);

    long countByDistrict_Id(Long districtId);
}
