package com.group18.dewecs.service;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.RiverBasin;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface WarningService {

    Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels, Long issuedByUserId);

    /**
     * As above, and when {@code riverBasinId} is given the warning covers every district of that river basin as well
     * as the district of the hazard event.
     */
    Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels, Long issuedByUserId, Long riverBasinId);

    Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels);

    /** A null {@code riverBasinId} keeps the districts the draft already covers; an id replaces them. */
    Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels, Long riverBasinId);

    /** Makes the draft official and broadcasts it on its channels (failures are logged, never block publishing). */
    Warning publish(Long warningId);

    Warning retract(Long warningId);

    /**
     * Raises the severity of a published warning and sends it again on its channels. The severity must go up. An
     * optional note is appended to the message; an optional new expiry must be in the future. The warning becomes
     * UPDATED and keeps its original issue time.
     */
    Warning escalate(Long warningId, String newSeverity, String note, LocalDateTime newExpiresAt);

    /** Sends again on the channels that have no successful delivery yet. Returns the new log rows (empty = nothing to do). */
    List<AlertDeliveryLog> rebroadcast(Long warningId);

    /** The delivery log of one warning, oldest first. */
    List<AlertDeliveryLog> listDeliveryLog(Long warningId);

    /** True when every chosen channel was delivered (directly or through a fallback). */
    boolean allChannelsDelivered(Long warningId);

    Warning getById(Long warningId);

    /** Either filter may be null to mean "no filter on that dimension". Lazily expires overdue warnings first. */
    List<Warning> list(WarningStatus statusFilter, Long districtId);

    /** The live warnings (ISSUED or UPDATED), newest first. Lazily expires overdue ones first. */
    List<Warning> listActive();

    List<HazardEvent> listHazardEventsForSelection();

    List<District> listDistricts();

    List<User> listUsersForSelection();

    List<RiverBasin> listRiverBasins();
}
