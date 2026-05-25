package com.gabaritaplus.api.dto.mockexam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MockExamRequest(
        @NotBlank(message = "Título é obrigatório.")
        @Size(max = 160)
        String title,
        @NotNull(message = "Tempo é obrigatório.")
        @Min(value = 1, message = "Tempo deve ser maior que zero.")
        Integer durationMinutes,
        @NotEmpty(message = "O simulado precisa de questões.")
        List<Long> questionIds
) {
}
