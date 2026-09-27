package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.RescueRequest;
import com.group18.dewecs.dto.RescueRequestResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RescueRequestMapper {

    public RescueRequestResponse toResponse(RescueRequest request) {
        return new RescueRequestResponse(
                request.getId(),
                request.getRequesterName(),
                request.getRequesterPhone(),
                request.getDistrict().getName(),
                request.getDistrict().getId(),
                request.getGpsLat(),
                request.getGpsLng(),
                request.getDescription(),
                request.getPriority().name(),
                request.getStatus().name(),
                request.getAssignedTeam() != null ? request.getAssignedTeam().getName() : null,
                request.getSubmittedAt(),
                request.getAssignedAt(),
                request.getCompletedAt()
        );
    }

    public List<RescueRequestResponse> toResponseList(List<RescueRequest> requests) {
        return requests.stream().map(this::toResponse).toList();
    }
}
