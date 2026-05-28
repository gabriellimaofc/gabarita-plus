package com.gabaritaplus.api.dto.importer;

import java.util.List;

public record ImportItemErrorResponse(
        Integer itemIndex,
        String title,
        Integer sourceQuestionNumber,
        List<String> errors
) {
}
