package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.District;
import com.group18.dewecs.domain.Severity;
import com.group18.dewecs.domain.Warning;
import com.group18.dewecs.dto.ActiveWarningCard;
import com.group18.dewecs.dto.WarningFormRequest;
import com.group18.dewecs.dto.WarningResponse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class WarningMapper {

    public WarningResponse toResponse(Warning warning) {
        return new WarningResponse(
                warning.getId(),
                warning.getHazardEvent().getHazardType().name(),
                warning.getHazardEvent().getDistrict().getName(),
                warning.getHazardEvent().getDistrict().getId(),
                warning.getSeverity().name(),
                warning.getStatus().name(),
                warning.getMessage(),
                warning.getIssuedBy().getFullName(),
                warning.getIssuedAt(),
                warning.getExpiresAt(),
                warning.getBroadcastChannels().stream().map(Enum::name).sorted().toList(),
                warning.effectiveDistricts().stream().map(District::getName).sorted().toList()
        );
    }

    /** A card of the Active Warnings list: "Flood - Colombo", severity, and the severity one click would raise it to. */
    public ActiveWarningCard toActiveCard(Warning warning) {
        String type = warning.getHazardEvent().getHazardType().name();
        String title = type.charAt(0) + type.substring(1).toLowerCase() + " – "
                + String.join(", ", warning.effectiveDistricts().stream().map(District::getName).sorted().toList());
        Severity[] all = Severity.values();
        int next = warning.getSeverity().ordinal() + 1;
        return new ActiveWarningCard(warning.getId(), title, warning.getSeverity().name(), warning.getStatus().name(),
                next < all.length ? all[next].name() : null);
    }

    public List<ActiveWarningCard> toActiveCards(List<Warning> warnings) {
        return warnings.stream().map(this::toActiveCard).toList();
    }

    public List<WarningResponse> toResponseList(List<Warning> warnings) {
        return warnings.stream().map(this::toResponse).toList();
    }

    public WarningFormRequest toFormRequest(Warning warning) {
        WarningFormRequest form = new WarningFormRequest();
        form.setHazardEventId(warning.getHazardEvent().getId());
        form.setIssuedByUserId(warning.getIssuedBy().getId());
        form.setSeverity(warning.getSeverity().name());
        form.setMessage(warning.getMessage());
        form.setExpiresAt(warning.getExpiresAt());
        form.setBroadcastChannels(new HashSet<>(warning.getBroadcastChannels().stream().map(Enum::name).toList()));
        return form;
    }
}
