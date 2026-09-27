package com.group18.dewecs.domain;

/**
 * DRAFT was added for the warning-issuance workflow (pre-publish authoring state).
 * The remaining values are unchanged from the original schema: ISSUED is used as
 * "published", CANCELLED as "retracted" — kept as-is (rather than renamed) since
 * the column is a plain varchar shared with the rest of the team's data.
 */
public enum WarningStatus {
    DRAFT,
    ISSUED,
    UPDATED,
    CANCELLED,
    EXPIRED
}
