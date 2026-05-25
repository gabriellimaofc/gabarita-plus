package com.gabaritaplus.api.dto.mockexam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MockExamResponse(
        Long id,
        String title,
        int durationMinutes,
        boolean finished,
        BigDecimal finalScore,
        List<MockExamQuestionResponse> questions,
        OffsetDateTime createdAt
) {
}
