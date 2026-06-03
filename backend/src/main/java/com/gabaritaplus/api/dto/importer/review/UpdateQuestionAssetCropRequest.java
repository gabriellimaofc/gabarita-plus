package com.gabaritaplus.api.dto.importer.review;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionAssetCropRequest(
        @NotNull @Min(0) Integer cropX,
        @NotNull @Min(0) Integer cropY,
        @NotNull @Min(1) Integer cropWidth,
        @NotNull @Min(1) Integer cropHeight
) {
}
