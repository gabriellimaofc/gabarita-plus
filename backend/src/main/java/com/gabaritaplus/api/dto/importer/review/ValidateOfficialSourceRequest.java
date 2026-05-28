package com.gabaritaplus.api.dto.importer.review;

public record ValidateOfficialSourceRequest(
        String officialSourceUrl,
        String officialPdfUrl,
        String officialAnswerKeyUrl,
        Integer officialPage
) {
}
