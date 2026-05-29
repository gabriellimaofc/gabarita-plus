package com.gabaritaplus.api.dto.importer.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gabaritaplus.api.dto.question.AlternativeResponse;
import com.gabaritaplus.api.dto.question.QuestionAssetResponse;
import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminImportedQuestionReviewDetailResponse(
        Long id,
        String title,
        String statement,
        @JsonInclude(JsonInclude.Include.NON_NULL) String statementHtml,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        String subject,
        String topic,
        String subtopic,
        DifficultyLevel difficulty,
        Integer year,
        String exam,
        @JsonInclude(JsonInclude.Include.NON_NULL) String competency,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ability,
        String source,
        String sourceUrl,
        String sourceExam,
        Integer sourceYear,
        Integer sourceQuestionNumber,
        String sourceBookColor,
        Integer sourceDay,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer sourcePage,
        @JsonInclude(JsonInclude.Include.NON_NULL) String officialSourceUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String officialPdfUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String officialAnswerKeyUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer officialPage,
        Boolean validatedAgainstOfficialSource,
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime validatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalProvider,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalProviderUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalQuestionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String externalLicense,
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime importedAt,
        Long importBatchId,
        String statementHash,
        QuestionImportStatus importStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation,
        String correctAlternative,
        long alternativesCount,
        long assetsCount,
        Integer autoValidationScore,
        AutoValidationStatus autoValidationStatus,
        String autoValidationErrors,
        String autoValidationWarnings,
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime autoValidatedAt,
        Boolean brokenImageDetected,
        Boolean suspiciousTextDetected,
        Boolean requiresAssetReview,
        List<QuestionAssetResponse> assets,
        List<AlternativeResponse> alternatives,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
