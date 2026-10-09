package com.group18.dewecs.service;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.Warning;

import java.util.List;

/** Multi-channel alerting of UC-01: send on every chosen channel, retry, fall back, and keep the delivery log. */
public interface AlertBroadcastService {

    /**
     * Sends the warning on each of its channels. A failing channel is tried again, then replaced by its fallback
     * channel; every attempt is written to the delivery log. Never throws for a delivery failure.
     */
    List<AlertDeliveryLog> broadcast(Warning warning, DeliveryTrigger trigger, String note);

    /** Tries again the channels that are not yet covered by a successful delivery (their own or a fallback). */
    List<AlertDeliveryLog> retryFailed(Warning warning);

    /** True when every chosen channel was delivered, directly or through a fallback. */
    boolean allChannelsCovered(Warning warning);

    /** The log of one warning, oldest first. */
    List<AlertDeliveryLog> listLog(Long warningId);
}
