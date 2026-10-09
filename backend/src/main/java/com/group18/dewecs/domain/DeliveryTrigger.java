package com.group18.dewecs.domain;

/** Why a broadcast ran: the first publish, an escalation, or a manual retry of the channels that failed. */
public enum DeliveryTrigger {
    PUBLISH,
    ESCALATION,
    RETRY
}
