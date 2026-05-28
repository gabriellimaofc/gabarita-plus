package com.gabaritaplus.api.dto.question;

import java.util.List;

public record AlternativeResponse(
        Long id,
        String letter,
        String text,
        String html,
        List<QuestionAssetResponse> assets
) {
}
