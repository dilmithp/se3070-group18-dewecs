package com.group18.dewecs.service;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.User;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.domain.WarningStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface WarningService {

    Warning createDraft(Long hazardEventId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels, Long issuedByUserId);

    Warning updateDraft(Long warningId, String severity, String message, LocalDateTime expiresAt,
                         Set<String> broadcastChannels);

    Warning publish(Long warningId);

    Warning retract(Long warningId);

    Warning getById(Long warningId);

    /** Either filter may be null to mean "no filter on that dimension". Lazily expires overdue warnings first. */
    List<Warning> list(WarningStatus statusFilter, Long districtId);

    List<HazardEvent> listHazardEventsForSelection();

    List<District> listDistricts();

    List<User> listUsersForSelection();
}
