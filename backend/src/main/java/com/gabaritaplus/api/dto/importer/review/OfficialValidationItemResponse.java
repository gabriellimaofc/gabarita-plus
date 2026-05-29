package com.gabaritaplus.api.dto.importer.review;

import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.util.List;

public record OfficialValidationItemResponse(
        Long questionId,
        String title,
        Integer sourceQuestionNumber,
        Boolean previousValidatedAgainstOfficialSource,
        Boolean newValidatedAgainstOfficialSource,
        Integer previousScore,
        Integer newScore,
        QuestionImportStatus importStatus,
        AutoValidationStatus autoValidationStatus,
        boolean validatedAgainstOfficialSource,
        boolean assetRecovered,
        int recoveredAssets,
        Integer newAutoValidationScore,
        AutoValidationStatus newAutoValidationStatus,
        boolean requiresAssetReview,
        boolean brokenImageDetected,
        boolean recoveryAttempted,
        boolean officialSourceFound,
        boolean pdfDownloaded,
        Long pdfSizeBytes,
        Integer pdfDownloadHttpStatus,
        String pdfDownloadContentType,
        Long pdfDownloadContentLength,
        String pdfDownloadErrorMessage,
        String pdfUrlUsed,
        Integer pdfPageCount,
        List<Integer> candidatePages,
        Integer selectedPage,
        boolean pdfRendered,
        Integer renderedWidth,
        Integer renderedHeight,
        boolean storageUploadAttempted,
        boolean storageUploadSuccess,
        String recoveryFailureReason,
        String recoveryMethod,
        String assetUrl,
        boolean updated,
        List<String> warnings,
        List<String> errors
) {
}
