package com.group18.dewecs.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** NIC and full name are normalised and range-checked in CitizenService (the contract trims before checking). */
public record IdentifyCitizenRequest(
        @NotBlank(message = "Enter your NIC") String nic,
        @NotBlank(message = "Enter your full name") String fullName,
        @NotBlank(message = "Enter a phone number")
        @Pattern(regexp = "^\\+?[0-9][0-9 -]{7,14}$", message = "Enter a valid phone number") String phone,
        @NotNull(message = "Select a district") Long districtId) {
}
