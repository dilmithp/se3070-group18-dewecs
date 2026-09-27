package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.Resource;
import com.group18.dewecs.dto.ReliefSupplyResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReliefSupplyMapper {

    public ReliefSupplyResponse toResponse(Resource resource) {
        return new ReliefSupplyResponse(
                resource.getId(),
                resource.getName(),
                resource.getType().name(),
                resource.getUnit(),
                resource.getQuantity(),
                resource.getDistrict().getName(),
                resource.getDistrict().getId(),
                resource.getOrganization().getName(),
                resource.getQuantity() < Resource.LOW_STOCK_THRESHOLD,
                resource.getQuantity() == 0
        );
    }

    public List<ReliefSupplyResponse> toResponseList(List<Resource> resources) {
        return resources.stream().map(this::toResponse).toList();
    }
}
