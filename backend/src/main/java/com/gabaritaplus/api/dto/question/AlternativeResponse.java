package com.gabaritaplus.api.dto.question;

public record AlternativeResponse(
        Long id,
        String letter,
        String text,
        boolean correct
) {
}
