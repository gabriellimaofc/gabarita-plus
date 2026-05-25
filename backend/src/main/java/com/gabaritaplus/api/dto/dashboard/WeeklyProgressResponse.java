package com.gabaritaplus.api.dto.dashboard;

public record WeeklyProgressResponse(
        String week,
        long totalAnswers,
        long correctAnswers,
        double accuracy
) {
}
