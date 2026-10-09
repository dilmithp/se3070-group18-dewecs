package com.group18.dewecs.service;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.DeliveryOutcome;
import com.group18.dewecs.domain.DeliveryTrigger;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.AlertDeliveryLogRepository;
import com.group18.dewecs.service.ChannelSender.SendResult;
import com.group18.dewecs.service.impl.AlertBroadcastServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The broadcast rules with a scripted sender and an in-memory log: no database, no network. */
class AlertBroadcastServiceImplTest {

    private final List<AlertDeliveryLog> store = new ArrayList<>();
    private final Set<BroadcastChannel> failing = EnumSet.noneOf(BroadcastChannel.class);
    private final List<BroadcastChannel> sendCalls = new ArrayList<>();
    private boolean senderThrows;

    private AlertBroadcastServiceImpl service;
    private Warning warning;

    @BeforeEach
    void setUp() {
        AlertDeliveryLogRepository repository = mock(AlertDeliveryLogRepository.class);
        when(repository.save(any(AlertDeliveryLog.class))).thenAnswer(invocation -> {
            AlertDeliveryLog row = invocation.getArgument(0);
            store.add(row);
            return row;
        });
        when(repository.findByWarning_IdOrderByCreatedAtAscIdAsc(anyLong())).thenAnswer(i -> List.copyOf(store));

        ChannelSender sender = new ChannelSender() {
            @Override
            public boolean supports(BroadcastChannel channel) {
                return true;
            }

            @Override
            public SendResult send(Warning w, BroadcastChannel channel) {
                sendCalls.add(channel);
                if (senderThrows) {
                    throw new IllegalStateException("gateway exploded");
                }
                return failing.contains(channel)
                        ? new SendResult(false, null, "down")
                        : new SendResult(true, 10, "ok");
            }
        };
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneId.of("Asia/Colombo"));
        service = new AlertBroadcastServiceImpl(List.of(sender), repository, clock, 2);

        warning = new Warning();
        warning.setId(5L);
    }

    private void channels(BroadcastChannel... chosen) {
        warning.setBroadcastChannels(new HashSet<>(List.of(chosen)));
    }

    @Test
    void everyChosenChannelIsSentOnceAndLogged() {
        channels(BroadcastChannel.SMS, BroadcastChannel.RADIO);

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.PUBLISH, null);

        assertThat(rows).hasSize(2).allSatisfy(r -> {
            assertThat(r.getOutcome()).isEqualTo(DeliveryOutcome.SENT);
            assertThat(r.getAttempt()).isEqualTo(1);
            assertThat(r.getTrigger()).isEqualTo(DeliveryTrigger.PUBLISH);
            assertThat(r.getFallbackFor()).isNull();
        });
        assertThat(rows.get(0).getCreatedAt().getYear()).isEqualTo(2026);
        assertThat(rows.get(0).getRecipients()).isEqualTo(10);
    }

    @Test
    void aFailingChannelIsRetriedAndThenReplacedByItsFallback() {
        channels(BroadcastChannel.SMS);
        failing.add(BroadcastChannel.SMS);

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.PUBLISH, null);

        assertThat(rows).extracting(AlertDeliveryLog::getChannel, AlertDeliveryLog::getAttempt,
                        AlertDeliveryLog::getOutcome, AlertDeliveryLog::getFallbackFor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.SMS, 1, DeliveryOutcome.FAILED, null),
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.SMS, 2, DeliveryOutcome.FAILED, null),
                        org.assertj.core.groups.Tuple.tuple(BroadcastChannel.RADIO, 1, DeliveryOutcome.SENT,
                                BroadcastChannel.SMS));
        assertThat(service.allChannelsCovered(warning)).isTrue();
    }

    @Test
    void theFallbackChainStopsWhenEverythingFails() {
        channels(BroadcastChannel.SMS);
        failing.addAll(EnumSet.of(BroadcastChannel.SMS, BroadcastChannel.RADIO, BroadcastChannel.SIREN));

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.PUBLISH, null);

        // SMS x2, then RADIO x2, then SIREN x2 (the end of the chain): six failed attempts, no endless loop.
        assertThat(rows).hasSize(6).allSatisfy(r -> assertThat(r.getOutcome()).isEqualTo(DeliveryOutcome.FAILED));
        assertThat(service.allChannelsCovered(warning)).isFalse();
    }

    @Test
    void aFallbackToAChannelThatWasAlreadyChosenIsSkipped() {
        channels(BroadcastChannel.SMS, BroadcastChannel.RADIO);
        failing.add(BroadcastChannel.SMS);

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.PUBLISH, null);

        // SMS fails twice and would fall back to RADIO, but RADIO is chosen anyway and is sent on its own.
        assertThat(rows.stream().filter(r -> r.getChannel() == BroadcastChannel.RADIO)).hasSize(1);
        assertThat(rows.stream().filter(r -> r.getFallbackFor() != null)).isEmpty();
        assertThat(service.allChannelsCovered(warning)).isFalse();
    }

    @Test
    void aSenderThatThrowsIsLoggedAsAFailureNotPropagated() {
        channels(BroadcastChannel.TV);
        senderThrows = true;

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.PUBLISH, null);

        assertThat(rows).isNotEmpty().allSatisfy(r -> {
            assertThat(r.getOutcome()).isEqualTo(DeliveryOutcome.FAILED);
            assertThat(r.getDetail()).contains("IllegalStateException").doesNotContain("gateway exploded");
        });
    }

    @Test
    void theNoteIsKeptInTheLogDetail() {
        channels(BroadcastChannel.EMAIL);

        List<AlertDeliveryLog> rows = service.broadcast(warning, DeliveryTrigger.ESCALATION, "Escalated from HIGH to CRITICAL.");

        assertThat(rows.get(0).getTrigger()).isEqualTo(DeliveryTrigger.ESCALATION);
        assertThat(rows.get(0).getDetail()).startsWith("Escalated from HIGH to CRITICAL.").contains("ok");
    }

    @Test
    void retryOnlyTouchesChannelsThatAreNotCoveredYet() {
        channels(BroadcastChannel.SMS, BroadcastChannel.SIREN);
        failing.add(BroadcastChannel.SIREN);
        service.broadcast(warning, DeliveryTrigger.PUBLISH, null);
        sendCalls.clear();

        List<AlertDeliveryLog> retried = service.retryFailed(warning);

        assertThat(sendCalls).containsOnly(BroadcastChannel.SIREN);
        assertThat(retried).isNotEmpty().allSatisfy(r -> assertThat(r.getTrigger()).isEqualTo(DeliveryTrigger.RETRY));
    }

    @Test
    void retryDoesNothingWhenEverythingWasDelivered() {
        channels(BroadcastChannel.SMS);
        service.broadcast(warning, DeliveryTrigger.PUBLISH, null);
        sendCalls.clear();

        assertThat(service.retryFailed(warning)).isEmpty();
        assertThat(sendCalls).isEmpty();
    }

    @Test
    void aWarningWithoutChannelsSendsNothingAndCountsAsCovered() {
        channels();

        assertThat(service.broadcast(warning, DeliveryTrigger.PUBLISH, null)).isEmpty();
        assertThat(service.allChannelsCovered(warning)).isTrue();
        assertThat(sendCalls).isEmpty();
    }
}
