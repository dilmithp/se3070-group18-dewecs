package com.group18.dewecs.mapper;

import com.group18.dewecs.domain.Shelter;
import com.group18.dewecs.domain.ShelterOccupant;
import com.group18.dewecs.dto.ShelterOccupantResponse;
import com.group18.dewecs.dto.ShelterResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelterMapper {

    public ShelterResponse toResponse(Shelter shelter) {
        return new ShelterResponse(
                shelter.getId(),
                shelter.getName(),
                shelter.getDistrict().getName(),
                shelter.getDistrict().getId(),
                shelter.getCapacity(),
                shelter.getCurrentOccupancy(),
                shelter.getStatus().name(),
                shelter.getOrganization().getName()
        );
    }

    public List<ShelterResponse> toResponseList(List<Shelter> shelters) {
        return shelters.stream().map(this::toResponse).toList();
    }

    public ShelterOccupantResponse toOccupantResponse(ShelterOccupant occupant) {
        return new ShelterOccupantResponse(
                occupant.getId(),
                occupant.getFullName(),
                occupant.getNic(),
                occupant.getCheckInTime()
        );
    }

    public List<ShelterOccupantResponse> toOccupantResponseList(List<ShelterOccupant> occupants) {
        return occupants.stream().map(this::toOccupantResponse).toList();
    }
}
