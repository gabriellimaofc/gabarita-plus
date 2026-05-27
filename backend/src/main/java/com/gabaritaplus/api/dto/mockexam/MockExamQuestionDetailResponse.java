package com.gabaritaplus.api.dto.mockexam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gabaritaplus.api.dto.question.AlternativeResponse;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;

import java.util.List;

public record MockExamQuestionDetailResponse(
        Long questionId,
        Integer questionOrder,
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
        String chosenAlternative,
        Boolean answered,
        Boolean correct,
        @JsonInclude(JsonInclude.Include.NON_NULL) String correctAlternative,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation,
        List<AlternativeResponse> alternatives
) {
}
