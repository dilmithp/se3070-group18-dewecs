package com.group18.dewecs.dto.api;

import java.util.List;

public record ReferenceDataResponse(List<DistrictItem> districts, List<String> categories) {

    public record DistrictItem(Long id, String name) {
    }
}
