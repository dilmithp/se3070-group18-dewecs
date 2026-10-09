package com.group18.dewecs.dto;

import java.time.LocalDateTime;

/** One line of a warning's delivery log. */
public class DeliveryLogResponse {

    private final LocalDateTime time;
    private final String trigger;
    private final String channel;
    private final int attempt;
    private final String outcome;
    private final String fallbackFor;
    private final Integer recipients;
    private final String detail;

    public DeliveryLogResponse(LocalDateTime time, String trigger, String channel, int attempt, String outcome,
                                String fallbackFor, Integer recipients, String detail) {
        this.time = time;
        this.trigger = trigger;
        this.channel = channel;
        this.attempt = attempt;
        this.outcome = outcome;
        this.fallbackFor = fallbackFor;
        this.recipients = recipients;
        this.detail = detail;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getChannel() {
        return channel;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFallbackFor() {
        return fallbackFor;
    }

    public Integer getRecipients() {
        return recipients;
    }

    public String getDetail() {
        return detail;
    }
}
