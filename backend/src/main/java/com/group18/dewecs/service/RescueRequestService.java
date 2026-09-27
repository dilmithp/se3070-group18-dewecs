package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.RescueRequestStatus;
import com.group18.dewecs.domain.RescueTeam;
import com.group18.dewecs.domain.Severity;

import java.math.BigDecimal;
import java.util.List;

public interface RescueRequestService {

    RescueRequest submit(Long districtId, String requesterName, String requesterPhone, BigDecimal gpsLat,
                          BigDecimal gpsLng, String description, String priority);

    RescueRequest assign(Long requestId, Long teamId);

    RescueRequest complete(Long requestId);

    RescueRequest cancel(Long requestId);

    RescueRequest getById(Long requestId);

    /** Any filter may be null to mean "no filter on that dimension". */
    List<RescueRequest> list(RescueRequestStatus statusFilter, Severity priorityFilter, Long districtId);

    List<District> listDistricts();

    List<RescueTeam> listTeamsForSelection();
}
