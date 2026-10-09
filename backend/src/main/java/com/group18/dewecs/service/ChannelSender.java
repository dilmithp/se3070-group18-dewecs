package com.group18.dewecs.service;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.Warning;

/**
 * Pushes a warning out on one channel. The shipped implementation only simulates delivery; a real SMS gateway, mail
 * server or push service is plugged in by adding another bean that supports the channel (the broadcast service uses
 * the first sender that supports it, so a real one replaces the simulation).
 */
public interface ChannelSender {

    boolean supports(BroadcastChannel channel);

    /** Must not throw for an ordinary delivery failure: return {@code success = false} with a reason. */
    SendResult send(Warning warning, BroadcastChannel channel);

    /** {@code recipients} is the number of registered citizens addressed, or null for mass media. */
    record SendResult(boolean success, Integer recipients, String detail) {
    }
}
