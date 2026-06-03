package com.gabaritaplus.api.dto.importer.review;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import jakarta.validation.constraints.Size;

public record UpdateImportedQuestionReviewRequest(
        String statement,
        @Size(max = 120) String topic,
        @Size(max = 120) String subtopic,
        DifficultyLevel difficulty
) {
}
