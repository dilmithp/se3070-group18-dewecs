package com.group18.dewecs.repository;

import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.RescueTeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueTeamRepository extends JpaRepository<RescueTeam, Long> {

    List<RescueTeam> findByStatus(RescueTeamStatus status);

    List<RescueTeam> findByDistrict_Id(Long districtId);
}
