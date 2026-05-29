package com.gabaritaplus.api.dto.importer.official;

import java.time.OffsetDateTime;

public record OfficialExamSourceResponse(
        Long id,
        String exam,
        Integer year,
        Integer day,
        String bookColor,
        String pdfUrl,
        String answerKeyUrl,
        String sourceUrl,
        String localPdfPath,
        String answerKeyMapJson,
        OffsetDateTime createdAt
) {
}
