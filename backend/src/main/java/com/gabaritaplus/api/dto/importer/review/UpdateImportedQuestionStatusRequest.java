package com.gabaritaplus.api.dto.importer.review;

import com.gabaritaplus.api.entity.enums.QuestionImportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateImportedQuestionStatusRequest(
        @NotNull QuestionImportStatus importStatus
) {
}
