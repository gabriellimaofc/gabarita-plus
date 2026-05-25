package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;

public record QuestionFilterRequest(
        String search,
        String subject,
        String topic,
        String subtopic,
        DifficultyLevel difficulty,
        Integer year,
        String exam,
        Boolean answered,
        Boolean incorrectOnly,
        Boolean favoritesOnly
) {
}
