package com.gabaritaplus.api.dto.importer.enemdev;

import java.util.List;

public record EnemDevYearResponse(
        Integer year,
        String title,
        List<String> disciplines,
        List<String> languages,
        Integer questionCount
) {
}
