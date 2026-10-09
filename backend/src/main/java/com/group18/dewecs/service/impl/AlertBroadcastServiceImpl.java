package com.group18.dewecs.service.impl;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.service.AlertBroadcastService;
import com.group18.dewecs.service.ChannelSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AlertBroadcastServiceImpl implements AlertBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(AlertBroadcastServiceImpl.class);

    /** When a channel keeps failing, the warning goes out on this one instead (at most one hop per failure). */
    static final Map<BroadcastChannel, BroadcastChannel> FALLBACKS = new EnumMap<>(BroadcastChannel.class);

    static {
        FALLBACKS.put(BroadcastChannel.APP_PUSH, BroadcastChannel.SMS);
        FALLBACKS.put(BroadcastChannel.EMAIL, BroadcastChannel.SMS);
        FALLBACKS.put(BroadcastChannel.SOCIAL_MEDIA, BroadcastChannel.SMS);
        FALLBACKS.put(BroadcastChannel.SMS, BroadcastChannel.RADIO);
        FALLBACKS.put(BroadcastChannel.TV, BroadcastChannel.RADIO);
        FALLBACKS.put(BroadcastChannel.RADIO, BroadcastChannel.SIREN);
    }

    private final List<ChannelSender> senders;
    private final AlertDeliveryLogRepository logRepository;
    private final Clock clock;
    private final int maxAttempts;

    public AlertBroadcastServiceImpl(List<ChannelSender> senders,
                                     AlertDeliveryLogRepository logRepository,
                                     Clock clock,
                                     @Value("${dewecs.alerts.max-attempts:2}") int maxAttempts) {
        this.senders = senders;
        this.logRepository = logRepository;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    @Transactional
    public List<AlertDeliveryLog> broadcast(Warning warning, DeliveryTrigger trigger, String note) {
        List<AlertDeliveryLog> written = new ArrayList<>();
        Set<BroadcastChannel> used = new HashSet<>(warning.getBroadcastChannels());
        for (BroadcastChannel channel : sorted(warning.getBroadcastChannels())) {
            deliver(warning, channel, null, trigger, note, used, written);
        }
        return written;
    }

    @Override
    @Transactional
    public List<AlertDeliveryLog> retryFailed(Warning warning) {
        Set<BroadcastChannel> covered = coveredChannels(logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(warning.getId()));
        List<AlertDeliveryLog> written = new ArrayList<>();
        Set<BroadcastChannel> used = new HashSet<>(warning.getBroadcastChannels());
        for (BroadcastChannel channel : sorted(warning.getBroadcastChannels())) {
            if (!covered.contains(channel)) {
                deliver(warning, channel, null, DeliveryTrigger.RETRY, "Manual retry.", used, written);
            }
        }
        return written;
    }

    @Override
    public boolean allChannelsCovered(Warning warning) {
        if (warning.getBroadcastChannels().isEmpty()) {
            return true;
        }
        Set<BroadcastChannel> covered = coveredChannels(logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(warning.getId()));
        return covered.containsAll(warning.getBroadcastChannels());
    }

    @Override
    public List<AlertDeliveryLog> listLog(Long warningId) {
        return logRepository.findByWarning_IdOrderByCreatedAtAscIdAsc(warningId);
    }

    /** A channel counts as covered when it, or the channel it fell back to, has a successful delivery. */
    private Set<BroadcastChannel> coveredChannels(List<AlertDeliveryLog> rows) {
        Set<BroadcastChannel> covered = EnumSet.noneOf(BroadcastChannel.class);
        for (AlertDeliveryLog row : rows) {
            if (row.getOutcome() == DeliveryOutcome.SENT) {
                covered.add(row.getChannel());
                if (row.getFallbackFor() != null) {
                    covered.add(row.getFallbackFor());
                }
            }
        }
        return covered;
    }

    private void deliver(Warning warning, BroadcastChannel channel, BroadcastChannel fallbackFor,
                         DeliveryTrigger trigger, String note, Set<BroadcastChannel> used,
                         List<AlertDeliveryLog> written) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ChannelSender.SendResult result = safeSend(warning, channel);
            written.add(record(warning, channel, attempt, result, trigger, fallbackFor, note));
            if (result.success()) {
                return;
            }
        }
        BroadcastChannel next = FALLBACKS.get(channel);
        if (next != null && used.add(next)) {
            deliver(warning, next, channel, trigger, note, used, written);
        }
    }

    private ChannelSender.SendResult safeSend(Warning warning, BroadcastChannel channel) {
        try {
            ChannelSender sender = senders.stream().filter(s -> s.supports(channel)).findFirst().orElse(null);
            if (sender == null) {
                return new ChannelSender.SendResult(false, null, "No sender is configured for this channel.");
            }
            return sender.send(warning, channel);
        } catch (RuntimeException e) {
            log.warn("Channel {} failed for warning {}: {}", channel, warning.getId(), e.getClass().getSimpleName());
            return new ChannelSender.SendResult(false, null, "Sender error (" + e.getClass().getSimpleName() + ").");
        }
    }

    private AlertDeliveryLog record(Warning warning, BroadcastChannel channel, int attempt,
                                    ChannelSender.SendResult result, DeliveryTrigger trigger,
                                    BroadcastChannel fallbackFor, String note) {
        AlertDeliveryLog row = new AlertDeliveryLog();
        row.setWarning(warning);
        row.setChannel(channel);
        row.setAttempt(attempt);
        row.setOutcome(result.success() ? DeliveryOutcome.SENT : DeliveryOutcome.FAILED);
        row.setTrigger(trigger);
        row.setFallbackFor(fallbackFor);
        row.setRecipients(result.recipients());
        String detail = note == null || note.isBlank() ? result.detail() : note + " " + result.detail();
        row.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        row.setCreatedAt(LocalDateTime.now(clock));
        return logRepository.save(row);
    }

    private List<BroadcastChannel> sorted(Set<BroadcastChannel> channels) {
        return channels.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
