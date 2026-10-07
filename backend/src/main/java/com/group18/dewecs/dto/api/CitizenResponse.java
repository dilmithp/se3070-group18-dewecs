package com.group18.dewecs.dto.api;

public record CitizenResponse(Long id, String fullName, Long districtId, String districtName, boolean created) {
}
