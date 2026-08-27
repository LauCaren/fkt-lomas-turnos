package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 180) String email,
        @Pattern(regexp = Patrones.TELEFONO_OPCIONAL, message = "El teléfono no es válido") String telefono,
        @NotBlank @Size(max = 2000) String mensaje
) {
}
