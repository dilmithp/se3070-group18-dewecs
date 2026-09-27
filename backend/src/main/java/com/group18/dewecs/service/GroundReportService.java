package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.GroundReport;
import com.group18.dewecs.domain.GroundReportStatus;
import com.group18.dewecs.domain.HazardType;
import com.group18.dewecs.domain.User;

import java.util.List;

public interface GroundReportService {

    GroundReport markReviewed(Long reportId, Long reviewingUserId);

    GroundReport action(Long reportId, Long reviewingUserId, String note);

    GroundReport dismiss(Long reportId, Long reviewingUserId);

    GroundReport getById(Long reportId);

    /** Any filter may be null to mean "no filter on that dimension". */
    List<GroundReport> list(GroundReportStatus statusFilter, Long districtId, HazardType categoryFilter);

    List<District> listDistricts();

    List<User> listUsersForSelection();
}
