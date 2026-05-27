package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.MasteryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateErrorNotebookStatusRequest(
        @NotNull(message = "Status de dominio e obrigatorio.")
        MasteryStatus masteryStatus
) {
}
