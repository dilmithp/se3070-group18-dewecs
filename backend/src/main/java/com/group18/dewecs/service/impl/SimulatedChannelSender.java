package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.service.ChannelSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pretends to deliver on every channel: it records what a real gateway would report, without contacting anything.
 * Channels listed in {@code dewecs.alerts.simulate-failures} (comma separated, e.g. SMS,TV) always fail, so the retry,
 * fallback and failure log can be shown and tested. It has the lowest priority, so a real sender replaces it.
 */
@Component
@Order(Integer.MAX_VALUE)
public class SimulatedChannelSender implements ChannelSender {

    /** Channels that reach registered citizens one by one; the others are mass media. */
    private static final Set<BroadcastChannel> TO_CITIZENS =
            EnumSet.of(BroadcastChannel.SMS, BroadcastChannel.EMAIL, BroadcastChannel.APP_PUSH,
                    BroadcastChannel.SOCIAL_MEDIA);

    private final CitizenRepository citizenRepository;
    private final Set<String> failingChannels;

    public SimulatedChannelSender(CitizenRepository citizenRepository,
                                  @Value("${dewecs.alerts.simulate-failures:}") String simulateFailures) {
        this.citizenRepository = citizenRepository;
        this.failingChannels = Arrays.stream(simulateFailures.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean supports(BroadcastChannel channel) {
        return true;
    }

    @Override
    public SendResult send(Warning warning, BroadcastChannel channel) {
        String name = label(channel);
        if (failingChannels.contains(channel.name())) {
            return new SendResult(false, null, "Simulated failure: the " + name + " gateway did not answer.");
        }
        if (TO_CITIZENS.contains(channel)) {
            int recipients = 0;
            for (District district : warning.effectiveDistricts()) {
                recipients += (int) citizenRepository.countByDistrict_Id(district.getId());
            }
            return new SendResult(true, recipients,
                    "Simulated " + name + " accepted for " + recipients + " registered citizens.");
        }
        return new SendResult(true, null, "Simulated " + name + " broadcast sent.");
    }

    private String label(BroadcastChannel channel) {
        return channel.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
