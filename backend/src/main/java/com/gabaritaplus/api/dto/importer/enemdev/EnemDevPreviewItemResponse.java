package com.gabaritaplus.api.dto.importer.enemdev;

import com.gabaritaplus.api.dto.importer.ImportQuestionPayload;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.util.List;

public record EnemDevPreviewItemResponse(
        ImportQuestionPayload question,
        String statementHash,
        QuestionImportStatus proposedImportStatus,
        boolean duplicate,
        List<String> warnings
) {
}
