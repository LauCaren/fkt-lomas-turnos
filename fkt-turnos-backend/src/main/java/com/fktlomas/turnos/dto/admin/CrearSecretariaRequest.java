package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearSecretariaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombreCompleto,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = Patrones.PASSWORD_FUERTE, message = Patrones.PASSWORD_FUERTE_MENSAJE)
        @Size(max = Patrones.PASSWORD_LONGITUD_MAXIMA, message = Patrones.PASSWORD_LONGITUD_MENSAJE)
        String password
) {
}
