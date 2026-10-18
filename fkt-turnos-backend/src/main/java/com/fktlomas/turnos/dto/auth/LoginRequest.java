package com.fktlomas.turnos.dto.auth;

import jakarta.validation.constraints.Email;
import com.fktlomas.turnos.util.DatosPersonales;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String codigoMfa
) {
    /** El email se normaliza al entrar (ver DatosPersonales.normalizarEmail): la base lo guarda en minúsculas. */
    public LoginRequest {
        email = DatosPersonales.normalizarEmail(email);
    }
}
