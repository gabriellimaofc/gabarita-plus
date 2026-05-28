package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.QuestionAssetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionAssetRequest(
        @NotNull(message = "Tipo do asset e obrigatorio.")
        QuestionAssetType type,
        @Size(max = 1000)
        String url,
        @Size(max = 1000)
        String storagePath,
        @Size(max = 255)
        String originalFileName,
        Integer sourcePage,
        Integer cropX,
        Integer cropY,
        Integer cropWidth,
        Integer cropHeight,
        @Size(max = 500)
        String altText,
        @Size(max = 500)
        String caption,
        @Size(max = 128)
        String checksum
) {
}
