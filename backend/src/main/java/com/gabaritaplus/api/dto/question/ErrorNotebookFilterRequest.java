package com.gabaritaplus.api.dto.question;

import com.gabaritaplus.api.entity.enums.DifficultyLevel;
import com.gabaritaplus.api.entity.enums.MasteryStatus;
import com.gabaritaplus.api.entity.enums.ReviewPriority;

public record ErrorNotebookFilterRequest(
        String subject,
        String topic,
        DifficultyLevel difficulty,
        MasteryStatus masteryStatus,
        ReviewPriority priority
) {
}
