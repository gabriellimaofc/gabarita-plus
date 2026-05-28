package com.gabaritaplus.api.dto.importer;

import java.util.List;

public record ImportReportResponse(
        Long batchId,
        int totalProcessed,
        int imported,
        int skippedDuplicates,
        int errors,
        int needsReview,
        int invalid,
        boolean dryRun,
        List<ImportItemErrorResponse> itemErrors
) {
}
