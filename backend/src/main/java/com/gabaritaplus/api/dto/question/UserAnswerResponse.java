package com.gabaritaplus.api.dto.question;

import java.time.OffsetDateTime;

public record UserAnswerResponse(
        Long id,
        Long questionId,
        String chosenAlternative,
        boolean correct,
        String correctAlternative,
        String explanation,
        int attemptNumber,
        long timeSpentSeconds,
        OffsetDateTime answeredAt
) {
}
