package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Título é obrigatório.")
        @Size(max = 180)
        String title,
        @NotBlank(message = "Enunciado é obrigatório.")
        String statement,
        @Size(max = 500)
        String imageUrl,
        @NotBlank(message = "Matéria é obrigatória.")
        @Size(max = 80)
        String subject,
        @NotBlank(message = "Tema é obrigatório.")
        @Size(max = 120)
        String topic,
        @Size(max = 120)
        String subtopic,
        @NotNull(message = "Dificuldade é obrigatória.")
        DifficultyLevel difficulty,
        @NotNull(message = "Ano é obrigatório.")
        @Min(value = 2000, message = "Ano inválido.")
        @Max(value = 2100, message = "Ano inválido.")
        Integer year,
        @NotBlank(message = "Prova é obrigatória.")
        @Size(max = 120)
        String exam,
        @Size(max = 120)
        String competency,
        @Size(max = 120)
        String ability,
        String explanation,
        @NotBlank(message = "Alternativa correta é obrigatória.")
        @Size(min = 1, max = 1)
        String correctAlternative,
        @Valid
        @NotEmpty(message = "A questão deve possuir alternativas.")
        List<AlternativeRequest> alternatives
) {
}
