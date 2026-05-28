package com.gabaritaplus.api.dto.importer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportQuestionsPayload(
        @Valid
        @NotEmpty(message = "A carga de importacao precisa conter ao menos uma questao.")
        List<ImportQuestionPayload> questions
) {
}
