package com.gabaritaplus.api.dto.mockexam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockExamAnswerRequest(
        @NotNull(message = "Questao e obrigatoria.")
        Long questionId,
        @NotBlank(message = "Alternativa e obrigatoria.")
        String chosenAlternative,
        @NotNull(message = "Tempo gasto e obrigatorio.")
        @Min(value = 1, message = "Tempo gasto deve ser maior que zero.")
        Long timeSpentSeconds
) {
}
