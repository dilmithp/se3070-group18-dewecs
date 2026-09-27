package com.group18.dewecs.domain;

/**
 * ACTIONED was added for the Phase 5 review workflow. The dashboard maps "reviewed" onto
 * VERIFIED and "dismissed" onto REJECTED (already existing) rather than renaming them.
 * PENDING_SYNC/NEEDS_INFO are pre-existing states this workflow doesn't touch.
 */
public enum GroundReportStatus {
    PENDING_SYNC,
    PENDING_REVIEW,
    VERIFIED,
    REJECTED,
    NEEDS_INFO,
    ACTIONED
}
