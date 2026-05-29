package com.gabaritaplus.api.dto.importer.review;

public record AutoValidationCountersResponse(
        long safe,
        long needsReview,
        long invalid,
        long brokenImages,
        long pendingInep
) {
}
