package com.gabaritaplus.api.dto.question;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation,
        @JsonInclude(JsonInclude.Include.NON_NULL) String correctAlternative,
        boolean favorite,
        Boolean answered,
        Boolean answeredCorrectly,
        List<AlternativeResponse> alternatives,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
