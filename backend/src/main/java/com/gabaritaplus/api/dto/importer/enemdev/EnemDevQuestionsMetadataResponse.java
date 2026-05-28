package com.gabaritaplus.api.dto.importer.enemdev;

public record EnemDevQuestionsMetadataResponse(
        Integer limit,
        Integer offset,
        Integer total,
        Boolean hasMore
) {
}
