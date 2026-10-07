package com.group18.dewecs.dto.api;

import java.util.List;

public record ReportPageResponse(List<ReportResponse> items, int page, int size, long totalItems, int totalPages) {
}
