package com.fktlomas.turnos.dto.auth;

import com.fktlomas.turnos.util.DatosPersonales;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OlvidePasswordRequest(
        @NotBlank @Email String email
) {
    /** El email se normaliza al entrar (ver DatosPersonales.normalizarEmail): la base lo guarda en minúsculas. */
    public OlvidePasswordRequest {
        email = DatosPersonales.normalizarEmail(email);
    }
}
