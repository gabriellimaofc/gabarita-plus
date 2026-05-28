package com.gabaritaplus.api.dto.importer.enemdev;

import java.util.List;

public record EnemDevExamResponse(
        String title,
        Integer year,
        List<EnemDevLabelValueResponse> disciplines,
        List<EnemDevLabelValueResponse> languages,
        List<EnemDevExamQuestionSummaryResponse> questions
) {
}
