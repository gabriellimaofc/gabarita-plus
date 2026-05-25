package com.gabaritaplus.api.dto.question;

import java.time.OffsetDateTime;

public record UserAnswerResponse(
        Long id,
        Long questionId,
        String chosenAlternative,
        boolean correct,
        int attemptNumber,
        long timeSpentSeconds,
        OffsetDateTime answeredAt
) {
}
