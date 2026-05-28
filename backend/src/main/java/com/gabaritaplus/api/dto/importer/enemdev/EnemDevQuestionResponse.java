package com.gabaritaplus.api.dto.importer.enemdev;

import java.util.List;

public record EnemDevQuestionResponse(
        String title,
        Integer index,
        String discipline,
        String language,
        Integer year,
        String context,
        List<String> files,
        String correctAlternative,
        String alternativesIntroduction,
        List<EnemDevAlternativeResponse> alternatives
) {
}
