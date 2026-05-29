package com.gabaritaplus.api.dto.importer.official;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OfficialExamSourceRequest(
        @NotBlank String exam,
        @NotNull Integer year,
        Integer day,
        String bookColor,
        @NotBlank String pdfUrl,
        String answerKeyUrl,
        @NotBlank String sourceUrl,
        String localPdfPath
) {
}
