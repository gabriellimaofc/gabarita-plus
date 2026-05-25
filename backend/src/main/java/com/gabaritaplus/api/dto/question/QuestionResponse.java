package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;

import java.time.OffsetDateTime;
import java.util.List;

public record QuestionResponse(
        Long id,
        String title,
        String statement,
        String imageUrl,
        String subject,
        String topic,
        String subtopic,
        DifficultyLevel difficulty,
        Integer year,
        String exam,
        String competency,
        String ability,
        String explanation,
        String correctAlternative,
        boolean favorite,
        Boolean answered,
        Boolean answeredCorrectly,
        List<AlternativeResponse> alternatives,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
