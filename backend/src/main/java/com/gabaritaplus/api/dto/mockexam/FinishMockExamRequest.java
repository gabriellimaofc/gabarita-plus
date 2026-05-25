package com.gabaritaplus.api.dto.mockexam;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinishMockExamRequest(
        @NotNull(message = "Nota final é obrigatória.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Nota inválida.")
        BigDecimal finalScore
) {
}
