package com.gabaritaplus.api.dto.mockexam;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;

public record MockExamQuestionResponse(
        Long questionId,
        Integer questionOrder,
        String title,
        String subject,
        DifficultyLevel difficulty,
        boolean answered,
        Boolean correct
) {
}
