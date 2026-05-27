package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.MasteryStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ErrorNotebookResponse(
        Long id,
        Long questionId,
        String questionTitle,
        String subject,
        String topic,
        DifficultyLevel difficulty,
        int errorCount,
        LocalDate lastErrorAt,
        LocalDate lastReviewedAt,
        LocalDate nextReviewAt,
        MasteryStatus masteryStatus,
        ReviewPriority priority,
        OffsetDateTime updatedAt
) {
}
