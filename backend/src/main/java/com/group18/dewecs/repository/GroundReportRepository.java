package com.group18.dewecs.repository;

import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroundReportRepository extends JpaRepository<GroundReport, Long> {

    long countByStatus(GroundReportStatus status);

    /** One citizen's reports, newest first (mobile API). */
    Page<GroundReport> findByReportedBy_IdOrderBySubmittedAtDescIdDesc(Long citizenId, Pageable pageable);

    /** Replay check for retried submissions: the oldest match, so duplicate rows never break later retries. */
    Optional<GroundReport> findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc(
            Long citizenId, HazardType category, LocalDateTime submittedAt);

    @Query("""
            SELECT r FROM GroundReport r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:districtId IS NULL OR r.district.id = :districtId)
              AND (:category IS NULL OR r.category = :category)
            """)
    List<GroundReport> search(@Param("status") GroundReportStatus status,
                               @Param("districtId") Long districtId,
                               @Param("category") HazardType category);
}
