package com.group18.dewecs.repository;

import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    List<Shelter> findByStatus(ShelterStatus status);

    List<Shelter> findByDistrict_Id(Long districtId);

    List<Shelter> findByStatusAndDistrict_Id(ShelterStatus status, Long districtId);

    @Query("SELECT s FROM Shelter s WHERE s.currentOccupancy < s.capacity AND s.status <> "
            + "com.group18.dewecs.domain.ShelterStatus.CLOSED")
    List<Shelter> findAvailable();

    long countByStatus(ShelterStatus status);
}
