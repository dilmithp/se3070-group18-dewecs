package com.group18.dewecs.domain;

/**
 * Shared severity scale used by both {@link HazardEvent} and {@link Warning}
 * so the two can be compared directly (e.g. a warning downgraded relative to its event).
 */
public enum Severity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}
