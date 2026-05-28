package com.gabaritaplus.api.dto.importer.enemdev;

import java.util.List;

public record EnemDevPreviewResponse(
        Integer year,
        int totalFetched,
        int previewedItems,
        List<EnemDevPreviewItemResponse> items
) {
}
