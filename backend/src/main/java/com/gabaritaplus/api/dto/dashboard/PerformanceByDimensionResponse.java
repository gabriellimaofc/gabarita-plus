package com.gabaritaplus.api.dto.dashboard;

public record PerformanceByDimensionResponse(
        String name,
        long total,
        long correct,
        double accuracy
) {
}
