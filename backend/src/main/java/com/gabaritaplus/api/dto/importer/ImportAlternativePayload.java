package com.gabaritaplus.api.dto.importer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportAlternativePayload(
        @NotBlank(message = "Letra da alternativa e obrigatoria.")
        @Size(min = 1, max = 1)
        String letter,
        @NotBlank(message = "Texto da alternativa e obrigatorio.")
        String text,
        String html,
        @Valid
        List<ImportQuestionAssetPayload> assets
) {
}
