package com.gabaritaplus.api.dto.mockexam;

import java.time.OffsetDateTime;

public record MockExamAnswerResponse(
        Long questionId,
        Integer questionOrder,
        String chosenAlternative,
        int answeredCount,
        int unansweredCount,
        OffsetDateTime answeredAt
) {
}
