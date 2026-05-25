package com.gabaritaplus.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 120)
        String fullName,
        @NotBlank(message = "Email é obrigatório.")
        @Email(message = "Email inválido.")
        @Size(max = 150)
        String email,
        @NotBlank(message = "Username é obrigatório.")
        @Size(min = 3, max = 60)
        String username,
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 64)
        String password,
        @Size(max = 100)
        String targetCourse
) {
}
