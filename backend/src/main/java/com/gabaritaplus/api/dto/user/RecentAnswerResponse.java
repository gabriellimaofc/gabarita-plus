package com.gabaritaplus.api.dto.user;

import java.time.OffsetDateTime;

public record RecentAnswerResponse(
        Long answerId,
        Long questionId,
        String questionTitle,
        String subject,
        boolean correct,
        String chosenAlternative,
        long timeSpentSeconds,
        int attemptNumber,
        OffsetDateTime answeredAt
) {
}
