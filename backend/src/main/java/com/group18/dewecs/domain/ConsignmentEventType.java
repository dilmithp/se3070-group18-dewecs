package com.group18.dewecs.domain;

/** What happened to a consignment: the logistics audit log. */
public enum ConsignmentEventType {
    DISPATCHED,
    FIELD_UNIT_NOTIFIED,
    REROUTED,
    DRIVER_NOTIFIED,
    HANDOVER,
    DAMAGE_REPORTED,
    CANCELLED
}
