package com.fktlomas.turnos.dto.auth;

import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerPasswordRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = Patrones.PASSWORD_FUERTE, message = Patrones.PASSWORD_FUERTE_MENSAJE) String passwordNueva
) {
}
