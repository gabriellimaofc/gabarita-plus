package com.gabaritaplus.api.service.importer;

import com.gabaritaplus.api.entity.enums.AutoValidationStatus;

import java.util.List;

public record AutoValidationResult(
        int score,
        AutoValidationStatus status,
        List<String> errors,
        List<String> warnings,
        boolean brokenImageDetected,
        boolean suspiciousTextDetected,
        boolean requiresAssetReview
) {
}
