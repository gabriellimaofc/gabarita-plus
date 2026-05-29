package com.gabaritaplus.api.dto.importer.review;

import com.gabaritaplus.api.entity.enums.AutoValidationStatus;
import com.gabaritaplus.api.entity.enums.QuestionImportStatus;

import java.util.List;

public record OfficialValidationItemResponse(
        Long questionId,
        String title,
        Integer sourceQuestionNumber,
        Boolean previousValidatedAgainstOfficialSource,
        Boolean newValidatedAgainstOfficialSource,
        Integer previousScore,
        Integer newScore,
        QuestionImportStatus importStatus,
        AutoValidationStatus autoValidationStatus,
        boolean validatedAgainstOfficialSource,
        boolean assetRecovered,
        boolean updated,
        List<String> warnings,
        List<String> errors
) {
}
