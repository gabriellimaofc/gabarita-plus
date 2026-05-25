package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.MasteryStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ErrorNotebookResponse(
        Long id,
        Long questionId,
        String questionTitle,
        String subject,
        int errorCount,
        LocalDate lastReviewedAt,
        LocalDate nextReviewAt,
        MasteryStatus masteryStatus,
        OffsetDateTime updatedAt
) {
}
