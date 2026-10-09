package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ReportKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportKpiRepository extends JpaRepository<ReportKpi, Long> {

    List<ReportKpi> findByReport_IdOrderBySortOrderAscIdAsc(Long reportId);
}
