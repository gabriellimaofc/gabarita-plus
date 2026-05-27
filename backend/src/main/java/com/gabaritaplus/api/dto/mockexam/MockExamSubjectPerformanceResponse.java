package com.gabaritaplus.api.dto.mockexam;

import java.math.BigDecimal;

public record MockExamSubjectPerformanceResponse(
        String subject,
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        BigDecimal accuracy
) {
}
