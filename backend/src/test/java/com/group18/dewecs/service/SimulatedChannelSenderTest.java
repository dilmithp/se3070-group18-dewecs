package com.group18.dewecs.service;

import com.group18.dewecs.domain.BroadcastChannel;
import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.HazardEvent;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.repository.CitizenRepository;
import com.group18.dewecs.service.ChannelSender.SendResult;
import com.group18.dewecs.service.impl.SimulatedChannelSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimulatedChannelSenderTest {

    private CitizenRepository citizens;
    private Warning warning;

    @BeforeEach
    void setUp() {
        citizens = mock(CitizenRepository.class);
        when(citizens.countByDistrict_Id(1L)).thenReturn(120L);
        when(citizens.countByDistrict_Id(2L)).thenReturn(30L);
        warning = new Warning();
        HazardEvent event = new HazardEvent();
        event.setDistrict(district(1L));
        warning.setHazardEvent(event);
    }

    private District district(Long id) {
        District d = new District();
        d.setId(id);
        d.setName("District " + id);
        return d;
    }

    @Test
    void channelsToCitizensCountTheRegisteredCitizensOfTheEventDistrict() {
        SendResult result = new SimulatedChannelSender(citizens, "").send(warning, BroadcastChannel.SMS);

        assertThat(result.success()).isTrue();
        assertThat(result.recipients()).isEqualTo(120);
        assertThat(result.detail()).contains("sms").contains("120");
    }

    @Test
    void aBasinWarningAddsUpEveryCoveredDistrict() {
        warning.setAffectedDistricts(new HashSet<>(Set.of(district(1L), district(2L))));

        assertThat(new SimulatedChannelSender(citizens, "").send(warning, BroadcastChannel.APP_PUSH).recipients())
                .isEqualTo(150);
    }

    @Test
    void massMediaHaveNoRecipientCount() {
        for (BroadcastChannel channel : Set.of(BroadcastChannel.RADIO, BroadcastChannel.TV, BroadcastChannel.SIREN)) {
            SendResult result = new SimulatedChannelSender(citizens, "").send(warning, channel);
            assertThat(result.success()).isTrue();
            assertThat(result.recipients()).isNull();
        }
    }

    @Test
    void configuredChannelsAlwaysFail_caseAndSpacesDoNotMatter() {
        SimulatedChannelSender sender = new SimulatedChannelSender(citizens, " sms , Tv ");

        assertThat(sender.send(warning, BroadcastChannel.SMS).success()).isFalse();
        assertThat(sender.send(warning, BroadcastChannel.TV).success()).isFalse();
        assertThat(sender.send(warning, BroadcastChannel.SMS).detail()).contains("did not answer");
        assertThat(sender.send(warning, BroadcastChannel.RADIO).success()).isTrue();
    }

    @Test
    void itSupportsEveryChannel() {
        SimulatedChannelSender sender = new SimulatedChannelSender(citizens, "");

        for (BroadcastChannel channel : BroadcastChannel.values()) {
            assertThat(sender.supports(channel)).isTrue();
        }
    }
}
