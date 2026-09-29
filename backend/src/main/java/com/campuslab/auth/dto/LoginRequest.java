package com.campuslab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve estar em formato válido")
        @Size(max = 255, message = "Email não pode exceder 255 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 255, message = "Senha não pode exceder 255 caracteres")
        String password
) {
}
