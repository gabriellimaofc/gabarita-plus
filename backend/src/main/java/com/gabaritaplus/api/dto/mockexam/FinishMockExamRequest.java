package com.gabaritaplus.api.dto.mockexam;

import jakarta.validation.constraints.Min;

public record FinishMockExamRequest(
        @Min(value = 0, message = "Tempo total invalido.")
        Long timeSpentSeconds
) {
}
