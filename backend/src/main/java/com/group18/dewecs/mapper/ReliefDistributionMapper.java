package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.ConsignmentItem;
import com.group18.dewecs.domain.ReliefConsignment;
import com.group18.dewecs.dto.ReliefDistributionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Assumes exactly one {@link ConsignmentItem} per consignment — the shape this phase's UI creates. */
@Component
public class ReliefDistributionMapper {

    public ReliefDistributionResponse toResponse(ReliefConsignment consignment) {
        ConsignmentItem item = consignment.getItems().isEmpty() ? null : consignment.getItems().get(0);

        return new ReliefDistributionResponse(
                consignment.getId(),
                item != null ? item.getResource().getName() : null,
                item != null ? item.getResource().getUnit() : null,
                item != null ? item.getQuantity() : null,
                consignment.getShelter().getName(),
                consignment.getShelter().getId(),
                consignment.getStatus().name(),
                consignment.getOrganization().getName(),
                consignment.getDispatchedAt(),
                consignment.getDeliveredAt()
        );
    }

    public List<ReliefDistributionResponse> toResponseList(List<ReliefConsignment> consignments) {
        return consignments.stream().map(this::toResponse).toList();
    }
}
