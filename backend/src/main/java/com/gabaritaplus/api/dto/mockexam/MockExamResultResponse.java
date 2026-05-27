package com.gabaritaplus.api.dto.mockexam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MockExamResultResponse(
        Long id,
        String title,
        boolean finished,
        BigDecimal finalScore,
        int questionCount,
        int correctAnswers,
        int incorrectAnswers,
        int unansweredQuestions,
        Long timeSpentSeconds,
        OffsetDateTime finishedAt,
        List<MockExamSubjectPerformanceResponse> performanceBySubject,
        List<MockExamQuestionDetailResponse> questions
) {
}
