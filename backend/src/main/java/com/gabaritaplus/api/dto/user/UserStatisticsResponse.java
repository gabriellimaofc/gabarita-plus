package com.gabaritaplus.api.dto.user;

import java.util.List;

public record UserStatisticsResponse(
        long totalAnswers,
        long totalCorrectAnswers,
        double accuracyRate,
        double averageTimeSpentSeconds,
        List<RecentAnswerResponse> recentAnswers
) {
}
