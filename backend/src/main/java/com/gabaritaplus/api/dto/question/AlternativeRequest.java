package com.gabaritaplus.api.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlternativeRequest(
        @NotBlank(message = "Letra é obrigatória.")
        @Size(min = 1, max = 1)
        String letter,
        @NotBlank(message = "Texto da alternativa é obrigatório.")
        String text,
        boolean correct
) {
}
