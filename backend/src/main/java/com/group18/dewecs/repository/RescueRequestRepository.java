package com.group18.dewecs.repository;

import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RescueRequestRepository extends JpaRepository<RescueRequest, Long> {

    @Query("""
            SELECT r FROM RescueRequest r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:priority IS NULL OR r.priority = :priority)
              AND (:districtId IS NULL OR r.district.id = :districtId)
            """)
    List<RescueRequest> search(@Param("status") RescueRequestStatus status,
                                @Param("priority") Severity priority,
                                @Param("districtId") Long districtId);

    long countByStatus(RescueRequestStatus status);

    java.util.Optional<RescueRequest> findFirstByAssignedTeam_IdAndStatusOrderByIdDesc(Long teamId, RescueRequestStatus status);

    java.util.List<RescueRequest> findByDistrict_IdAndStatusIn(Long districtId, java.util.Collection<RescueRequestStatus> statuses);
}
