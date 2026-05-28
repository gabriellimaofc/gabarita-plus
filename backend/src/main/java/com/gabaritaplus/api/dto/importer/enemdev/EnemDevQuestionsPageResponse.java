package com.gabaritaplus.api.dto.importer.enemdev;

import java.util.List;

public record EnemDevQuestionsPageResponse(
        EnemDevQuestionsMetadataResponse metadata,
        List<EnemDevQuestionResponse> questions
) {
}
