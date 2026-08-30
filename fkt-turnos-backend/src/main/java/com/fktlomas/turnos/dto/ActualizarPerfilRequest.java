package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.*;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150)
        String nombreCompleto,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono no es válido")
        String telefono,

        @NotNull(message = "El género es obligatorio")
        Genero genero,

        @NotBlank(message = "La obra social es obligatoria (usá 'Particular' si no tenés)")
        @Size(max = 100)
        String obraSocial
) {
}
