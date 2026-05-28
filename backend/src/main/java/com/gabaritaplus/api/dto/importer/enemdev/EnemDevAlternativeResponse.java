package com.gabaritaplus.api.dto.importer.enemdev;

public record EnemDevAlternativeResponse(
        String letter,
        String text,
        String file,
        Boolean isCorrect
) {
}
