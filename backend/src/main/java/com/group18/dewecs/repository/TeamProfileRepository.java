package com.group18.dewecs.repository;

import com.group18.dewecs.domain.TeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamProfileRepository extends JpaRepository<TeamProfile, Long> {

    Optional<TeamProfile> findByTeam_Id(Long teamId);

    List<TeamProfile> findByTeam_IdIn(Collection<Long> teamIds);
}
