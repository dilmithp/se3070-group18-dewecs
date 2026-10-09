package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.AlertDeliveryLog;
import com.group18.dewecs.dto.DeliveryLogResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryLogMapper {

    public DeliveryLogResponse toResponse(AlertDeliveryLog row) {
        return new DeliveryLogResponse(
                row.getCreatedAt(),
                row.getTrigger().name(),
                row.getChannel().name(),
                row.getAttempt(),
                row.getOutcome().name(),
                row.getFallbackFor() == null ? null : row.getFallbackFor().name(),
                row.getRecipients(),
                row.getDetail());
    }

    public List<DeliveryLogResponse> toResponseList(List<AlertDeliveryLog> rows) {
        return rows.stream().map(this::toResponse).toList();
    }
}
