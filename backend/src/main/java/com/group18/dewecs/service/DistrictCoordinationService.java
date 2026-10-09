package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Organization;
import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.domain.TeamCapability;
import com.group18.dewecs.dto.CoordinationViews.DispatchOptions;
import com.group18.dewecs.dto.CoordinationViews.OccupancyResult;
import com.group18.dewecs.dto.CoordinationViews.Picture;
import com.group18.dewecs.dto.CoordinationViews.SyncResult;
import com.group18.dewecs.dto.CoordinationViews.TeamUpdateResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-03: the combined shelter and rescue picture of a district, occupancy updates with capacity flags, team dispatch
 * (proximity, capability, cross-organization handshake), field status updates, supply logs, offline sync, audit trail.
 */
public interface DistrictCoordinationService {

    Picture picture(Long districtId);

    /** expectedOccupancy and clientTime may be null. Applies the update and returns the capacity level and alternates. */
    OccupancyResult updateOccupancy(Long shelterId, Integer occupancy, Integer expectedOccupancy, String officer,
                                    Long organizationId, LocalDateTime clientTime);

    DispatchOptions dispatchOptions(Long requestId, TeamCapability capability, Long actingOrganizationId);

    RescueRequest dispatch(Long requestId, Long teamId, String notes, Long actingOrganizationId,
                           boolean confirmCrossOrganization, String officer);

    /** A2: no team can be found; records that the incident is unassigned and escalated to the DMC. */
    void escalateUnassigned(Long requestId, String officer);

    /** stage: EN_ROUTE, ON_SITE, TASK_COMPLETE or NEEDS_SUPPORT. */
    TeamUpdateResult updateTeamStage(Long teamId, String stage, String note, String officer);

    void saveProfile(Long teamId, TeamCapability capability, BigDecimal lat, BigDecimal lng);

    void logSupply(Long shelterId, Long resourceId, Integer quantity, String officer, Long organizationId);

    /** Replays occupancy updates queued offline, oldest first; an update older than the shelter's latest is flagged. */
    SyncResult sync(List<QueuedOccupancy> actions, String officer);

    List<District> listDistricts();

    List<Organization> listOrganizations();

    List<Resource> listSupplies();

    record QueuedOccupancy(Long shelterId, Integer occupancy, LocalDateTime clientTime) {
    }
}
