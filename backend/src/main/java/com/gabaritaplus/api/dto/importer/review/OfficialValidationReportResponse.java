package com.gabaritaplus.api.dto.importer.review;

import java.util.List;

public record OfficialValidationReportResponse(
        int totalProcessed,
        int processed,
        int validated,
        int skipped,
        int failed,
        int ambiguousOfficialSource,
        int answerKeyMissing,
        int answerKeyMismatch,
        int updatedQuestions,
        int needsReview,
        int invalid,
        int brokenImages,
        int pendingAssets,
        int pendingInep,
        int assetRecovered,
        int assetRecoveryFailed,
        List<OfficialValidationItemResponse> items
) {
}
