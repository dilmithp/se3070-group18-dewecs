package com.group18.dewecs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "citizens")
public class Citizen extends User {

    @NotBlank
    @Column(unique = true)
    private String nic;

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }
}
