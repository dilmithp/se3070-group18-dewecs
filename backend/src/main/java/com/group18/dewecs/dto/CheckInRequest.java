package com.group18.dewecs.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckInRequest {

    @NotBlank(message = "Enter the occupant's full name")
    private String fullName;

    @NotBlank(message = "Enter the occupant's NIC")
    private String nic;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }
}
