package com.group18.dewecs.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only views of the district coordination dashboard (UC-03). Plain fields: no entities reach the templates. */
public final class CoordinationViews {

    private CoordinationViews() {
    }

    public static class Picture {
        public final Long districtId;
        public final String districtName;
        public final int activeWarnings;
        public final List<ShelterRow> shelters;
        public final List<TeamRow> teams;
        public final List<IncidentRow> incidents;
        public final List<EventLine> events;

        public Picture(Long districtId, String districtName, int activeWarnings, List<ShelterRow> shelters,
                       List<TeamRow> teams, List<IncidentRow> incidents, List<EventLine> events) {
            this.districtId = districtId;
            this.districtName = districtName;
            this.activeWarnings = activeWarnings;
            this.shelters = shelters;
            this.teams = teams;
            this.incidents = incidents;
            this.events = events;
        }

        public boolean isEmpty() {
            return shelters.isEmpty() && teams.isEmpty();
        }
    }

    public static class ShelterRow {
        public final Long id;
        public final String name;
        public final String organizationName;
        public final int occupancy;
        public final int capacity;
        public final int percent;
        /** OK, NEAR, FULL or CLOSED. */
        public final String level;
        public final List<String> alternates;

        public ShelterRow(Long id, String name, String organizationName, int occupancy, int capacity, int percent,
                          String level, List<String> alternates) {
            this.id = id;
            this.name = name;
            this.organizationName = organizationName;
            this.occupancy = occupancy;
            this.capacity = capacity;
            this.percent = percent;
            this.level = level;
            this.alternates = alternates;
        }
    }

    public static class TeamRow {
        public final Long id;
        public final String name;
        public final String organizationName;
        public final String status;
        public final String stage;
        public final String capability;
        public final LocalDateTime updatedAt;
        /** True when a dispatched team has not reported for a while: the status shown is the last known one. */
        public final boolean stale;
        public final String supportNote;
        public final String incident;
        public final String baseLat;
        public final String baseLng;

        public TeamRow(Long id, String name, String organizationName, String status, String stage, String capability,
                       LocalDateTime updatedAt, boolean stale, String supportNote, String incident, String baseLat,
                       String baseLng) {
            this.id = id;
            this.name = name;
            this.organizationName = organizationName;
            this.status = status;
            this.stage = stage;
            this.capability = capability;
            this.updatedAt = updatedAt;
            this.stale = stale;
            this.supportNote = supportNote;
            this.incident = incident;
            this.baseLat = baseLat;
            this.baseLng = baseLng;
        }
    }

    public static class IncidentRow {
        public final Long id;
        public final String label;
        public final String priority;
        public final String status;
        public final String teamName;
        public final LocalDateTime submittedAt;

        public IncidentRow(Long id, String label, String priority, String status, String teamName,
                           LocalDateTime submittedAt) {
            this.id = id;
            this.label = label;
            this.priority = priority;
            this.status = status;
            this.teamName = teamName;
            this.submittedAt = submittedAt;
        }
    }

    public static class EventLine {
        public final LocalDateTime time;
        public final String type;
        public final String subject;
        public final String detail;
        public final String recordedBy;
        public final boolean needsReview;

        public EventLine(LocalDateTime time, String type, String subject, String detail, String recordedBy,
                         boolean needsReview) {
            this.time = time;
            this.type = type;
            this.subject = subject;
            this.detail = detail;
            this.recordedBy = recordedBy;
            this.needsReview = needsReview;
        }
    }

    public static class CandidateTeam {
        public final Long id;
        public final String name;
        public final String organizationName;
        public final String capability;
        public final String distance;
        public final boolean crossOrganization;
        public final boolean otherDistrict;

        public CandidateTeam(Long id, String name, String organizationName, String capability, String distance,
                             boolean crossOrganization, boolean otherDistrict) {
            this.id = id;
            this.name = name;
            this.organizationName = organizationName;
            this.capability = capability;
            this.distance = distance;
            this.crossOrganization = crossOrganization;
            this.otherDistrict = otherDistrict;
        }
    }

    public static class DispatchOptions {
        public final Long requestId;
        public final Long districtId;
        public final String description;
        public final String priority;
        public final String status;
        public final String location;
        public final List<CandidateTeam> candidates;
        public final boolean noneAvailable;

        public DispatchOptions(Long requestId, Long districtId, String description, String priority, String status,
                               String location, List<CandidateTeam> candidates, boolean noneAvailable) {
            this.requestId = requestId;
            this.districtId = districtId;
            this.description = description;
            this.priority = priority;
            this.status = status;
            this.location = location;
            this.candidates = candidates;
            this.noneAvailable = noneAvailable;
        }
    }

    public static class OccupancyResult {
        public final String shelterName;
        public final int occupancy;
        public final int capacity;
        public final String level;
        public final List<String> alternates;
        public final boolean conflict;

        public OccupancyResult(String shelterName, int occupancy, int capacity, String level, List<String> alternates,
                               boolean conflict) {
            this.shelterName = shelterName;
            this.occupancy = occupancy;
            this.capacity = capacity;
            this.level = level;
            this.alternates = alternates;
            this.conflict = conflict;
        }
    }

    public static class TeamUpdateResult {
        public final String teamName;
        public final String stage;
        public final List<String> backups;

        public TeamUpdateResult(String teamName, String stage, List<String> backups) {
            this.teamName = teamName;
            this.stage = stage;
            this.backups = backups;
        }
    }

    public static class SyncResult {
        public final int applied;
        public final int conflicts;
        public final int rejected;

        public SyncResult(int applied, int conflicts, int rejected) {
            this.applied = applied;
            this.conflicts = conflicts;
            this.rejected = rejected;
        }
    }
}
