package com.gabaritaplus.api.dto.user;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String username,
        String bio,
        String targetCourse,
        boolean active,
        Set<String> roles,
        OffsetDateTime createdAt
) {
}
