package com.gabaritaplus.api.dto.importer.enemdev;

public record EnemDevExamQuestionSummaryResponse(
        String title,
        Integer index,
        String discipline,
        String language
) {
}
