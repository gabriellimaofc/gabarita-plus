package com.gabaritaplus.api.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 120)
        String fullName,
        @Size(max = 500)
        String bio,
        @Size(max = 100)
        String targetCourse
) {
}
