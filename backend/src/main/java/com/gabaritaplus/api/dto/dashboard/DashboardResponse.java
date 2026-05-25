package com.gabaritaplus.api.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        double accuracyRate,
        long totalAnswered,
        double averageTimeSpentSeconds,
        List<PerformanceByDimensionResponse> performanceBySubject,
        List<PerformanceByDimensionResponse> performanceByTopic,
        List<WeeklyProgressResponse> weeklyProgress
) {
}
