package com.gabaritaplus.api.dto.question;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record QuestionResponse(
        Long id,
        String title,
        String statement,
        String statementHtml,
        String imageUrl,
        String subject,
        String topic,
        String subtopic,
        DifficultyLevel difficulty,
        Integer year,
        String exam,
        String competency,
        String ability,
        String source,
        String sourceUrl,
        String sourceExam,
        Integer sourceYear,
        Integer sourceQuestionNumber,
        String sourceBookColor,
        Integer sourceDay,
        Integer sourcePage,
        String statementHash,
        QuestionImportStatus importStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation,
        @JsonInclude(JsonInclude.Include.NON_NULL) String correctAlternative,
        boolean favorite,
        Boolean answered,
        Boolean answeredCorrectly,
        List<QuestionAssetResponse> assets,
        List<AlternativeResponse> alternatives,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
