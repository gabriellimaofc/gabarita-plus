package com.gabaritaplus.api.dto.importer.enemdev;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EnemDevImportRequest(
        @NotNull(message = "Ano e obrigatorio.")
        @Min(value = 2000, message = "Ano invalido.")
        @Max(value = 2100, message = "Ano invalido.")
        Integer year,
        @Min(value = 1, message = "O limite precisa ser positivo.")
        Integer limit,
        @Min(value = 0, message = "O offset precisa ser maior ou igual a zero.")
        Integer offset,
        String language
) {
}
