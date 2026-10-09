package com.group18.dewecs.service;

import com.group18.dewecs.domain.Warning;

import java.util.Set;

/**
 * The one-screen way of UC-01 ("Add New Hazard Event"): the duty officer picks the hazard type, severity and the
 * affected area (a district, or a river basin), writes the message and issues the warning in one step.
 */
public interface WarningIssuanceService {

    /**
     * Registers the hazard event, drafts the warning and publishes it (which broadcasts it), all in one transaction:
     * if anything is refused, nothing is saved.
     *
     * @param districtId   the affected district; used when {@code riverBasinId} is null
     * @param riverBasinId when given, the warning covers every district of the basin and the event is registered in
     *                     the first district of the basin (by name)
     * @param title        optional; when not blank it becomes the first line of the message
     */
    Warning issue(String hazardType, String severity, Long districtId, Long riverBasinId, String title,
                  String message, Set<String> broadcastChannels, Long issuedByUserId);
}
