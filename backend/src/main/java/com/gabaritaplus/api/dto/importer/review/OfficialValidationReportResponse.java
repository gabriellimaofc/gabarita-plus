package com.gabaritaplus.api.dto.importer.review;

import java.util.List;

public record OfficialValidationReportResponse(
        int processed,
        int validated,
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
