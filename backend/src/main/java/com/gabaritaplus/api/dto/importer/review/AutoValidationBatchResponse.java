package com.gabaritaplus.api.dto.importer.review;

public record AutoValidationBatchResponse(
        int processed,
        int safe,
        int needsReview,
        int invalid,
        int published
) {
}
