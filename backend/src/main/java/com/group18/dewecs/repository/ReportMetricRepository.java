package com.group18.dewecs.repository;

import com.group18.dewecs.domain.ReportMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportMetricRepository extends JpaRepository<ReportMetric, Long> {

    List<ReportMetric> findByReport_IdOrderByMetricType(Long reportId);
}
