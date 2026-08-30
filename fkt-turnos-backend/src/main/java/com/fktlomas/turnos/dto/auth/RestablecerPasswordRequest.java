package com.fktlomas.turnos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String passwordNueva
) {
}
