package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.QuestionAssetType;

public record QuestionAssetResponse(
        Long id,
        Long questionId,
        Long alternativeId,
        QuestionAssetType type,
        String url,
        String storagePath,
        String originalFileName,
        Integer sourcePage,
        Integer cropX,
        Integer cropY,
        Integer cropWidth,
        Integer cropHeight,
        String altText,
        String caption,
        String checksum
) {
}
