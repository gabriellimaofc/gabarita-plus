package com.gabaritaplus.api.dto.question;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserAnswerRequest(
        @NotNull(message = "Questão é obrigatória.")
        Long questionId,
        @NotBlank(message = "Alternativa escolhida é obrigatória.")
        @Size(min = 1, max = 1)
        String chosenAlternative,
        @NotNull(message = "Tempo gasto é obrigatório.")
        @Min(value = 1, message = "Tempo gasto deve ser maior que zero.")
        Long timeSpentSeconds
) {
}
