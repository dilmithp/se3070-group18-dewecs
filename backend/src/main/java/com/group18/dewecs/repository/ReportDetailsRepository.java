package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ReportDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportDetailsRepository extends JpaRepository<ReportDetails, Long> {

    Optional<ReportDetails> findByReport_Id(Long reportId);

    List<ReportDetails> findByReport_IdIn(Collection<Long> reportIds);
}
