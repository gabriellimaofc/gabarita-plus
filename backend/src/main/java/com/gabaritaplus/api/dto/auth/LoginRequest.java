package com.gabaritaplus.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email ou username é obrigatório.")
        String usernameOrEmail,
        @NotBlank(message = "Senha é obrigatória.")
        String password
) {
}
