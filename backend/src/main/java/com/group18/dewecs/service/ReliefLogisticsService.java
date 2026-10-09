package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.domain.ShelterNeed;
import com.group18.dewecs.dto.LogisticsViews.CommandCenterView;
import com.group18.dewecs.dto.LogisticsViews.DetailsView;
import com.group18.dewecs.dto.LogisticsViews.EventLine;
import com.group18.dewecs.dto.LogisticsViews.NotificationLine;
import com.group18.dewecs.dto.LogisticsViews.SplitLine;
import com.group18.dewecs.dto.ShelterResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-02 relief logistics on top of the plain distribution rules: the command center for an incident and district,
 * the shelter demand feed, dispatch with split sourcing and a concurrency lock, field handover with damage, re-routing,
 * the logistics audit log and the agency notifications.
 */
public interface ReliefLogisticsService {

    /** Both ids may be null (the page then only offers the choice). The district defaults to the one of the incident. */
    CommandCenterView commandCenter(Long hazardEventId, Long districtId);

    ShelterNeed recordNeed(Long shelterId, String resourceType, Integer quantity, boolean urgent, String note);

    ShelterNeed cancelNeed(Long needId);

    /**
     * Dispatches a batch. When the quantity is more than 85% of the chosen agency's stock (or more than it has), nothing
     * is committed and a split proposal across partner agencies is returned instead, unless {@code acceptSplit} is true
     * (then the linked sub-batches are committed). A stock that changed since the form was shown is refused with a clear
     * message.
     */
    DispatchOutcome dispatch(DispatchRequest request, boolean acceptSplit, String officer);

    /** Field handover: the recipient signs, the officer tallies what was accepted and what was damaged. */
    ReliefConsignment handover(Long consignmentId, HandoverRequest request, String officer);

    /** Changes the destination of a dispatched consignment to another shelter of the same district. */
    ReliefConsignment reroute(Long consignmentId, Long newShelterId, String reason, String officer);

    /** Cancels a dispatched consignment (stock goes back) and records it in the audit log. */
    ReliefConsignment cancel(Long consignmentId, String officer);

    Optional<DetailsView> details(Long consignmentId);

    List<EventLine> events(Long consignmentId);

    List<NotificationLine> notifications();

    /** The other shelters of the district of the current destination: where a dispatched consignment can be re-routed. */
    List<ShelterResponse> rerouteOptions(Long consignmentId);

    List<District> listDistricts();

    List<HazardEvent> listIncidents();

    record DispatchRequest(Long hazardEventId, Long resourceId, Long shelterId, Integer quantity, Long needId,
                           String handlingNotes, String vehicle, String driverName, String driverPhone,
                           LocalDateTime expectedArrival, Integer expectedStock) {
    }

    record HandoverRequest(String receivedBy, Integer receivedQuantity, Integer damagedQuantity, String note,
                           byte[] photo) {
    }

    /** Either {@code created} (committed) or {@code proposal} (waiting for the officer's decision). */
    record DispatchOutcome(List<ReliefConsignment> created, List<SplitLine> proposal) {
        public boolean needsDecision() {
            return created.isEmpty() && !proposal.isEmpty();
        }
    }
}
