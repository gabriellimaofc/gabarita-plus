package com.gabaritaplus.api.dto.importer;

import com.gabaritaplus.api.entity.enums.ImportBatchStatus;

import java.time.OffsetDateTime;

public record ImportBatchResponse(
        Long id,
        String sourceName,
        String sourceUrl,
        String exam,
        Integer year,
        ImportBatchStatus status,
        Integer totalItems,
        Integer importedItems,
        Integer skippedItems,
        Integer failedItems,
        Integer needsReviewItems,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorReport,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
