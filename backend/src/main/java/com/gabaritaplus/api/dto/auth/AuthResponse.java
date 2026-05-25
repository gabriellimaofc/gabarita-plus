package com.gabaritaplus.api.dto.auth;

import com.gabaritaplus.api.dto.user.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
}
