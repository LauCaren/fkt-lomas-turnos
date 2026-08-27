package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearEspecialidadRequest(
        @NotBlank @Size(max = 100) String nombre
) {
}
