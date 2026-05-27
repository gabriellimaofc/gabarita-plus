package com.gabaritaplus.api.dto.mockexam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MockExamRequest(
        @NotBlank(message = "Titulo e obrigatorio.")
        @Size(max = 160)
        String title,
        @NotNull(message = "Tempo e obrigatorio.")
        @Min(value = 1, message = "Tempo deve ser maior que zero.")
        Integer durationMinutes,
        List<Long> questionIds,
        @Min(value = 1, message = "Quantidade de questoes invalida.")
        Integer questionCount
) {
}
