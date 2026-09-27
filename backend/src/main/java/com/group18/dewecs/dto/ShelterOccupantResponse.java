package com.group18.dewecs.dto;

import java.time.LocalDateTime;

public class ShelterOccupantResponse {

    private final Long id;
    private final String fullName;
    private final String nic;
    private final LocalDateTime checkInTime;

    public ShelterOccupantResponse(Long id, String fullName, String nic, LocalDateTime checkInTime) {
        this.id = id;
        this.fullName = fullName;
        this.nic = nic;
        this.checkInTime = checkInTime;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getNic() {
        return nic;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }
}
