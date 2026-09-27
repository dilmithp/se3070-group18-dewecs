package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ConsignmentStatus;
import com.group18.dewecs.domain.ReliefConsignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReliefConsignmentRepository extends JpaRepository<ReliefConsignment, Long> {

    @Query("""
            SELECT DISTINCT c FROM ReliefConsignment c JOIN c.items i
            WHERE (:status IS NULL OR c.status = :status)
              AND (:shelterId IS NULL OR c.shelter.id = :shelterId)
              AND (:resourceId IS NULL OR i.resource.id = :resourceId)
            """)
    List<ReliefConsignment> search(@Param("status") ConsignmentStatus status,
                                    @Param("shelterId") Long shelterId,
                                    @Param("resourceId") Long resourceId);
}
