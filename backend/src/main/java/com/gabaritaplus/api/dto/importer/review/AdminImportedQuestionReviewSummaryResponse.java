package com.gabaritaplus.api.dto.importer.review;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.time.OffsetDateTime;

public record AdminImportedQuestionReviewSummaryResponse(
        Long id,
        String title,
        String source,
        Integer sourceYear,
        Integer sourceQuestionNumber,
        String sourceBookColor,
        Integer sourceDay,
        QuestionImportStatus importStatus,
        Boolean validatedAgainstOfficialSource,
        String externalProvider,
        Long importBatchId,
        String subject,
        DifficultyLevel difficulty,
        OffsetDateTime createdAt,
        OffsetDateTime importedAt,
        long alternativesCount,
        long assetsCount
) {
}
